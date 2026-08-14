package com.example.cleanoutfridge.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.NetworkUnavailableException
import com.example.cleanoutfridge.data.remote.dto.RecipeSummaryDto
import com.example.cleanoutfridge.data.repository.RecipeRepository
import kotlinx.coroutines.launch

sealed interface RecipeSearchState {
    data object Idle : RecipeSearchState
    data object Loading : RecipeSearchState
    data class Success(val items: List<RecipeSummaryDto>, val totalElements: Int) : RecipeSearchState
    data class Error(val message: String) : RecipeSearchState
}

/** §7.1 레시피 검색 화면 상태. */
class RecipeSearchViewModel(
    private val recipeRepository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    var state: RecipeSearchState by mutableStateOf(RecipeSearchState.Idle)
        private set

    var keyword: String by mutableStateOf("")
        private set

    fun onKeywordChange(value: String) {
        keyword = value
    }

    fun search() {
        val query = keyword.trim()
        viewModelScope.launch {
            state = RecipeSearchState.Loading
            state = try {
                val page = recipeRepository.getRecipes(
                    keyword = query.ifEmpty { null },
                    size = 30,
                )
                RecipeSearchState.Success(page.items, page.totalElements)
            } catch (e: Exception) {
                RecipeSearchState.Error(e.toUserMessage())
            }
        }
    }
}

private fun Exception.toUserMessage(): String = when (this) {
    is NetworkUnavailableException -> message ?: "네트워크 연결을 확인해주세요."
    is ApiException -> message
    else -> message ?: "알 수 없는 오류가 발생했어요."
}
