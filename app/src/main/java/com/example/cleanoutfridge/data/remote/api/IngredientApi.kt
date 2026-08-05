package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import com.example.cleanoutfridge.data.remote.dto.IngredientCategoryDto
import com.example.cleanoutfridge.data.remote.dto.IngredientDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** §4 표준 식재료 API. */
interface IngredientApi {

    /** §4.1 GET /ingredients */
    @GET("ingredients")
    suspend fun getIngredients(
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): Response<ApiResponse<PageResponse<IngredientDto>>>

    /** §4.2 GET /ingredients/{ingredientId} */
    @GET("ingredients/{ingredientId}")
    suspend fun getIngredient(
        @Path("ingredientId") ingredientId: Int,
    ): Response<ApiResponse<IngredientDto>>

    /** §4.3 GET /ingredient-categories */
    @GET("ingredient-categories")
    suspend fun getIngredientCategories(): Response<ApiResponse<List<IngredientCategoryDto>>>
}
