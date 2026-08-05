package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** §5 냉장고 재고 API. */
interface FridgeItemApi {

    /** §5.1 GET /fridge-items */
    @GET("fridge-items")
    suspend fun getFridgeItems(
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null,
        @Query("starred") starred: Boolean? = null,
        @Query("expired") expired: Boolean? = null,
        @Query("deadlineBefore") deadlineBefore: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): Response<ApiResponse<PageResponse<FridgeItemDto>>>

    /** §5.2 GET /fridge-items/{fridgeItemId} */
    @GET("fridge-items/{fridgeItemId}")
    suspend fun getFridgeItem(
        @Path("fridgeItemId") fridgeItemId: Int,
    ): Response<ApiResponse<FridgeItemDto>>

    /** §5.3 POST /fridge-items */
    @POST("fridge-items")
    suspend fun createFridgeItem(
        @Body request: CreateFridgeItemRequest,
    ): Response<ApiResponse<FridgeItemDto>>

    /** §5.4 PATCH /fridge-items/{fridgeItemId} — only non-null fields are applied server-side. */
    @PATCH("fridge-items/{fridgeItemId}")
    suspend fun updateFridgeItem(
        @Path("fridgeItemId") fridgeItemId: Int,
        @Body request: UpdateFridgeItemRequest,
    ): Response<ApiResponse<FridgeItemDto>>

    /** §5.5 DELETE /fridge-items/{fridgeItemId} — also cascades to its Notifications server-side. */
    @DELETE("fridge-items/{fridgeItemId}")
    suspend fun deleteFridgeItem(
        @Path("fridgeItemId") fridgeItemId: Int,
    ): Response<Unit>

    /** §5.6 PATCH /fridge-items/{fridgeItemId}/star */
    @PATCH("fridge-items/{fridgeItemId}/star")
    suspend fun updateStar(
        @Path("fridgeItemId") fridgeItemId: Int,
        @Body request: UpdateStarRequest,
    ): Response<ApiResponse<StarResponse>>

    /** §5.7 GET /fridge-items/expiring */
    @GET("fridge-items/expiring")
    suspend fun getExpiringFridgeItems(
        @Query("days") days: Int? = null,
        @Query("includeExpired") includeExpired: Boolean? = null,
    ): Response<ApiResponse<List<ExpiringFridgeItemDto>>>

    /** §5.8 POST /fridge-items/{fridgeItemId}/consume */
    @POST("fridge-items/{fridgeItemId}/consume")
    suspend fun consumeFridgeItem(
        @Path("fridgeItemId") fridgeItemId: Int,
        @Body request: ConsumeFridgeItemRequest,
    ): Response<ApiResponse<ConsumeFridgeItemResponse>>

    /** §6.6 POST /fridge-items/{fridgeItemId}/notifications/default */
    @POST("fridge-items/{fridgeItemId}/notifications/default")
    suspend fun createDefaultNotifications(
        @Path("fridgeItemId") fridgeItemId: Int,
        @Body request: CreateDefaultNotificationsRequest,
    ): Response<ApiResponse<CreateDefaultNotificationsResponse>>
}
