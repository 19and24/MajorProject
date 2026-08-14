"""One-off import of the 10000recipe CSV dump into the app's SQLite/MySQL database.

Usage (from backend/ with the venv active):
    python scripts/import_recipes.py [path/to/TB_RECIPE_SEARCH-*.csv]

Populates Ingredient (from the distinct set of parsed ingredient names appearing
in >= MIN_INGREDIENT_FREQ recipes) and Recipe/RecipeIngredient (from every row).
Safe to re-run: it wipes and rebuilds recipe/recipe_ingredient/ingredient data.
"""
import csv
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from parse_ingredients import parse_items  # noqa: E402

from app.core.category_emojis import CATEGORY_EMOJIS, DEFAULT_EMOJI  # noqa: E402
from app.database import Base, SessionLocal, engine  # noqa: E402
from app.models.ingredient import Ingredient  # noqa: E402
from app.models.recipe import Recipe  # noqa: E402
from app.models.recipe_ingredient import RecipeIngredient  # noqa: E402

MIN_INGREDIENT_FREQ = 5
BATCH_SIZE = 2000
DEFAULT_CSV_PATH = Path("D:/TB_RECIPE_SEARCH-231130.csv")


def clean_field(value: str | None, max_len: int) -> str:
    value = (value or "").strip()
    return value[:max_len]


def load_rows(csv_path: Path) -> list[dict]:
    with csv_path.open(encoding="cp949", errors="replace", newline="") as f:
        reader = csv.DictReader(f)
        return [row for row in reader if (row.get("CKG_NM") or "").strip()]


def build_ingredient_catalog(rows: list[dict]) -> dict[str, dict]:
    """Returns {ingredient_name: {"count": int, "category_votes": Counter}}."""
    catalog: dict[str, dict] = {}
    for row in rows:
        acto = clean_field(row.get("CKG_MTRL_ACTO_NM"), 120) or "기타"
        for name, _amount in parse_items(row.get("CKG_MTRL_CN") or ""):
            name = clean_field(name, 100)
            if not name:
                continue
            entry = catalog.setdefault(name, {"count": 0, "category_votes": Counter()})
            entry["count"] += 1
            entry["category_votes"][acto] += 1
    return catalog


def main() -> None:
    csv_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_CSV_PATH
    if not csv_path.exists():
        raise SystemExit(f"CSV not found: {csv_path}")

    print(f"Reading {csv_path} ...")
    rows = load_rows(csv_path)
    print(f"Loaded {len(rows)} recipe rows")

    print("Building ingredient catalog ...")
    catalog = build_ingredient_catalog(rows)
    kept_names = {
        name for name, entry in catalog.items() if entry["count"] >= MIN_INGREDIENT_FREQ
    }
    print(f"Distinct ingredient names: {len(catalog)}, kept (freq >= {MIN_INGREDIENT_FREQ}): {len(kept_names)}")

    Base.metadata.create_all(engine)
    db = SessionLocal()
    try:
        print("Clearing existing recipe_ingredient / recipe / ingredient rows ...")
        db.query(RecipeIngredient).delete()
        db.query(Recipe).delete()
        db.query(Ingredient).delete()
        db.commit()

        print("Inserting ingredients ...")
        name_to_id: dict[str, int] = {}
        ingredient_rows = []
        for name in sorted(kept_names):
            category = catalog[name]["category_votes"].most_common(1)[0][0]
            emoji = CATEGORY_EMOJIS.get(category, DEFAULT_EMOJI)
            ingredient_rows.append(Ingredient(name=name, category=category, emoji=emoji))

        db.add_all(ingredient_rows)
        db.commit()
        for ingredient in ingredient_rows:
            name_to_id[ingredient.name] = ingredient.id
        print(f"Inserted {len(ingredient_rows)} ingredients")

        print("Inserting recipes and recipe_ingredient links ...")
        total_recipes = 0
        total_links = 0
        recipe_batch: list[Recipe] = []
        pending_links_by_recipe: list[tuple[Recipe, list[dict]]] = []

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
                    ingredient_id = name_to_id.get(name)
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
            db.commit()
            total_recipes += len(recipe_batch)
            total_links += len(link_rows)
            recipe_batch = []
            pending_links_by_recipe = []
            print(f"  ... {total_recipes} recipes, {total_links} ingredient links")

        for row in rows:
            recipe = Recipe(
                title=clean_field(row.get("CKG_NM") or row.get("RCP_TTL"), 100) or "제목 없음",
                method=clean_field(row.get("CKG_MTH_ACTO_NM"), 200),
                ingredient_acto=clean_field(row.get("CKG_MTRL_ACTO_NM"), 200),
                type=clean_field(row.get("CKG_KND_ACTO_NM"), 200),
                ingredient_all=clean_field(row.get("CKG_MTRL_CN"), 400),
                portion=clean_field(row.get("CKG_INBUN_NM"), 20),
                cooktime=clean_field(row.get("CKG_TIME_NM"), 20),
            )
            parsed_items = parse_items(row.get("CKG_MTRL_CN") or "")
            recipe_batch.append(recipe)
            pending_links_by_recipe.append((recipe, parsed_items))
            if len(recipe_batch) >= BATCH_SIZE:
                flush()
        flush()

        print(f"Done. Inserted {total_recipes} recipes and {total_links} recipe_ingredient links.")
    finally:
        db.close()


if __name__ == "__main__":
    main()
