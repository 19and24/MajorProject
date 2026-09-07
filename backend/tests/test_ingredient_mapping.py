import unittest
import warnings
from pathlib import Path

from scripts.ingredient_mapping import IngredientMapper


MAPPING_PATH = Path(__file__).resolve().parents[1] / "data" / "ingredient_mapping.csv"


class IngredientMappingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mapper = IngredientMapper.from_csv(MAPPING_PATH)

    def test_garlic_preparation_forms_share_one_canonical_ingredient(self):
        for name in ("마늘", "통마늘", "다진마늘", "다진 마늘", "간마늘"):
            with self.subTest(name=name):
                self.assertEqual(self.mapper.resolve(name, None).canonical_name, "마늘")

    def test_garlic_scapes_do_not_map_to_garlic(self):
        self.assertEqual(self.mapper.resolve("마늘쫑", None).canonical_name, "마늘쫑")
        self.assertIsNone(self.mapper.resolve("마늘 소스", None))

    def test_leading_amount_is_moved_out_of_name(self):
        resolved = self.mapper.resolve("6쪽 마늘", None)
        self.assertEqual(resolved.canonical_name, "마늘")
        self.assertEqual(resolved.amount, "6쪽")

    def test_optional_marker_is_preserved(self):
        resolved = self.mapper.resolve("(선택) 다진마늘", "1큰술")
        self.assertEqual(resolved.canonical_name, "마늘")
        self.assertEqual(resolved.amount, "1큰술")
        self.assertTrue(resolved.optional)

    def test_spacing_variation_uses_exact_compact_alias(self):
        self.assertEqual(self.mapper.resolve("청 양 고추", None).canonical_name, "청양고추")

    def test_neutral_cooking_oils_share_edible_oil_canonical(self):
        for name in ("카놀라유", "포도씨유", "현미유", "해바라기유", "아보카도유"):
            with self.subTest(name=name):
                self.assertEqual(self.mapper.resolve(name, None).canonical_name, "식용유")

        self.assertEqual(
            self.mapper.resolve("엑스트라버진올리브오일", None).canonical_name,
            "올리브오일",
        )

    def test_corn_and_greek_yogurt_variants_are_grouped(self):
        self.assertEqual(self.mapper.resolve("초당옥수수", None).canonical_name, "옥수수")
        self.assertEqual(self.mapper.resolve("캔 옥수수", None).canonical_name, "옥수수")
        self.assertEqual(
            self.mapper.resolve("무가당그릭요거트", None).canonical_name,
            "그릭요거트",
        )
        self.assertEqual(self.mapper.resolve("무가당요거트", None).canonical_name, "요거트")

    def test_hard_cheese_spelling_variants_are_grouped_by_identity(self):
        self.assertEqual(
            self.mapper.resolve("그라노파다노치즈", None).canonical_name,
            "그라나파다노치즈",
        )
        self.assertEqual(
            self.mapper.resolve("파르미지아노레지아노치즈", None).canonical_name,
            "파마산치즈",
        )

    def test_substitutable_but_different_ingredients_stay_separate(self):
        self.assertEqual(self.mapper.resolve("멸치액젓", None).canonical_name, "멸치액젓")
        self.assertEqual(self.mapper.resolve("참치액", None).canonical_name, "참치액")
        self.assertEqual(self.mapper.resolve("페페론치노", None).canonical_name, "페페론치노")
        self.assertEqual(self.mapper.resolve("레드페퍼", None).canonical_name, "레드페퍼")

    def test_low_frequency_preparation_descriptions_are_removed(self):
        cases = {
            "고명용 대파": "대파",
            "다진 홍고추": "홍고추",
            "냉동 오징어": "오징어",
            "LA갈비 구워둔 것": "LA갈비",
        }
        for raw_name, canonical_name in cases.items():
            with self.subTest(raw_name=raw_name):
                self.assertEqual(
                    self.mapper.resolve(raw_name, None).canonical_name,
                    canonical_name,
                )

    def test_size_and_embedded_amount_are_preserved_as_amount(self):
        resolved = self.mapper.resolve("감자 작은 사이즈 3개 손질 후", None)

        self.assertEqual(resolved.canonical_name, "감자")
        self.assertEqual(resolved.amount, "3개")

    def test_embedded_amount_is_removed_even_when_amount_column_exists(self):
        resolved = self.mapper.resolve("가지 1개", "약간")

        self.assertEqual(resolved.canonical_name, "가지")
        self.assertEqual(resolved.amount, "1개 약간")

    def test_compact_preparation_and_form_descriptions_use_known_base_only(self):
        cases = {
            "구운감자": "감자",
            "냉동블루베리": "블루베리",
            "건 표고버섯 슬라이스": "표고버섯",
            "갈은통깨": "참깨",
            "감자(홍)": "감자",
            "감자1": "감자",
            "각종 채소": "채소",
            "꼬마 새송이버섯": "새송이버섯",
            "생닭": "닭고기",
            "무가당 두유": "두유",
            "간들깨가루": "들깨가루",
            "가는 베이비 우엉": "우엉",
            "3배 강한 식초": "식초",
            "간수뺀 천일염": "천일염",
            "(작은크기( 감자": "감자",
        }
        for raw_name, canonical_name in cases.items():
            with self.subTest(raw_name=raw_name):
                self.assertEqual(
                    self.mapper.resolve(raw_name, None).canonical_name,
                    canonical_name,
                )

        self.assertIsNone(self.mapper.resolve("건빵", None))

    def test_parenthesized_size_and_repeated_preparation_are_removed(self):
        parenthesized = self.mapper.resolve("감자 3개(작은 사이즈)", None)
        repeated = self.mapper.resolve("감자 손질 2개 손질 후", None)

        self.assertEqual(parenthesized.canonical_name, "감자")
        self.assertEqual(parenthesized.amount, "3개")
        self.assertEqual(repeated.canonical_name, "감자")
        self.assertEqual(repeated.amount, "2개")

    def test_clear_compound_seasonings_create_multiple_links(self):
        resolved = self.mapper.resolve_many("소금 후추", "약간")

        self.assertEqual(
            [item.canonical_name for item in resolved],
            ["소금", "후추"],
        )
        self.assertTrue(all(item.raw_name == "소금 후추" for item in resolved))
        self.assertTrue(all(item.amount == "약간" for item in resolved))

    def test_alternative_ingredients_are_not_treated_as_simultaneous(self):
        resolved = self.mapper.resolve_many("간장 또는 액젓", None)
        self.assertIsNotNone(resolved)
        self.assertEqual(len(resolved), 1)
        self.assertEqual(resolved[0].canonical_name, "간장")

    def test_slash_delimiter_splits_compound_ingredients(self):
        """소금/후추 같은 슬래시 구분 복합 재료를 분리한다."""
        resolved = self.mapper.resolve_many('소금/후추', '약간')
        self.assertEqual(
            [item.canonical_name for item in resolved],
            ['소금', '후추'],
        )

    def test_slash_in_fraction_is_not_split(self):
        """1/2 같은 분수 표현은 compound로 분리하지 않는다."""
        # 분수가 포함된 경우 compound 분리가 발생하지 않아야 함
        resolved = self.mapper.resolve('마늘', '1/2개')
        self.assertEqual(resolved.canonical_name, '마늘')

    def test_alternative_expression_resolves_first_option(self):
        """'물엿 또는 올리고당' 같은 대안 표현에서 첫 번째 해결 가능한 대안을 채택한다."""
        resolved = self.mapper.resolve_many('물엿 또는 올리고당', None)
        self.assertIsNotNone(resolved)
        self.assertEqual(len(resolved), 1)
        self.assertEqual(resolved[0].canonical_name, '물엿')

    def test_alternative_with_or_keyword(self):
        """영문 'or' 키워드도 대안으로 처리한다."""
        resolved = self.mapper.resolve_many('참기름 or 들기름', None)
        self.assertIsNotNone(resolved)
        self.assertEqual(resolved[0].canonical_name, '참기름')

    def test_alternative_returns_none_when_nothing_resolves(self):
        """대안 중 아무것도 해결 못하면 None을 반환한다."""
        result = self.mapper.resolve_many('미지의재료A 또는 미지의재료B', None)
        self.assertIsNone(result)

    def test_modifier_prefix_low_sugar_is_stripped(self):
        """'저당 고추장'에서 '저당' 접두사가 제거되어 '고추장'으로 매핑된다."""
        resolved = self.mapper.resolve('저당 고추장', None)
        self.assertIsNotNone(resolved)
        self.assertEqual(resolved.canonical_name, '고추장')

    def test_modifier_prefix_half_is_stripped(self):
        """'하프 마요네즈'에서 '하프' 접두사가 제거되어 '마요네즈'로 매핑된다."""
        resolved = self.mapper.resolve('하프 마요네즈', None)
        self.assertIsNotNone(resolved)
        self.assertEqual(resolved.canonical_name, '마요네즈')

    def test_missing_exclusion_file_issues_warning_and_continues(self):
        with warnings.catch_warnings(record=True) as recorded:
            warnings.simplefilter("always")
            mapper = IngredientMapper.from_csv(
                MAPPING_PATH,
                exclusion_path=Path("non_existent_exclusion.csv"),
            )
            self.assertEqual(len(recorded), 1)
            self.assertTrue(issubclass(recorded[0].category, UserWarning))
            self.assertIn("재료 제외 파일을 찾을 수 없습니다", str(recorded[0].message))
            self.assertIsNotNone(mapper)


if __name__ == "__main__":
    unittest.main()
