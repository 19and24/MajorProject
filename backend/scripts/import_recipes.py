"""Import the 10000recipe CSV dump into the app database.

Usage (from backend/ with the venv active):
    python scripts/import_recipes.py
    python scripts/import_recipes.py data/TB_RECIPE_SEARCH_251231.csv
    python scripts/import_recipes.py --dry-run
    python scripts/import_recipes.py --test
    python scripts/import_recipes.py --test 500 --offset 1000
    python scripts/import_recipes.py --force

With no path argument, every CSV in backend/data is read. Rows are de-duplicated
by RCP_SNO so dated full snapshots can coexist. A normal run seeds an empty recipe
table and skips an already populated database. --force rebuilds recipes and links,
but preserves Ingredient rows because FridgeItem also references them.
"""
import argparse
import csv
import sys
from collections import Counter
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
BACKEND_DIR = SCRIPT_DIR.parent
DEFAULT_DATA_DIR = BACKEND_DIR / "data"
DEFAULT_MAPPING_PATH = DEFAULT_DATA_DIR / "ingredient_mapping.csv"
DEFAULT_EXCLUSION_PATH = DEFAULT_DATA_DIR / "ingredient_exclusions.csv"
DEFAULT_UNRESOLVED_OUTPUT = DEFAULT_DATA_DIR / "unresolved_ingredients.csv"
RECIPE_CSV_GLOB = "TB_RECIPE_SEARCH*.csv"

sys.path.insert(0, str(SCRIPT_DIR))
sys.path.insert(0, str(BACKEND_DIR))

from parse_ingredients import ingredient_key, parse_items  # noqa: E402
from ingredient_mapping import IngredientMapper, ResolvedIngredient  # noqa: E402

from app.core.category_emojis import CATEGORY_EMOJIS, DEFAULT_EMOJI  # noqa: E402
from app.database import SessionLocal  # noqa: E402
from app.models.fridge_item import FridgeItem  # noqa: E402
from app.models.ingredient import Ingredient  # noqa: E402
from app.models.recipe import Recipe  # noqa: E402
from app.models.recipe_ingredient import RecipeIngredient  # noqa: E402

MIN_INGREDIENT_FREQ = 1
BATCH_SIZE = 2000
REQUIRED_COLUMNS = {
    "RCP_SNO",
    "CKG_NM",
    "CKG_MTRL_CN",
    "CKG_MTH_ACTO_NM",
    "CKG_MTRL_ACTO_NM",
    "CKG_KND_ACTO_NM",
    "CKG_INBUN_NM",
    "CKG_TIME_NM",
}


def clean_field(value: str | None, max_len: int) -> str:
    value = (value or "").strip()
    return value[:max_len]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="10000recipe CSV 데이터 적재")
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        help="CSV 파일 또는 CSV 파일이 들어 있는 폴더(기본값: backend/data)",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="기존 레시피와 연결을 지우고 다시 적재(재료 ID는 보존)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="CSV 읽기와 파싱 결과만 확인하고 DB에는 쓰지 않음",
    )
    parser.add_argument(
        "--test",
        nargs="?",
        type=int,
        const=100,
        default=None,
        metavar="N",
        help="테스트 적재할 레시피 개수(숫자를 생략하면 100개)",
    )
    parser.add_argument(
        "--offset",
        type=int,
        default=0,
        metavar="N",
        help="--test에서 건너뛸 레시피 개수(0부터 시작, 기본값: 0)",
    )
    parser.add_argument(
        "--min-ingredient-frequency",
        type=int,
        default=MIN_INGREDIENT_FREQ,
        metavar="N",
        help=f"N개 이상의 레시피에 등장한 재료만 연결(기본값: {MIN_INGREDIENT_FREQ})",
    )
    parser.add_argument(
        "--mapping-file",
        type=Path,
        default=DEFAULT_MAPPING_PATH,
        help="표준 재료 매핑 CSV(기본값: backend/data/ingredient_mapping.csv)",
    )
    parser.add_argument(
        "--exclusion-file",
        type=Path,
        default=DEFAULT_EXCLUSION_PATH,
        help="재료로 보지 않을 원문 목록 CSV",
    )
    parser.add_argument(
        "--unresolved-output",
        type=Path,
        default=DEFAULT_UNRESOLVED_OUTPUT,
        help="전체 미해결 재료 보고서 CSV 경로",
    )
    args = parser.parse_args()
    if args.min_ingredient_frequency < 1:
        parser.error("--min-ingredient-frequency는 1 이상이어야 합니다.")
    if args.test is not None and args.test < 1:
        parser.error("--test의 레시피 개수는 1 이상이어야 합니다.")
    if args.offset < 0:
        parser.error("--offset은 0 이상이어야 합니다.")
    if args.offset and args.test is None:
        parser.error("--offset은 --test와 함께 사용해야 합니다.")
    return args


def resolve_csv_paths(paths: list[Path]) -> list[Path]:
    candidates = paths or [DEFAULT_DATA_DIR]
    resolved: set[Path] = set()
    for candidate in candidates:
        candidate = candidate.expanduser()
        if candidate.is_dir():
            resolved.update(path.resolve() for path in candidate.glob(RECIPE_CSV_GLOB))
        elif candidate.is_file() and candidate.suffix.lower() == ".csv":
            resolved.add(candidate.resolve())
        else:
            raise SystemExit(f"CSV 파일 또는 폴더를 찾을 수 없습니다: {candidate}")

    csv_paths = sorted(resolved)
    if not csv_paths:
        raise SystemExit(f"CSV 파일이 없습니다: {', '.join(map(str, candidates))}")
    return csv_paths


def recipe_title(row: dict[str, str | None]) -> str:
    return (row.get("CKG_NM") or row.get("RCP_TTL") or "").strip()


def load_rows(csv_paths: list[Path]) -> list[dict[str, str | None]]:
    rows_by_source_id: dict[str, dict[str, str | None]] = {}
    for csv_path in csv_paths:
        # Excel의 한국어 ANSI CSV는 EUC-KR 확장 인코딩인 CP949를 사용한다.
        # 순수 euc-kr 코덱은 CP949 확장 문자에서 실패할 수 있으므로 cp949로 읽는다.
        with csv_path.open(encoding="cp949", errors="strict", newline="") as f:
            reader = csv.DictReader(f)
            columns = set(reader.fieldnames or [])
            missing = REQUIRED_COLUMNS - columns
            if missing:
                raise SystemExit(
                    f"필수 CSV 컬럼이 없습니다 ({csv_path.name}): {', '.join(sorted(missing))}"
                )
            for line_number, row in enumerate(reader, start=2):
                if not recipe_title(row):
                    continue
                source_id = (row.get("RCP_SNO") or "").strip()
                if not source_id:
                    source_id = f"{csv_path.resolve()}:{line_number}"
                rows_by_source_id[source_id] = row
    return list(rows_by_source_id.values())


def build_ingredient_catalog(
    rows: list[dict[str, str | None]],
    mapper: IngredientMapper,
) -> tuple[dict[str, dict], Counter]:
    """Return canonical ingredient entries and unresolved source-name counts."""
    catalog: dict[str, dict] = {}
    unresolved: Counter = Counter()
    for row in rows:
        for raw_name, amount in parse_items(row.get("CKG_MTRL_CN") or ""):
            resolved_items = mapper.resolve_many(clean_field(raw_name, 100), amount)
            if resolved_items is None:
                unresolved[clean_field(raw_name, 100)] += 1
                continue
            for resolved in resolved_items:
                key = ingredient_key(resolved.canonical_name)
                entry = catalog.setdefault(
                    key,
                    {
                        "name": resolved.canonical_name,
                        "category": resolved.category,
                        "count": 0,
                    },
                )
                entry["count"] += 1
    return catalog, unresolved


def display_ingredient_text(parsed_items: list[tuple[str, str | None]]) -> str:
    return ", ".join(
        f"{name} {amount}" if amount else name for name, amount in parsed_items
    )


def write_unresolved_report(unresolved: Counter, output_path: Path) -> None:
    """Write every unresolved name as an Excel-friendly mapping worklist."""
    output_path = output_path.expanduser()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open(encoding="utf-8-sig", mode="w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow([
            "raw_name",
            "source_frequency",
            "canonical_name",
            "category",
            "review_status",
            "notes",
        ])
        for raw_name, count in sorted(
            unresolved.items(),
            key=lambda item: (-item[1], ingredient_key(item[0])),
        ):
            writer.writerow([raw_name, count, "", "", "미해결", ""])


def select_test_rows(
    rows: list[dict[str, str | None]],
    test_count: int | None,
    offset: int,
) -> list[dict[str, str | None]]:
    if test_count is None:
        return rows

    selected = rows[offset:offset + test_count]
    if not selected:
        raise SystemExit(
            f"테스트 범위에 레시피가 없습니다: offset={offset}, count={test_count}"
        )
    return selected


def remap_fridge_items_to_canonical(
    db,
    ingredients: list[Ingredient],
    mapper: IngredientMapper,
    key_to_id: dict[str, int],
) -> int:
    """Move fridge items from former alias ingredients to canonical IDs."""
    remapped_count = 0
    for ingredient in ingredients:
        resolved = mapper.resolve(ingredient.name, None)
        if resolved is None:
            continue
        target_id = key_to_id.get(ingredient_key(resolved.canonical_name))
        if target_id is None or target_id == ingredient.id:
            continue
        remapped_count += (
            db.query(FridgeItem)
            .filter(FridgeItem.ingredient_id == ingredient.id)
            .update(
                {FridgeItem.ingredient_id: target_id},
                synchronize_session=False,
            )
        )
    return remapped_count


def main() -> None:
    args = parse_args()
    mapper = IngredientMapper.from_csv(args.mapping_file, args.exclusion_file)
    csv_paths = resolve_csv_paths(args.paths)
    print("Reading CSV files:")
    for csv_path in csv_paths:
        print(f"  - {csv_path}")
    rows = load_rows(csv_paths)
    print(f"Loaded {len(rows)} unique recipe rows")
    rows = select_test_rows(rows, args.test, args.offset)
    if args.test is not None:
        print(
            f"Test mode: selected {len(rows)} recipes "
            f"(offset={args.offset}, requested={args.test})"
        )

    print("Building ingredient catalog ...")
    catalog, unresolved = build_ingredient_catalog(rows, mapper)
    kept_keys = {
        key
        for key, entry in catalog.items()
        if entry["count"] >= args.min_ingredient_frequency
    }
    print(
        f"Mapped canonical ingredients: {len(catalog)}, "
        f"kept (freq >= {args.min_ingredient_frequency}): {len(kept_keys)}"
    )
    print(
        f"Unresolved ingredient occurrences: {sum(unresolved.values())} "
        f"across {len(unresolved)} names"
    )
    write_unresolved_report(unresolved, args.unresolved_output)
    print(f"Full unresolved report: {args.unresolved_output.resolve()}")
    if unresolved:
        print("Most common unresolved names (top 10):")
        for name, count in unresolved.most_common(10):
            print(f"  - {name}: {count}")

    if args.dry_run:
        parsed_links = sum(
            entry["count"] for key, entry in catalog.items() if key in kept_keys
        )
        print(f"Dry run complete. Parsed {parsed_links} ingredient occurrences.")
        return

    db = SessionLocal()
    try:
        ingredient_catalog_cleared = False
        existing_recipe_count = db.query(Recipe).count()
        if existing_recipe_count and not args.force:
            print(
                f"Recipe table already contains {existing_recipe_count} rows; "
                "skipping seed import. Use --force to rebuild recipes."
            )
            return

        if args.force:
            print("Clearing existing recipe_ingredient / recipe rows ...")
            db.query(RecipeIngredient).delete(synchronize_session=False)
            db.query(Recipe).delete(synchronize_session=False)
            fridge_item_count = db.query(FridgeItem).count()
            if fridge_item_count == 0:
                print("No fridge items found; clearing the old ingredient catalog ...")
                db.query(Ingredient).delete(synchronize_session=False)
                ingredient_catalog_cleared = True
            else:
                print(
                    f"Preserving ingredient rows because {fridge_item_count} "
                    "fridge items reference them."
                )
            db.flush()

        print("Upserting ingredients ...")
        # A bulk delete can leave deleted ORM instances visible in the session's
        # identity map. Never reuse their now-invalid primary keys.
        existing_ingredients = (
            [] if ingredient_catalog_cleared else db.query(Ingredient).all()
        )
        key_to_id = {
            ingredient_key(ingredient.name): ingredient.id
            for ingredient in existing_ingredients
        }
        ingredient_rows: list[Ingredient] = []
        for key in sorted(kept_keys):
            if key in key_to_id:
                continue
            entry = catalog[key]
            name = entry["name"]
            category = entry["category"]
            emoji = CATEGORY_EMOJIS.get(category, DEFAULT_EMOJI)
            ingredient_rows.append(Ingredient(name=name, category=category, emoji=emoji))

        db.add_all(ingredient_rows)
        db.flush()
        for ingredient in ingredient_rows:
            key_to_id[ingredient_key(ingredient.name)] = ingredient.id
        print(
            f"Inserted {len(ingredient_rows)} new ingredients; "
            f"reused {len(kept_keys) - len(ingredient_rows)} existing ingredients"
        )

        remapped_fridge_items = 0
        if args.force and not ingredient_catalog_cleared:
            remapped_fridge_items = remap_fridge_items_to_canonical(
                db,
                existing_ingredients,
                mapper,
                key_to_id,
            )
            if remapped_fridge_items:
                db.flush()
                print(
                    f"Remapped {remapped_fridge_items} fridge items to canonical ingredients"
                )

        print("Inserting recipes and recipe_ingredient links ...")
        total_recipes = 0
        total_links = 0
        recipe_batch: list[Recipe] = []
        pending_links_by_recipe: list[
            tuple[Recipe, list[ResolvedIngredient]]
        ] = []

        def flush():
            nonlocal total_recipes, total_links, recipe_batch, pending_links_by_recipe
            if not recipe_batch:
                return
            db.add_all(recipe_batch)
            db.flush()  # assigns recipe.id without committing
            link_rows = []
            for recipe, parsed_items in pending_links_by_recipe:
                seen_ingredient_ids: set[int] = set()
                for resolved in parsed_items:
                    ingredient_id = key_to_id.get(
                        ingredient_key(resolved.canonical_name)
                    )
                    if ingredient_id is None or ingredient_id in seen_ingredient_ids:
                        continue
                    seen_ingredient_ids.add(ingredient_id)
                    link_rows.append(RecipeIngredient(
                        recipe_id=recipe.id,
                        ingredient_id=ingredient_id,
                        original_name=clean_field(resolved.raw_name, 100) or None,
                        amount=clean_field(resolved.amount, 50) or None,
                        optional=resolved.optional,
                    ))
            db.add_all(link_rows)
            db.commit()  # keep each batch's transaction short-lived (TiDB drops long ones)
            total_recipes += len(recipe_batch)
            total_links += len(link_rows)
            recipe_batch = []
            pending_links_by_recipe = []
            print(f"  ... {total_recipes} recipes, {total_links} ingredient links")

        for row in rows:
            source_items = parse_items(row.get("CKG_MTRL_CN") or "")
            resolved_items = [
                resolved
                for name, amount in source_items
                for resolved in (
                    mapper.resolve_many(clean_field(name, 100), amount) or []
                )
            ]
            recipe = Recipe(
                title=clean_field(recipe_title(row), 100) or "제목 없음",
                method=clean_field(row.get("CKG_MTH_ACTO_NM"), 200),
                ingredient_acto=clean_field(row.get("CKG_MTRL_ACTO_NM"), 200),
                type=clean_field(row.get("CKG_KND_ACTO_NM"), 200),
                ingredient_all=clean_field(display_ingredient_text(source_items), 400),
                portion=clean_field(row.get("CKG_INBUN_NM"), 20),
                cooktime=clean_field(row.get("CKG_TIME_NM"), 20),
            )
            recipe_batch.append(recipe)
            pending_links_by_recipe.append((recipe, resolved_items))
            if len(recipe_batch) >= BATCH_SIZE:
                flush()
        flush()
        db.commit()

        print(f"Done. Inserted {total_recipes} recipes and {total_links} recipe_ingredient links.")
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    main()
