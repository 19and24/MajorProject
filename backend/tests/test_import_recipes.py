import sys
import unittest
from unittest.mock import patch

from scripts.import_recipes import parse_args, select_test_rows


class ImportRecipesOptionsTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
