import unittest

from scripts.parse_ingredients import ingredient_key, parse_items


class ParseIngredientsTest(unittest.TestCase):
    def test_current_control_delimited_csv_format(self):
        raw = (
            "[재료] 소고기\x07100\x07g\x07| 불린미역\x0750\x07g\x07| "
            "참기름\x07\x07조금\x07 [양념] 국간장\x071\x07큰술\x07"
        )

        self.assertEqual(
            parse_items(raw),
            [
                ("소고기", "100g"),
                ("불린미역", "50g"),
                ("참기름", "조금"),
                ("국간장", "1큰술"),
            ],
        )

    def test_legacy_space_delimited_format(self):
        raw = "[재료] 어묵 2개| 김밥용김 3장 [양념] 간장 약간"

        self.assertEqual(
            parse_items(raw),
            [("어묵", "2개"), ("김밥용김", "3장"), ("간장", "약간")],
        )

    def test_name_without_amount(self):
        self.assertEqual(parse_items("[재료] 월계수잎\x07\x07\x07"), [("월계수잎", None)])

    def test_database_duplicate_key_is_case_insensitive(self):
        self.assertEqual(ingredient_key("LA갈비"), ingredient_key("La갈비"))
        self.assertEqual(ingredient_key("MSG"), ingredient_key("msg"))

    def test_database_duplicate_key_normalizes_full_width_text(self):
        self.assertEqual(ingredient_key("Ａ간장"), ingredient_key("A간장"))


if __name__ == "__main__":
    unittest.main()
