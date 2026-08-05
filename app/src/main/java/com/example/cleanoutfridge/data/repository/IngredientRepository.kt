package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.api.IngredientApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.dto.IngredientCategoryDto
import com.example.cleanoutfridge.data.remote.dto.IngredientDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse

/** §4 표준 식재료 API wrapper. */
class IngredientRepository(
    private val api: IngredientApi = ApiClient.ingredientApi,
) {
    suspend fun getIngredients(
        keyword: String? = null,
        category: String? = null,
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
    ): PageResponse<IngredientDto> = apiCall {
        api.getIngredients(keyword = keyword, category = category, page = page, size = size, sort = sort)
    }

    suspend fun getIngredient(ingredientId: Int): IngredientDto = apiCall {
        api.getIngredient(ingredientId)
    }

    suspend fun getIngredientCategories(): List<IngredientCategoryDto> = apiCall {
        api.getIngredientCategories()
    }
}
