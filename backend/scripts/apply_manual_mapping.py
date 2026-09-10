"""Merge hand-written ``mapping_manually.txt`` into ``ingredient_mapping.csv``.

The curator keeps adding blocks to ``backend/mapping_manually.txt``:

* ``## 새 표준재료``  – new canonical ingredients (``표준명 | 별칭,별칭 # 메모``)
* ``## 매핑 개선``    – aliases for existing canonicals (``원문, 원문 -> 표준명 # 메모``)
* ``## 수식어`` / ``## 복합 재료`` – notes only, applied by hand.

Run ``python scripts/apply_manual_mapping.py`` for a dry-run report, then
``--apply`` to rewrite the CSV. Re-runnable: already-present aliases are skipped.
"""

import argparse
import csv
import re
import sys
from pathlib import Path

try:
    from .parse_ingredients import ingredient_key
except ImportError:
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from parse_ingredients import ingredient_key

DATA_DIR = Path(__file__).resolve().parents[1] / "data"
MAPPING_PATH = DATA_DIR / "ingredient_mapping.csv"
MANUAL_PATH = Path(__file__).resolve().parents[1] / "mapping_manually.txt"

# Target names in 매핑 개선 that are typos or need redirecting to a real canonical.
TARGET_FIXES = {
    "구간장": "국간장",
    "당먄": "당면",
    "들께가루": "들깨가루",
    "말린 고추": "건고추",
    "마늘후레이크": "마늘",
}
# 새 표준재료 LEFT 이름이 기존 표준재료로 흡수되어야 하는 경우.
NEW_CANON_REDIRECT = {
    "가스오부시 쯔유": "쯔유",
    "하이라이스": "하이라이스가루",
    "건두부 면": "두부면",
}
# ``->`` 없이 의미만 있는 매핑 개선 줄, 그리고 정규화가 접두사를 먹어
# 원문 그대로는 조회되지 않는 경우의 도달 가능한 별칭.
EXTRA_ALIASES = {"건고추": ["건청양고추"], "돈가스": ["카츠"]}
# Best-guess category for brand-new canonicals; the curator can correct these.
CATEGORY_GUESS = [
    (r"치즈", "달걀/유제품"),
    (r"쯔유|소스|육수|젓|장$|청$|카레|하이라이스|마요", "조미료/양념"),
    (r"김치", "김치/절임류"),
    (r"오징어|골뱅이|새우|게$|조개|굴$", "해산물류"),
    (r"복숭아|오미자|크린베리|크랜베리|참외", "과일류"),
    (r"두부|견과|정과|콩", "콩/견과류"),
    (r"쌀가루|가루$|면$|사리|라이스|후레이크", "곡류/면/가루"),
    (r"녹차|말차", "음료/주류"),
    (r"호박|다래순|나물", "채소류"),
]
NEW_ROW_CATEGORY_FALLBACK = "가공식품류"
NEW_ROW_NOTE = "수동 매핑(mapping_manually.txt)"
AUGMENT_NOTE = "수동 매핑 보강"


def guess_category(name: str) -> str:
    for pattern, category in CATEGORY_GUESS:
        if re.search(pattern, name):
            return category
    return NEW_ROW_CATEGORY_FALLBACK


def parse_manual(text: str):
    """Return (new_canon, improvements) from the manual file.

    ``new_canon``: list of (canonical, [aliases]) from ``## 새 표준재료``.
    ``improvements``: list of (target, [raw_names], had_arrow) from ``## 매핑 개선``.
    """
    section = None
    new_canon: list[tuple[str, list[str]]] = []
    improvements: list[tuple[str, list[str], bool]] = []
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("## "):
            section = stripped[3:].strip()
            continue
        if not stripped or stripped.startswith("#"):
            continue
        stripped = stripped.split("#", 1)[0].strip()
        if not stripped:
            continue

        if section == "새 표준재료":
            canonical, _, rest = stripped.partition("|")
            aliases = [a.strip().rstrip(".") for a in rest.split(",") if a.strip().strip(".")]
            new_canon.append((canonical.strip(), aliases))
        elif section == "매핑 개선":
            if "->" not in stripped:
                continue
            left, _, target = stripped.partition("->")
            target = re.sub(r"\s*\([^)]*\)\s*$", "", target.strip()).strip()
            target = re.split(r"\s+또는\s+|\s+혹은\s+", target)[0].strip()
            target = TARGET_FIXES.get(target, target)
            raws = [r.strip() for r in left.split(",") if r.strip()]
            if target and raws:
                improvements.append((target, raws, True))
    for target, raws in EXTRA_ALIASES.items():
        improvements.append((target, raws, True))
    return new_canon, improvements


def load_rows():
    with MAPPING_PATH.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        return reader.fieldnames, list(reader)


def build_index(rows):
    """key -> canonical_name, for every canonical and alias already present."""
    index: dict[str, str] = {}
    for row in rows:
        names = [row["canonical_name"], *row["included_names"].split("|")]
        for name in names:
            name = name.strip()
            if name:
                index.setdefault(ingredient_key(name), row["canonical_name"])
    return index


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="CSV를 실제로 덮어씁니다")
    args = parser.parse_args()

    fieldnames, rows = load_rows()

    # Drop rows a previous run of this script created, so re-running rebuilds
    # them from scratch (idempotent). Alias-augmented rows are left alone;
    # already-present aliases are skipped by ``attach`` below.
    rows = [row for row in rows if not row["notes"].startswith(NEW_ROW_NOTE)]

    by_canonical = {row["canonical_name"]: row for row in rows}
    index = build_index(rows)

    new_canon, improvements = parse_manual(MANUAL_PATH.read_text(encoding="utf-8"))

    added: dict[str, list[str]] = {}   # canonical -> newly attached raw names
    created: list[str] = []
    skipped_dup: list[tuple[str, str]] = []  # (raw, existing canonical)
    unresolved: list[tuple[str, list[str]]] = []

    def attach(canonical: str, raws: list[str]) -> None:
        row = by_canonical[canonical]
        for raw in raws:
            key = ingredient_key(raw)
            owner = index.get(key)
            if owner == canonical:
                continue
            if owner is not None:
                skipped_dup.append((raw, owner))
                continue
            index[key] = canonical
            row["included_names"] += f"|{raw}"
            row["aliases"] = (row["aliases"] + f"|{raw}").lstrip("|")
            row["source_name_count"] = str(int(row["source_name_count"] or 0) + 1)
            added.setdefault(canonical, []).append(raw)
        if canonical in added and AUGMENT_NOTE not in row["notes"]:
            row["notes"] = f"{row['notes']}; {AUGMENT_NOTE}".lstrip("; ")

    # 1) new canonicals (or alias-adds when the name already exists)
    for canonical, aliases in new_canon:
        redirect = NEW_CANON_REDIRECT.get(canonical)
        if redirect is not None:
            attach(redirect, [canonical, *aliases])
            continue
        owner = index.get(ingredient_key(canonical))
        if owner is not None:
            attach(owner, ([canonical] if owner != canonical else []) + aliases)
            continue
        category = guess_category(canonical)
        row = {name: "" for name in fieldnames}
        row.update(
            canonical_name=canonical,
            category=category,
            # The loader only reads ``included_names``; ``aliases`` mirrors it
            # (canonical dropped) to match the rest of the file's convention.
            included_names="|".join([canonical, *aliases]),
            aliases="|".join(aliases),
            source_frequency="0",
            source_name_count=str(1 + len(aliases)),
            review_status="수동",
            notes=NEW_ROW_NOTE,
        )
        rows.append(row)
        by_canonical[canonical] = row
        index[ingredient_key(canonical)] = canonical
        for alias in aliases:
            index.setdefault(ingredient_key(alias), canonical)
        created.append(f"{canonical}  [{category}]  +{len(aliases)} aliases")

    # 2) alias improvements for existing canonicals
    for target, raws, _ in improvements:
        owner = index.get(ingredient_key(target)) or by_canonical.get(target, {}).get(
            "canonical_name"
        )
        if owner is None:
            unresolved.append((target, raws))
            continue
        attach(owner, raws)

    # ---- report ----
    print(f"새 표준재료 생성: {len(created)}")
    for line in created:
        print(f"  + {line}")
    total_aliases = sum(len(v) for v in added.values())
    print(f"\n별칭 추가: {total_aliases}건 / 표준재료 {len(added)}개")
    for canonical, raws in sorted(added.items()):
        print(f"  {canonical}: {', '.join(raws)}")
    print(f"\n이미 매핑되어 건너뜀: {len(skipped_dup)}")
    for raw, owner in skipped_dup:
        print(f"  {raw}  (이미 {owner})")
    print(f"\n표준재료를 못 찾음 (수동 확인 필요): {len(unresolved)}")
    for target, raws in unresolved:
        print(f"  -> {target}  <=  {', '.join(raws)}")

    if not args.apply:
        print("\n(dry-run) 반영하려면 --apply")
        return 0

    with MAPPING_PATH.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    print(f"\n반영 완료: {MAPPING_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
