package com.example.cleanoutfridge.model

data class FoodItem(
    val id: String,
    val name: String,
    val category: String,
    val expiryDate: String,
    val emoji: String,
    val addedDate: String,
)

enum class Tab { HOME, FRIDGE, ADD, STATS, CLEANOUT }
