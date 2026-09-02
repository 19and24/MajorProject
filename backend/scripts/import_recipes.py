"""Import the 10000recipe CSV dump into the app database.

Usage (from backend/ with the venv active):
    python scripts/import_recipes.py
    python scripts/import_recipes.py data/TB_RECIPE_SEARCH_251231.csv
    python scripts/import_recipes.py --dry-run
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

sys.path.insert(0, str(SCRIPT_DIR))
sys.path.insert(0, str(BACKEND_DIR))

from parse_ingredients import ingredient_key, parse_items  # noqa: E402

from app.core.category_emojis import CATEGORY_EMOJIS, DEFAULT_EMOJI  # noqa: E402
from app.database import SessionLocal  # noqa: E402
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
        "--min-ingredient-frequency",
        type=int,
        default=MIN_INGREDIENT_FREQ,
        metavar="N",
        help=f"N개 이상의 레시피에 등장한 재료만 연결(기본값: {MIN_INGREDIENT_FREQ})",
    )
    args = parser.parse_args()
    if args.min_ingredient_frequency < 1:
        parser.error("--min-ingredient-frequency는 1 이상이어야 합니다.")
    return args


def resolve_csv_paths(paths: list[Path]) -> list[Path]:
    candidates = paths or [DEFAULT_DATA_DIR]
    resolved: set[Path] = set()
    for candidate in candidates:
        candidate = candidate.expanduser()
        if candidate.is_dir():
            resolved.update(path.resolve() for path in candidate.glob("*.csv"))
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


def build_ingredient_catalog(rows: list[dict[str, str | None]]) -> dict[str, dict]:
    """Return normalized-key entries with a representative display name."""
    catalog: dict[str, dict] = {}
    for row in rows:
        acto = clean_field(row.get("CKG_MTRL_ACTO_NM"), 120) or "기타"
        for name, _amount in parse_items(row.get("CKG_MTRL_CN") or ""):
            name = clean_field(name, 100)
            if not name:
                continue
            key = ingredient_key(name)
            entry = catalog.setdefault(
                key,
                {"name": name, "count": 0, "category_votes": Counter()},
            )
            entry["count"] += 1
            entry["category_votes"][acto] += 1
    return catalog


def display_ingredient_text(parsed_items: list[tuple[str, str | None]]) -> str:
    return ", ".join(
        f"{name} {amount}" if amount else name for name, amount in parsed_items
    )


def main() -> None:
    args = parse_args()
    csv_paths = resolve_csv_paths(args.paths)
    print("Reading CSV files:")
    for csv_path in csv_paths:
        print(f"  - {csv_path}")
    rows = load_rows(csv_paths)
    print(f"Loaded {len(rows)} unique recipe rows")

    print("Building ingredient catalog ...")
    catalog = build_ingredient_catalog(rows)
    kept_keys = {
        key
        for key, entry in catalog.items()
        if entry["count"] >= args.min_ingredient_frequency
    }
    print(
        f"Distinct ingredient names: {len(catalog)}, "
        f"kept (freq >= {args.min_ingredient_frequency}): {len(kept_keys)}"
    )

    if args.dry_run:
        parsed_links = sum(
            1
            for row in rows
            for name, _amount in parse_items(row.get("CKG_MTRL_CN") or "")
            if ingredient_key(clean_field(name, 100)) in kept_keys
        )
        print(f"Dry run complete. Parsed {parsed_links} ingredient occurrences.")
        return

    db = SessionLocal()
    try:
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
            db.flush()

        print("Upserting ingredients ...")
        existing_ingredients = db.query(Ingredient).all()
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
            category = entry["category_votes"].most_common(1)[0][0]
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

        print("Inserting recipes and recipe_ingredient links ...")
        total_recipes = 0
        total_links = 0
        recipe_batch: list[Recipe] = []
        pending_links_by_recipe: list[
            tuple[Recipe, list[tuple[str, str | None]]]
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
                for name, amount in parsed_items:
                    name = clean_field(name, 100)
                    ingredient_id = key_to_id.get(ingredient_key(name))
                    if ingredient_id is None or ingredient_id in seen_ingredient_ids:
                        continue
                    seen_ingredient_ids.add(ingredient_id)
                    link_rows.append(RecipeIngredient(
                        recipe_id=recipe.id,
                        ingredient_id=ingredient_id,
                        amount=clean_field(amount, 50) or None,
                        optional=False,
                    ))
            db.add_all(link_rows)
            db.flush()
            total_recipes += len(recipe_batch)
            total_links += len(link_rows)
            recipe_batch = []
            pending_links_by_recipe = []
            print(f"  ... {total_recipes} recipes, {total_links} ingredient links")

        for row in rows:
            parsed_items = parse_items(row.get("CKG_MTRL_CN") or "")
            recipe = Recipe(
                title=clean_field(recipe_title(row), 100) or "제목 없음",
                method=clean_field(row.get("CKG_MTH_ACTO_NM"), 200),
                ingredient_acto=clean_field(row.get("CKG_MTRL_ACTO_NM"), 200),
                type=clean_field(row.get("CKG_KND_ACTO_NM"), 200),
                ingredient_all=clean_field(display_ingredient_text(parsed_items), 400),
                portion=clean_field(row.get("CKG_INBUN_NM"), 20),
                cooktime=clean_field(row.get("CKG_TIME_NM"), 20),
            )
            recipe_batch.append(recipe)
            pending_links_by_recipe.append((recipe, parsed_items))
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
