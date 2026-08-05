package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.Serializable

/** §7.1 recipe list row (summary, no ingredient breakdown). */
@Serializable
data class RecipeSummaryDto(
    val id: Int,
    val title: String,
    val method: String? = null,
    val ingredientActo: String? = null,
    val type: String? = null,
    val portion: String? = null,
    val cooktime: String? = null,
    val imageUrl: String? = null,
)

/** Ingredient line item inside a recipe (§7.2/7.3, and RecipeIngredient table). */
@Serializable
data class RecipeIngredientDto(
    val ingredient: IngredientDto,
    val amount: String? = null,
    val optional: Boolean = false,
)

/** §7.2 recipe detail. */
@Serializable
data class RecipeDetailDto(
    val id: Int,
    val title: String,
    val method: String? = null,
    val ingredientActo: String? = null,
    val type: String? = null,
    val ingredientAll: String? = null,
    val portion: String? = null,
    val cooktime: String? = null,
    val imageUrl: String? = null,
    val ingredients: List<RecipeIngredientDto> = emptyList(),
)

/** §8.4 GET /recipes/{id}/external-search response payload. */
@Serializable
data class ExternalSearchDto(
    val recipeId: Int,
    val title: String? = null,
    val provider: String,
    val url: String,
)
