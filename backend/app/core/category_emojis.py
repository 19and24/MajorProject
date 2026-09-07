"""Display emoji for each standard ingredient category (values sourced from the
CKG_MTRL_ACTO_NM column of the imported recipe dataset)."""

CATEGORY_EMOJIS: dict[str, str] = {
    "채소류": "\U0001f96c",       # 🥬
    "해물류": "\U0001f990",       # 🦐
    "가공식품류": "\U0001f96b",   # 🥫
    "밀가루": "\U0001f33e",       # 🌾
    "돼지고기": "\U0001f416",     # 🐖
    "달걀/유제품": "\U0001f95a",  # 🥚
    "소고기": "\U0001f404",       # 🐄
    "기타": "\U0001f9fa",         # 🧺
    "닭고기": "\U0001f414",       # 🐔
    "콩/견과류": "\U0001f95c",    # 🥜
    "쌀": "\U0001f35a",           # 🍚
    "과일류": "\U0001f34e",       # 🍎
    "건어물류": "\U0001f41f",     # 🐟
    "버섯류": "\U0001f344",       # 🍄
    "육류": "\U0001f969",         # 🥩
    "곡류": "\U0001f33e",         # 🌾
    "조미료/양념": "\U0001f9c2",  # 🧂
    "유지류": "\U0001fad2",       # 🫒
    "물/육수": "\U0001f4a7",     # 💧
    "기본재료": "\U0001f4a7",    # 💧
    "곡류/면/가루": "\U0001f33e", # 🌾
    "김치/절임류": "\U0001f96c", # 🥬
    "향신료/허브": "\U0001f33f", # 🌿
    "음료/주류": "\U0001f964",   # 🥤
}

DEFAULT_EMOJI = "\U0001f37d"  # 🍽
