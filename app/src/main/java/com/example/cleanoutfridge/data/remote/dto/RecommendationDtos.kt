package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.Serializable

/** §8.1 fridge-item stock status used for the "냉털" ingredient picker. */
enum class FridgeStockStatus {
    FRESH, EXPIRING, EXPIRED
}

/** §8.2 how a recipe ingredient maps onto the user's fridge stock. */
enum class OwnershipStatus {
    AVAILABLE, EXPIRING, MISSING
}

/** §9.4 whether the recommendation came from the AI ranker or the rule-based fallback. */
enum class RecommendationMode {
    AI_RANKED, RULE_BASED_FALLBACK
}

/** §8.1 GET /recommendations/fridge-items item. */
@Serializable
data class SelectableFridgeItemDto(
    val fridgeItemId: Int,
    val ingredient: IngredientDto,
    val quantity: Double,
    val unit: String? = null,
    val deadline: String? = null,
    val daysUntilDeadline: Int? = null,
    val status: FridgeStockStatus,
    val selectable: Boolean,
    val selectedByDefault: Boolean = false,
    val disabledReason: String? = null,
)

@Serializable
data class SelectableFridgeItemsData(
    val items: List<SelectableFridgeItemDto> = emptyList(),
)

/** §8.2/8.3 request body shared by the recommend and re-filter endpoints. */
@Serializable
data class RecommendRecipesRequest(
    val selectedFridgeItemIds: List<Int>,
    val excludedFridgeItemIds: List<Int> = emptyList(),
    val prioritizeExpiring: Boolean = true,
    val limit: Int = 20,
)

/** Echo of a selected fridge item in the recommendation response. */
@Serializable
data class SelectedFridgeItemSummaryDto(
    val fridgeItemId: Int,
    val ingredientId: Int,
    val name: String? = null,
    val status: FridgeStockStatus,
)

/** Recipe reference embedded in a recommendation entry. */
@Serializable
data class RecommendedRecipeRefDto(
    val id: Int,
    val title: String,
    val imageUrl: String? = null,
    val method: String? = null,
    val type: String? = null,
    val portion: String? = null,
    val cooktime: String? = null,
)

/** One ingredient line inside a recommendation, annotated with ownership status. */
@Serializable
data class RecommendationIngredientDto(
    val ingredientId: Int,
    val name: String,
    val amount: String? = null,
    val ownershipStatus: OwnershipStatus,
    val fridgeItemId: Int? = null,
)

@Serializable
data class ExternalSearchRefDto(
    val provider: String,
    val url: String,
)

/** A single ranked recipe recommendation. */
@Serializable
data class RecipeRecommendationDto(
    val recipe: RecommendedRecipeRefDto,
    val score: Double? = null,
    val matchRate: Double? = null,
    val expiringTargeted: Boolean = false,
    val recommendationReason: String? = null,
    val ingredients: List<RecommendationIngredientDto> = emptyList(),
    val externalSearch: ExternalSearchRefDto? = null,
)

/** §8.2/8.3/9.3/9.4 response payload — most fields are optional because the exception-
 * response variants (§9.3 empty result, §9.4 AI fallback) omit some of them. */
@Serializable
data class RecommendationResultDto(
    val selectedFridgeItems: List<SelectedFridgeItemSummaryDto> = emptyList(),
    val recommendations: List<RecipeRecommendationDto> = emptyList(),
    val emptyReason: String? = null,
    val recommendationMode: RecommendationMode? = null,
)

/** §9.2 error payload: `data.invalidFridgeItemIds` on EXPIRED_FRIDGE_ITEM_SELECTED. */
@Serializable
data class InvalidFridgeItemIdsData(
    val invalidFridgeItemIds: List<Int> = emptyList(),
)
