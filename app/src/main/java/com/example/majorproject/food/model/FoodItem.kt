package com.example.majorproject.food.model

data class FoodItem(
    val id: String,
    val name: String,
    val category: String,
    val expiryDate: String,
    val emoji: String,
    val addedDate: String,
)

enum class Tab { HOME, FOODS, ADD, STATS, SETTINGS }

enum class Filter { ALL, URGENT, EXPIRED, FRESH }
