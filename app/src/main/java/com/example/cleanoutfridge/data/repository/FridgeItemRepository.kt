package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.api.FridgeItemApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.apiCallUnit
import com.example.cleanoutfridge.data.remote.dto.ConsumeFridgeItemRequest
import com.example.cleanoutfridge.data.remote.dto.ConsumeFridgeItemResponse
import com.example.cleanoutfridge.data.remote.dto.CreateDefaultNotificationsRequest
import com.example.cleanoutfridge.data.remote.dto.CreateDefaultNotificationsResponse
import com.example.cleanoutfridge.data.remote.dto.CreateFridgeItemRequest
import com.example.cleanoutfridge.data.remote.dto.ExpiringFridgeItemDto
import com.example.cleanoutfridge.data.remote.dto.FridgeItemDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse
import com.example.cleanoutfridge.data.remote.dto.StarResponse
import com.example.cleanoutfridge.data.remote.dto.UpdateFridgeItemRequest
import com.example.cleanoutfridge.data.remote.dto.UpdateStarRequest

/** §5 냉장고 재고 API wrapper. */
class FridgeItemRepository(
    private val api: FridgeItemApi = ApiClient.fridgeItemApi,
) {
    suspend fun getFridgeItems(
        keyword: String? = null,
        category: String? = null,
        starred: Boolean? = null,
        expired: Boolean? = null,
        deadlineBefore: String? = null,
        sort: String? = null,
        page: Int? = null,
        size: Int? = null,
    ): PageResponse<FridgeItemDto> = apiCall {
        api.getFridgeItems(
            keyword = keyword,
            category = category,
            starred = starred,
            expired = expired,
            deadlineBefore = deadlineBefore,
            sort = sort,
            page = page,
            size = size,
        )
    }

    suspend fun getFridgeItem(fridgeItemId: Int): FridgeItemDto = apiCall {
        api.getFridgeItem(fridgeItemId)
    }

    suspend fun createFridgeItem(request: CreateFridgeItemRequest): FridgeItemDto = apiCall {
        api.createFridgeItem(request)
    }

    suspend fun updateFridgeItem(fridgeItemId: Int, request: UpdateFridgeItemRequest): FridgeItemDto = apiCall {
        api.updateFridgeItem(fridgeItemId, request)
    }

    /** Also removes any Notifications linked to this item, per §5.5. */
    suspend fun deleteFridgeItem(fridgeItemId: Int): Unit = apiCallUnit {
        api.deleteFridgeItem(fridgeItemId)
    }

    suspend fun setStarred(fridgeItemId: Int, starred: Boolean): StarResponse = apiCall {
        api.updateStar(fridgeItemId, UpdateStarRequest(starred))
    }

    suspend fun getExpiringFridgeItems(
        days: Int? = null,
        includeExpired: Boolean? = null,
    ): List<ExpiringFridgeItemDto> = apiCall {
        api.getExpiringFridgeItems(days = days, includeExpired = includeExpired)
    }

    /** Deducts [quantity] from the item's current stock; server reports `depleted` when it hits zero (§5.8). */
    suspend fun consumeFridgeItem(fridgeItemId: Int, quantity: Double): ConsumeFridgeItemResponse = apiCall {
        api.consumeFridgeItem(fridgeItemId, ConsumeFridgeItemRequest(quantity))
    }

    suspend fun createDefaultNotifications(
        fridgeItemId: Int,
        daysBefore: List<Int>,
        notifyHour: Int,
    ): CreateDefaultNotificationsResponse = apiCall {
        api.createDefaultNotifications(fridgeItemId, CreateDefaultNotificationsRequest(daysBefore, notifyHour))
    }
}
