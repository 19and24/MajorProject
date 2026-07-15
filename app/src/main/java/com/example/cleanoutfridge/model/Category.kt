package com.example.cleanoutfridge.model

data class Category(
    val id: String,
    val label: String,
    val emoji: String,
    val defaultDays: Int,
)

val CATEGORIES = listOf(
    Category(id = "유제품", label = "유제품", emoji = "🥛", defaultDays = 7),
    Category(id = "육류", label = "육류", emoji = "🥩", defaultDays = 3),
    Category(id = "채소", label = "채소", emoji = "🥬", defaultDays = 7),
    Category(id = "과일", label = "과일", emoji = "🍎", defaultDays = 5),
    Category(id = "냉동식품", label = "냉동식품", emoji = "🧊", defaultDays = 90),
    Category(id = "계란", label = "계란", emoji = "🥚", defaultDays = 21),
    Category(id = "두부/콩류", label = "두부/콩류", emoji = "🫘", defaultDays = 5),
    Category(id = "조미료", label = "조미료", emoji = "🫙", defaultDays = 180),
    Category(id = "가공식품", label = "가공식품", emoji = "🥫", defaultDays = 14),
    Category(id = "기타", label = "기타", emoji = "🍱", defaultDays = 7),
)

fun categoryById(id: String): Category? = CATEGORIES.find { it.id == id }
