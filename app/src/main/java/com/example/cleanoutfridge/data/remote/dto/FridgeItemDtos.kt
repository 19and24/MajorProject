package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.Serializable

/** §5 FridgeItem representation returned by list/detail/create/update endpoints. */
@Serializable
data class FridgeItemDto(
    val id: Int,
    val ingredient: IngredientDto,
    val quantity: Double,
    val unit: String? = null,
    val deadline: String? = null,
    val starred: Boolean = false,
    val expired: Boolean? = null,
    val daysUntilDeadline: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** §5.3 POST /fridge-items request body. */
@Serializable
data class CreateFridgeItemRequest(
    val ingredientId: Int,
    val quantity: Double,
    val unit: String? = null,
    val deadline: String? = null,
    val starred: Boolean = false,
)

/** §5.4 PATCH /fridge-items/{id} request body — all fields optional, only sent ones are updated. */
@Serializable
data class UpdateFridgeItemRequest(
    val quantity: Double? = null,
    val unit: String? = null,
    val deadline: String? = null,
    val starred: Boolean? = null,
)

/** §5.6 PATCH /fridge-items/{id}/star request body. */
@Serializable
data class UpdateStarRequest(
    val starred: Boolean,
)

/** §5.6 response payload. */
@Serializable
data class StarResponse(
    val id: Int,
    val starred: Boolean,
)

/** §5.7 GET /fridge-items/expiring item. */
@Serializable
data class ExpiringFridgeItemDto(
    val id: Int,
    val ingredient: IngredientDto,
    val quantity: Double,
    val unit: String? = null,
    val deadline: String? = null,
    val daysUntilDeadline: Int? = null,
    val expired: Boolean = false,
)

/** §5.8 POST /fridge-items/{id}/consume request body. */
@Serializable
data class ConsumeFridgeItemRequest(
    val quantity: Double,
)

/** §5.8 response payload. */
@Serializable
data class ConsumeFridgeItemResponse(
    val id: Int,
    val remainingQuantity: Double,
    val unit: String? = null,
    val depleted: Boolean = false,
)
