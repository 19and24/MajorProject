package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.Serializable

/** §10.1 GET /dashboard expiring item row. */
@Serializable
data class DashboardExpiringItemDto(
    val fridgeItemId: Int,
    val ingredientName: String? = null,
    val deadline: String? = null,
    val daysUntilDeadline: Int? = null,
)

/** §10.1 GET /dashboard recommended recipe row. */
@Serializable
data class DashboardRecommendedRecipeDto(
    val id: Int,
    val title: String,
    val score: Double? = null,
)

/** §10.1 GET /dashboard response payload. */
@Serializable
data class DashboardSummaryDto(
    val totalFridgeItemCount: Int = 0,
    val starredFridgeItemCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val expiredCount: Int = 0,
    val upcomingNotificationCount: Int = 0,
    val expiringItems: List<DashboardExpiringItemDto> = emptyList(),
    val recommendedRecipes: List<DashboardRecommendedRecipeDto> = emptyList(),
)
