package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import com.example.cleanoutfridge.data.remote.dto.RecommendRecipesRequest
import com.example.cleanoutfridge.data.remote.dto.RecommendationResultDto
import com.example.cleanoutfridge.data.remote.dto.SelectableFridgeItemsData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** §8 냉털 레시피 추천 API. */
interface RecommendationApi {

    /** §8.1 GET /recommendations/fridge-items */
    @GET("recommendations/fridge-items")
    suspend fun getSelectableFridgeItems(
        @Query("starred") starred: Boolean? = null,
        @Query("category") category: String? = null,
        @Query("includeExpired") includeExpired: Boolean? = null,
    ): Response<ApiResponse<SelectableFridgeItemsData>>

    /** §8.2 POST /recommendations/recipes */
    @POST("recommendations/recipes")
    suspend fun recommendRecipes(
        @Body request: RecommendRecipesRequest,
    ): Response<ApiResponse<RecommendationResultDto>>

    /**
     * §8.3 POST /recommendations/recipes/filter — re-run recommendations after the user
     * tweaks the selection. Per the spec this may legitimately 404 if the backend chose to
     * omit it and just re-call [recommendRecipes] instead; callers should fall back to that.
     */
    @POST("recommendations/recipes/filter")
    suspend fun filterRecommendedRecipes(
        @Body request: RecommendRecipesRequest,
    ): Response<ApiResponse<RecommendationResultDto>>
}
