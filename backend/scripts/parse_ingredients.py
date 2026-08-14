"""Parsing helpers for CKG_MTRL_CN (recipe ingredient text) from the 10000recipe CSV dump.

Format example:
  [재료] 어묵 2개| 김밥용김 3장| 당면 1움큼 [양념] 간장 1T| 참기름 1T

Sections are marked by a bracketed label; items inside a section are separated by `|`.
Each item is "이름" optionally followed by an amount (e.g. "2개", "1/2컵", "약간").
"""
import re

SECTION_RE = re.compile(r"\[[^\[\]]*\]")
# Amount = digits/fractions/decimals optionally followed by a unit word, or bare
# quantity words like 약간/적당량/조금/약간씩, allowing an optional leading space.
AMOUNT_RE = re.compile(
    r"\s*("
    r"[0-9][0-9./+~\s]*[가-힣a-zA-Z]*"  # e.g. "2개", "1/2컵", "1+1/2공기", "50g", "1T"
    r"|약간씩|약간|적당량|적당히|조금|넉넉히|소량"
    r")\s*$"
)


def split_sections(raw: str) -> list[str]:
    """Splits on section markers, returning only the item-list chunks (labels dropped)."""
    if not raw:
        return []
    parts = SECTION_RE.split(raw)
    return [p for p in parts if p.strip()]


def parse_items(raw: str) -> list[tuple[str, str | None]]:
    """Returns a list of (ingredient_name, amount_or_None) tuples parsed from CKG_MTRL_CN."""
    results: list[tuple[str, str | None]] = []
    for section in split_sections(raw):
        for item in section.split("|"):
            item = item.strip().replace("?", "").strip()
            if not item:
                continue
            name, amount = _split_name_amount(item)
            name = name.strip()
            if not name:
                continue
            results.append((name, amount))
    return results


def _split_name_amount(item: str) -> tuple[str, str | None]:
    match = AMOUNT_RE.search(item)
    if not match or match.start() == 0:
        return item, None
    name = item[: match.start()]
    amount = match.group(1).strip()
    return name, amount or None
