package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.api.RecommendationApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.dto.RecommendRecipesRequest
import com.example.cleanoutfridge.data.remote.dto.RecommendationResultDto
import com.example.cleanoutfridge.data.remote.dto.SelectableFridgeItemDto

/** §8 냉털 레시피 추천 API wrapper. */
class RecommendationRepository(
    private val api: RecommendationApi = ApiClient.recommendationApi,
) {
    /** §8.1 GET /recommendations/fridge-items */
    suspend fun getSelectableFridgeItems(
        starred: Boolean? = null,
        category: String? = null,
        includeExpired: Boolean? = null,
    ): List<SelectableFridgeItemDto> = apiCall {
        api.getSelectableFridgeItems(starred = starred, category = category, includeExpired = includeExpired)
    }.items

    /**
     * §8.2 POST /recommendations/recipes.
     *
     * Note per §9.4: an AI ranking failure is not an error — the server still returns
     * `success: true` with `recommendationMode: RULE_BASED_FALLBACK`. Callers should check
     * [RecommendationResultDto.recommendationMode] rather than expect this to throw for that case.
     * A thrown [ApiException] here means a genuine validation failure (§9.1/§9.2) or server error.
     */
    suspend fun recommendRecipes(
        selectedFridgeItemIds: List<Int>,
        excludedFridgeItemIds: List<Int> = emptyList(),
        prioritizeExpiring: Boolean = true,
        limit: Int = 20,
    ): RecommendationResultDto = apiCall {
        api.recommendRecipes(
            RecommendRecipesRequest(selectedFridgeItemIds, excludedFridgeItemIds, prioritizeExpiring, limit)
        )
    }

    /**
     * §8.3 POST /recommendations/recipes/filter — re-run recommendations after the user
     * adjusts their selection on the results screen. The spec allows the backend to omit
     * this endpoint entirely and reuse [recommendRecipes] instead, so a 404 here transparently
     * retries against the main endpoint.
     */
    suspend fun filterRecommendedRecipes(
        selectedFridgeItemIds: List<Int>,
        excludedFridgeItemIds: List<Int> = emptyList(),
        prioritizeExpiring: Boolean = true,
        limit: Int = 20,
    ): RecommendationResultDto {
        val request = RecommendRecipesRequest(selectedFridgeItemIds, excludedFridgeItemIds, prioritizeExpiring, limit)
        return try {
            apiCall { api.filterRecommendedRecipes(request) }
        } catch (e: ApiException) {
            if (e.httpStatus == 404) {
                apiCall { api.recommendRecipes(request) }
            } else {
                throw e
            }
        }
    }
}
