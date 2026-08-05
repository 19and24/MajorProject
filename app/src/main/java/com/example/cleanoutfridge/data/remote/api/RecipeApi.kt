package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import com.example.cleanoutfridge.data.remote.dto.ExternalSearchDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse
import com.example.cleanoutfridge.data.remote.dto.RecipeDetailDto
import com.example.cleanoutfridge.data.remote.dto.RecipeIngredientDto
import com.example.cleanoutfridge.data.remote.dto.RecipeSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** §7 레시피 API + §8.4 외부 검색 URL. */
interface RecipeApi {

    /** §7.1 GET /recipes */
    @GET("recipes")
    suspend fun getRecipes(
        @Query("keyword") keyword: String? = null,
        @Query("method") method: String? = null,
        @Query("type") type: String? = null,
        @Query("ingredientId") ingredientId: Int? = null,
        @Query("portion") portion: String? = null,
        @Query("cooktime") cooktime: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): Response<ApiResponse<PageResponse<RecipeSummaryDto>>>

    /** §7.2 GET /recipes/{recipeId} */
    @GET("recipes/{recipeId}")
    suspend fun getRecipe(
        @Path("recipeId") recipeId: Int,
    ): Response<ApiResponse<RecipeDetailDto>>

    /** §7.3 GET /recipes/{recipeId}/ingredients */
    @GET("recipes/{recipeId}/ingredients")
    suspend fun getRecipeIngredients(
        @Path("recipeId") recipeId: Int,
    ): Response<ApiResponse<List<RecipeIngredientDto>>>

    /** §8.4 GET /recipes/{recipeId}/external-search — optional; recommendations already embed this. */
    @GET("recipes/{recipeId}/external-search")
    suspend fun getExternalSearch(
        @Path("recipeId") recipeId: Int,
    ): Response<ApiResponse<ExternalSearchDto>>
}
