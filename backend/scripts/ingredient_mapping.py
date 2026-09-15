"""Resolve raw recipe ingredient names to curated canonical ingredients."""

import csv
import re
import warnings
from dataclasses import dataclass
from pathlib import Path

try:
    from .parse_ingredients import ingredient_key, normalize_ingredient_name
except ImportError:  # Direct execution from backend/scripts.
    from parse_ingredients import ingredient_key, normalize_ingredient_name


MAPPING_COLUMNS = {"canonical_name", "category", "included_names"}
EXCLUSION_COLUMNS = {"raw_name", "reason"}
LEADING_NOISE_RE = re.compile(r"^[\s+.:;\\\]\[}_~]+")
OPTIONAL_PREFIX_RE = re.compile(r"^\(\s*선택(?:[_\s][^)]*)?\s*\)\s*")
STAGE_PREFIX_RE = re.compile(r"^\d+\s*차\s*(?:절임물|양념|반죽)\s*[-:]\s*")
CONCENTRATION_PREFIX_RE = re.compile(r"^\d+\s*배\s*(?:농축\s*)?")
PERCENT_PREFIX_RE = re.compile(r"^\d+\s*%\s*")
MALFORMED_SIZE_PREFIX_RE = re.compile(r"^\(\s*(?:작은|중간|큰)\s*크기\s*\(\s*")
TRAILING_SIZE_PAREN_RE = re.compile(
    r"\s*\((?:조금\s*)?(?:작은|중간|큰)\s*(?:사이즈|크기|것)?\)\s*$"
)
PREPARATION_PREFIX_RE = re.compile(
    r"^(?:고명용|가니쉬용|장식용|데코용|다진|간|갈은|깐|볶은|삶은|"
    r"데친|냉동|불린|찐|구운)\s+"
)
AMOUNT_UNIT_PATTERN = (
    r"kg|mg|ml|g|l|리터|개|쪽|호|컵|큰술|작은술|꼬집|장|봉|팩|줌|스푼|"
    r"공기|소주잔|숟가락|티스푼|캔|병|알|모|대|토막|조각|줄기|뿌리|통|"
    r"마리|인분|줄|단|송이|포기|묶음|톨|움큼|cm|t|T"
)
LEADING_AMOUNT_RE = re.compile(
    r"^(?P<amount>\d+(?:[./]\d+)?\s*"
    rf"(?:{AMOUNT_UNIT_PATTERN}))\s*"
    r"(?P<name>.+)$",
    re.IGNORECASE,
)
TRAILING_AMOUNT_RE = re.compile(
    r"^(?P<name>.+?)\s+(?P<amount>\d+(?:[./~]\d*)?\s*"
    rf"(?:{AMOUNT_UNIT_PATTERN})[\s~+=]*)$",
    re.IGNORECASE,
)
TRAILING_HALF_AMOUNT_RE = re.compile(r"^(?P<name>.+?)\s+(?P<amount>반)$")
TRAILING_AMOUNT_PREPARATION_RE = re.compile(
    r"^(?P<name>.+?)(?:\s+손질)?\s+(?P<amount>\d+(?:[./]\d+)?\s*"
    rf"(?:{AMOUNT_UNIT_PATTERN}))\s*(?:손질\s*후|손질한\s*것)$",
    re.IGNORECASE,
)
TRAILING_SIZE_RE = re.compile(
    r"^(?P<name>.+?)\s+(?:아주\s*)?(?:작은|중간|큰)\s*"
    r"(?:사이즈|크기|거|것)?\s*"
    r"(?:(?P<amount>\d+\s*개))?\s*(?:손질\s*후)?$"
)
TRAILING_SIZE_SYMBOL_RE = re.compile(
    r"^(?P<name>.+?)\s+(?:특대|대|중|소|大|中|小)$"
)
TRAILING_PREPARATION_RE = re.compile(
    r"^(?P<name>.+?)\s+(?:손질\s*후|손질한\s*것|손질|다진\s*것|썬\s*것|"
    r"구워둔\s*것|갈아둔\s*것|크게|듬뿍|담뿍)$"
)
FALLBACK_PREFIX_RE = re.compile(
    r"^(?:가공|가는|갈은|갈아낸|갈아준|깐|깍은|볶은|삶은|데친|냉동|불린|"
    r"찐|구운|손질한|손질된|손질|채썬|썬|썰은|썰어준|다진|건조|고운|굵은|국산|겨울|압축|자연산|"
    r"각종|미니|꼬마|작은|생|가염|무가당|저지방|유기농|간수뺀|강한|"
    r"만능|자른|통밀|한우|신선한|자투리|저칼로리|조미|중간|중국|한입쏙|"
    r"베이비|햇|말린|으깬|녹인|데운|따뜻한|차가운|어슷썬|잘게|잘개|곱게|"
    r"얇게|얅게|저민|두껍게|슬라이스한|슬라이스|채친|채\s*썬|토핑용|세척용|튀김용|구이용|"
    r"국물용|육수용|김밥용|샐러드용|고명용|기호에\s*따라|취향에\s*따라|취향껏|시판용|시판|"
    r"간|crushed|저당|안\s*매운|순한|무설탕|무가염|하프|송송\s*썬|송송|다듬은|빻은|"
    r"크게|작게|부순|씨를\s*빼고|해감\s*한|"
    r"밑간용[-:\s]*|밑간[-:\s]*|볶음용|조림용|국용|찌개용|"
    r"구울\s*때[-:\s]*|볶을\s*때[-:\s]*|절일\s*때[-:\s]*|해감할\s*때[-:\s]*)"
    r"\s*(?P<name>.+)$",
    re.IGNORECASE,
)
FALLBACK_SPACED_PREFIX_RE = re.compile(
    r"^(?:건|거피)\s+(?P<name>.+)$"
)
FALLBACK_SUFFIX_RE = re.compile(
    r"^(?P<name>.+?)(?:\s*(?:가루|채썬것|채|슬라이스|큐브|필렛|필레|분태|살|"
    r"통조림|캔|겉잎|속대|잎|줄기|뿌리|껍질|초록부분|흰부분|송송|볶음용|조림용|찌개용|구이용|카레용|수육용|샤브용)|\s+한)$"
)
FALLBACK_PARENTHETICAL_RE = re.compile(r"^(?P<name>.+?)\s*\([^()]+\)$")
FALLBACK_TRAILING_DIGIT_RE = re.compile(r"^(?P<name>.+?)[\s]*\d+$")
FALLBACK_TRAILING_LETTER_RE = re.compile(r"^(?P<name>.+?)[\s]*[A-Za-z가나다라ㄱㄴㄷ]$")
FALLBACK_TRAILING_NOISE_RE = re.compile(
    r"^(?P<name>.+?)\s+(?:약|약간|조금|적당량|넉넉히|듬뿍|담뿍|"
    r"국그릇\s*기준|한|두|세|반|깎아서|깍아서|평평하게|수북하게|수북이|취향껏|취향에\s*따라|기호껏|소량)$"
)
COMPOUND_DELIMITER_RE = re.compile(r"\s*(?:\+|&|·|,|，|/|\.)\s*")
TRAILING_GENERIC_WORDS_RE = re.compile(r"(?:\s+(?:약간|조금|밑간용|밑간))+$")
ALTERNATIVE_RE = re.compile(
    r"(?:\s*(?:또는|혹은|내지는)\s*|\s+or\s+|\s+(?:이나|나)\s+)", re.IGNORECASE
)
SPLITTABLE_CANONICAL_NAMES = {
    "소금",
    "굵은소금",
    "후추",
    "설탕",
    "간장",
    "국간장",
    "진간장",
    "식초",
    "참깨",
    "참기름",
    "들기름",
    "식용유",
    "고춧가루",
    "마늘",
    "생강",
}


@dataclass(frozen=True)
class CanonicalIngredient:
    name: str
    category: str


@dataclass(frozen=True)
class ResolvedIngredient:
    raw_name: str
    canonical_name: str
    category: str
    amount: str | None
    optional: bool


class IngredientMapper:
    def __init__(
        self,
        entries: dict[str, CanonicalIngredient],
        excluded_keys: set[str] | None = None,
    ):
        self._entries = entries
        self._excluded_keys = excluded_keys or set()
        compact_entries: dict[str, CanonicalIngredient] = {}
        for key, entry in entries.items():
            compact = key.replace(" ", "")
            previous = compact_entries.get(compact)
            if previous is not None and previous != entry:
                continue
            compact_entries[compact] = entry
        self._compact_entries = compact_entries

    @classmethod
    def from_csv(
        cls,
        mapping_path: Path,
        exclusion_path: Path | None = None,
    ) -> "IngredientMapper":
        if not mapping_path.is_file():
            raise SystemExit(f"재료 매핑 파일을 찾을 수 없습니다: {mapping_path}")

        entries: dict[str, CanonicalIngredient] = {}
        with mapping_path.open(encoding="utf-8-sig", errors="strict", newline="") as f:
            reader = csv.DictReader(f)
            missing = MAPPING_COLUMNS - set(reader.fieldnames or [])
            if missing:
                raise SystemExit(
                    f"재료 매핑 필수 컬럼이 없습니다: {', '.join(sorted(missing))}"
                )

            for line_number, row in enumerate(reader, start=2):
                canonical_name = normalize_ingredient_name(row.get("canonical_name") or "")
                category = (row.get("category") or "").strip()
                if not canonical_name or not category:
                    raise SystemExit(
                        f"재료 매핑의 표준명 또는 카테고리가 비었습니다: "
                        f"{mapping_path}:{line_number}"
                    )

                entry = CanonicalIngredient(canonical_name, category)
                included_names = [canonical_name]
                included_names.extend(
                    name.strip()
                    for name in (row.get("included_names") or "").split("|")
                    if name.strip()
                )
                for included_name in included_names:
                    key = ingredient_key(included_name)
                    previous = entries.get(key)
                    if previous is not None and previous != entry:
                        raise SystemExit(
                            "하나의 명칭이 서로 다른 표준 재료에 중복 매핑되었습니다: "
                            f"{included_name} ({previous.name}, {entry.name})"
                        )
                    entries[key] = entry

        excluded_keys: set[str] = set()
        if exclusion_path is not None:
            if not exclusion_path.is_file():
                warnings.warn(
                    f"재료 제외 파일을 찾을 수 없습니다: {exclusion_path}",
                    UserWarning,
                    stacklevel=2,
                )
            else:
                with exclusion_path.open(
                    encoding="utf-8-sig", errors="strict", newline=""
                ) as f:
                    reader = csv.DictReader(f)
                    missing = EXCLUSION_COLUMNS - set(reader.fieldnames or [])
                    if missing:
                        raise SystemExit(
                            f"재료 제외 필수 컬럼이 없습니다: {', '.join(sorted(missing))}"
                        )
                    for row in reader:
                        raw_name = normalize_ingredient_name(row.get("raw_name") or "")
                        if raw_name:
                            excluded_keys.add(ingredient_key(raw_name))

        return cls(entries, excluded_keys)

    def resolve(
        self,
        raw_name: str,
        amount: str | None,
    ) -> ResolvedIngredient | None:
        candidate, resolved_amount, optional = normalize_source_ingredient(raw_name, amount)
        entry = self._lookup(candidate)
        if entry is None:
            for fallback in fallback_ingredient_names(candidate):
                entry = self._lookup(fallback)
                if entry is not None:
                    break
        if entry is None:
            return None
        return ResolvedIngredient(
            raw_name=raw_name,
            canonical_name=entry.name,
            category=entry.category,
            amount=resolved_amount,
            optional=optional,
        )

    def resolve_many(
        self,
        raw_name: str,
        amount: str | None,
    ) -> list[ResolvedIngredient] | None:
        """Resolve one source field to one or more canonical ingredients.

        ``None`` means the source is still ambiguous. An empty list means it is
        a reviewed non-ingredient such as cookware, a container, or an action.
        """
        candidate, resolved_amount, optional = normalize_source_ingredient(
            raw_name, amount
        )
        if self._is_excluded(raw_name) or self._is_excluded(candidate):
            return []

        resolved = self.resolve(raw_name, amount)
        if resolved is not None:
            return [resolved]

        # 대안 표현 처리: "물엿 또는 올리고당" → 첫 번째 대안 우선 시도
        alternatives = ALTERNATIVE_RE.split(candidate)
        if len(alternatives) >= 2:
            for alt in alternatives:
                alt = alt.strip()
                if not alt:
                    continue
                entry = self._resolve_candidate(alt)
                if entry is not None:
                    return [ResolvedIngredient(
                        raw_name=raw_name,
                        canonical_name=entry.name,
                        category=entry.category,
                        amount=resolved_amount,
                        optional=optional,
                    )]
            return None  # 어떤 대안도 해결 못함

        parts = self._compound_parts(candidate)
        if parts is None:
            return None

        results: list[ResolvedIngredient] = []
        seen: set[str] = set()
        for part in parts:
            entry = self._resolve_candidate(part)
            if entry is None:
                return None
            key = ingredient_key(entry.name)
            if key in seen:
                continue
            seen.add(key)
            results.append(ResolvedIngredient(
                raw_name=raw_name,
                canonical_name=entry.name,
                category=entry.category,
                amount=resolved_amount,
                optional=optional,
            ))
        return results or None

    def _lookup(self, candidate: str) -> CanonicalIngredient | None:
        key = ingredient_key(candidate)
        return self._entries.get(key) or self._compact_entries.get(key.replace(" ", ""))

    def _resolve_candidate(self, candidate: str) -> CanonicalIngredient | None:
        entry = self._lookup(candidate)
        if entry is not None:
            return entry
        for fallback in fallback_ingredient_names(candidate):
            entry = self._lookup(fallback)
            if entry is not None:
                return entry
        return None

    def _is_excluded(self, candidate: str) -> bool:
        return ingredient_key(candidate) in self._excluded_keys

    def _compound_parts(self, candidate: str) -> list[str] | None:
        delimited = [part.strip() for part in COMPOUND_DELIMITER_RE.split(candidate)]
        if 2 <= len(delimited) <= 5 and all(delimited):
            # 분수 표현 보호: 모든 part가 한글을 포함하는 경우에만 compound로 처리
            if any(re.search(r'[가-힣]', part) for part in delimited):
                if all(self._resolve_candidate(part) is not None for part in delimited):
                    return delimited

        spaced = candidate.split()
        if 2 <= len(spaced) <= 4:
            resolved = [self._resolve_candidate(part) for part in spaced]
            if all(entry is not None for entry in resolved) and all(
                entry.name in SPLITTABLE_CANONICAL_NAMES for entry in resolved
            ):
                return spaced

        # 붙어 있는 두 단일 표준재료(예: "소금후추") 분리. SPLITTABLE_CANONICAL_NAMES로
        # 범위를 좁혀 임의의 접두/접미 일치로 잘못 쪼개는 일을 막는다.
        for name in sorted(SPLITTABLE_CANONICAL_NAMES, key=len, reverse=True):
            if candidate.startswith(name) and len(candidate) > len(name):
                remainder = candidate[len(name):]
                if remainder in SPLITTABLE_CANONICAL_NAMES:
                    return [name, remainder]
        return None


def fallback_ingredient_names(candidate: str) -> list[str]:
    """Return conservative description-stripped candidates for exact lookup."""
    results: list[str] = []
    queue = [candidate]
    seen = {candidate}
    patterns = (
        FALLBACK_PREFIX_RE,
        FALLBACK_SPACED_PREFIX_RE,
        FALLBACK_SUFFIX_RE,
        FALLBACK_PARENTHETICAL_RE,
        FALLBACK_TRAILING_DIGIT_RE,
        FALLBACK_TRAILING_LETTER_RE,
        FALLBACK_TRAILING_NOISE_RE,
        TRAILING_SIZE_SYMBOL_RE,
    )
    while queue and len(seen) < 12:
        current = queue.pop(0)
        for pattern in patterns:
            match = pattern.match(current)
            if not match:
                continue
            stripped = match.group("name").strip()
            if not stripped or stripped in seen:
                continue
            seen.add(stripped)
            results.append(stripped)
            queue.append(stripped)
    return results


def normalize_source_ingredient(
    raw_name: str,
    amount: str | None,
) -> tuple[str, str | None, bool]:
    candidate = normalize_ingredient_name(raw_name)
    optional = bool(OPTIONAL_PREFIX_RE.match(candidate))
    candidate = OPTIONAL_PREFIX_RE.sub("", candidate)
    candidate = LEADING_NOISE_RE.sub("", candidate).strip()
    candidate = MALFORMED_SIZE_PREFIX_RE.sub("", candidate).strip()
    candidate = re.sub(r"[\s~+:：.;,]+$", "", candidate).strip()

    if candidate.startswith("(") and candidate.endswith(")"):
        candidate = candidate[1:-1].strip()

    candidate = STAGE_PREFIX_RE.sub("", candidate)
    candidate = CONCENTRATION_PREFIX_RE.sub("", candidate)
    candidate = PERCENT_PREFIX_RE.sub("", candidate)
    candidate = TRAILING_SIZE_PAREN_RE.sub("", candidate).strip()
    candidate = re.sub(r"\s+추가$", "", candidate).strip()
    candidate = PREPARATION_PREFIX_RE.sub("", candidate).strip()

    size_match = TRAILING_SIZE_RE.match(candidate)
    if size_match:
        candidate = size_match.group("name").strip()
        amount = _merge_amounts(size_match.group("amount"), amount)
    else:
        size_symbol_match = TRAILING_SIZE_SYMBOL_RE.match(candidate)
        if size_symbol_match:
            candidate = size_symbol_match.group("name").strip()

    amount_preparation_match = TRAILING_AMOUNT_PREPARATION_RE.match(candidate)
    if amount_preparation_match:
        candidate = amount_preparation_match.group("name").strip()
        amount = _merge_amounts(amount_preparation_match.group("amount"), amount)
    else:
        preparation_match = TRAILING_PREPARATION_RE.match(candidate)
        if preparation_match:
            candidate = preparation_match.group("name").strip()

    leading_match = LEADING_AMOUNT_RE.match(candidate)
    if leading_match:
        candidate = leading_match.group("name").strip()
        amount = _merge_amounts(leading_match.group("amount"), amount)
    else:
        trailing_match = TRAILING_AMOUNT_RE.match(candidate)
        if trailing_match:
            candidate = trailing_match.group("name").strip()
            amount = _merge_amounts(trailing_match.group("amount"), amount)
        else:
            half_match = TRAILING_HALF_AMOUNT_RE.match(candidate)
            if half_match:
                candidate = half_match.group("name").strip()
                amount = _merge_amounts(half_match.group("amount"), amount)

    candidate = TRAILING_GENERIC_WORDS_RE.sub("", candidate).strip()
    candidate = re.sub(r"[\s~+:：.;,]+$", "", candidate).strip()
    return candidate, amount, optional


def _merge_amounts(first: str | None, second: str | None) -> str | None:
    values = [value.strip() for value in (first, second) if value and value.strip()]
    return " ".join(dict.fromkeys(values)) or None
