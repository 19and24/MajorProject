package com.example.cleanoutfridge.model

data class ScenarioItem(
    val name: String,
    val category: String,
    val emoji: String,
    val confidence: Int,
)

data class ScanScenario(
    val scene: String,
    val emoji: String,
    val items: List<ScenarioItem>,
)

val AI_SCAN_SCENARIOS = listOf(
    ScanScenario(
        scene = "냉장고 내부",
        emoji = "🧊",
        items = listOf(
            ScenarioItem("우유", "유제품", "🥛", 97),
            ScenarioItem("계란", "계란", "🥚", 95),
            ScenarioItem("두부", "두부/콩류", "🫘", 89),
            ScenarioItem("방울토마토", "채소", "🍅", 91),
        ),
    ),
    ScanScenario(
        scene = "장보기 식재료",
        emoji = "🛒",
        items = listOf(
            ScenarioItem("삼겹살", "육류", "🥩", 94),
            ScenarioItem("시금치", "채소", "🥬", 88),
            ScenarioItem("그릭 요거트", "유제품", "🥛", 92),
            ScenarioItem("사과", "과일", "🍎", 98),
        ),
    ),
    ScanScenario(
        scene = "식탁 위 식재료",
        emoji = "🍽️",
        items = listOf(
            ScenarioItem("바나나", "과일", "🍌", 96),
            ScenarioItem("냉동만두", "냉동식품", "🧊", 85),
            ScenarioItem("스트링 치즈", "유제품", "🧀", 90),
        ),
    ),
)
