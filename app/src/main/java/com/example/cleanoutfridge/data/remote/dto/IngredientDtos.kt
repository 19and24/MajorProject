package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.Serializable

/** Standard ingredient master data (§2 Ingredient / §4). */
@Serializable
data class IngredientDto(
    val id: Int,
    val name: String,
    val category: String? = null,
    val emoji: String? = null,
)

/** §4.3 GET /ingredient-categories item. */
@Serializable
data class IngredientCategoryDto(
    val name: String,
    val emoji: String? = null,
)
