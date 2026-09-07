import sys
import csv
import tempfile
import unittest
from collections import Counter
from datetime import datetime, timedelta
from decimal import Decimal
from pathlib import Path
from unittest.mock import patch

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base
from app.models.fridge_item import FridgeItem
from app.models.ingredient import Ingredient
from scripts.import_recipes import (
    build_ingredient_catalog,
    parse_args,
    remap_fridge_items_to_canonical,
    select_test_rows,
    write_unresolved_report,
)
from scripts.ingredient_mapping import IngredientMapper
from scripts.parse_ingredients import ingredient_key


MAPPING_PATH = Path(__file__).resolve().parents[1] / "data" / "ingredient_mapping.csv"


class ImportRecipesOptionsTest(unittest.TestCase):
    def test_compound_source_name_adds_each_canonical_ingredient(self):
        mapper = IngredientMapper.from_csv(MAPPING_PATH)
        rows = [{"CKG_MTRL_CN": "소금 후추\x07약간"}]

        catalog, unresolved = build_ingredient_catalog(rows, mapper)

        self.assertEqual({entry["name"] for entry in catalog.values()}, {"소금", "후추"})
        self.assertFalse(unresolved)
    def test_test_option_defaults_to_one_hundred(self):
        with patch.object(sys, "argv", ["import_recipes.py", "--test"]):
            args = parse_args()

        self.assertEqual(args.test, 100)
        self.assertEqual(args.offset, 0)

    def test_test_option_accepts_count_and_offset(self):
        with patch.object(
            sys,
            "argv",
            ["import_recipes.py", "--test", "25", "--offset", "10"],
        ):
            args = parse_args()

        self.assertEqual(args.test, 25)
        self.assertEqual(args.offset, 10)

    def test_offset_requires_test_mode(self):
        with patch.object(sys, "argv", ["import_recipes.py", "--offset", "10"]):
            with self.assertRaises(SystemExit):
                parse_args()

    def test_select_test_rows_uses_zero_based_offset(self):
        rows = [{"RCP_SNO": str(index)} for index in range(10)]

        selected = select_test_rows(rows, test_count=3, offset=4)

        self.assertEqual([row["RCP_SNO"] for row in selected], ["4", "5", "6"])

    def test_select_test_rows_rejects_out_of_range_offset(self):
        rows = [{"RCP_SNO": "1"}]

        with self.assertRaises(SystemExit):
            select_test_rows(rows, test_count=10, offset=5)

    def test_unresolved_report_contains_every_name_in_frequency_order(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "unresolved.csv"
            write_unresolved_report(
                Counter({"굴": 9, "양송이": 8, "희귀재료": 1}),
                output,
            )
            with output.open(encoding="utf-8-sig", newline="") as f:
                rows = list(csv.DictReader(f))

        self.assertEqual([row["raw_name"] for row in rows], ["굴", "양송이", "희귀재료"])
        self.assertEqual([row["source_frequency"] for row in rows], ["9", "8", "1"])

    def test_fridge_items_are_remapped_when_a_canonical_ingredient_changes(self):
        engine = create_engine(
            "sqlite://",
            connect_args={"check_same_thread": False},
            poolclass=StaticPool,
        )
        Base.metadata.create_all(engine)
        Session = sessionmaker(bind=engine)
        try:
            with Session() as db:
                canola = Ingredient(name="카놀라유", category="유지류")
                edible_oil = Ingredient(name="식용유", category="유지류")
                db.add_all([canola, edible_oil])
                db.flush()
                item = FridgeItem(
                    ingredient_id=canola.id,
                    quantity=Decimal("1"),
                    unit="병",
                    deadline=datetime.now() + timedelta(days=30),
                    starred=False,
                )
                db.add(item)
                db.flush()

                mapper = IngredientMapper.from_csv(MAPPING_PATH)
                remapped = remap_fridge_items_to_canonical(
                    db,
                    [canola, edible_oil],
                    mapper,
                    {
                        ingredient_key(canola.name): canola.id,
                        ingredient_key(edible_oil.name): edible_oil.id,
                    },
                )
                db.flush()
                db.refresh(item)

                self.assertEqual(remapped, 1)
                self.assertEqual(item.ingredient_id, edible_oil.id)
        finally:
            Base.metadata.drop_all(engine)
            engine.dispose()


if __name__ == "__main__":
    unittest.main()
