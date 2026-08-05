package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.api.RecipeApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.dto.ExternalSearchDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse
import com.example.cleanoutfridge.data.remote.dto.RecipeDetailDto
import com.example.cleanoutfridge.data.remote.dto.RecipeIngredientDto
import com.example.cleanoutfridge.data.remote.dto.RecipeSummaryDto

/** §7 레시피 API + §8.4 외부 검색 URL wrapper. */
class RecipeRepository(
    private val api: RecipeApi = ApiClient.recipeApi,
) {
    suspend fun getRecipes(
        keyword: String? = null,
        method: String? = null,
        type: String? = null,
        ingredientId: Int? = null,
        portion: String? = null,
        cooktime: String? = null,
        page: Int? = null,
        size: Int? = null,
    ): PageResponse<RecipeSummaryDto> = apiCall {
        api.getRecipes(
            keyword = keyword,
            method = method,
            type = type,
            ingredientId = ingredientId,
            portion = portion,
            cooktime = cooktime,
            page = page,
            size = size,
        )
    }

    suspend fun getRecipe(recipeId: Int): RecipeDetailDto = apiCall {
        api.getRecipe(recipeId)
    }

    suspend fun getRecipeIngredients(recipeId: Int): List<RecipeIngredientDto> = apiCall {
        api.getRecipeIngredients(recipeId)
    }

    /**
     * §8.4 — usually unnecessary since [com.example.cleanoutfridge.data.remote.dto.RecipeRecommendationDto]
     * already embeds `externalSearch`; call this only if a screen needs the URL for a recipe
     * fetched outside the recommendation flow (e.g. from [getRecipe]).
     */
    suspend fun getExternalSearch(recipeId: Int): ExternalSearchDto = apiCall {
        api.getExternalSearch(recipeId)
    }
}
