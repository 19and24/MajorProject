package com.example.cleanoutfridge.model

/**
 * UI-facing model for a fridge item. [id] is the server's FridgeItem id; screens key list rows
 * on it as a String for compatibility with existing Compose `key = { it.id }` usage.
 */
data class FoodItem(
    val id: String,
    val ingredientId: Int,
    val name: String,
    val category: String,
    val expiryDate: String,
    val emoji: String,
    val addedDate: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val starred: Boolean = false,
)

enum class Tab { HOME, FRIDGE, ADD, STATS, CLEANOUT }
