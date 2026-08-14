package com.example.cleanoutfridge.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.NetworkUnavailableException
import com.example.cleanoutfridge.data.remote.dto.RecipeDetailDto
import com.example.cleanoutfridge.data.repository.RecipeRepository
import kotlinx.coroutines.launch

sealed interface RecipeDetailState {
    data object Loading : RecipeDetailState
    data class Success(val recipe: RecipeDetailDto) : RecipeDetailState
    data class Error(val message: String) : RecipeDetailState
}

/** §7.2/§8.4 레시피 상세 화면 상태. Call [load] once per recipeId (e.g. from LaunchedEffect). */
class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    var state: RecipeDetailState by mutableStateOf(RecipeDetailState.Loading)
        private set

    private var loadedRecipeId: Int? = null

    fun load(recipeId: Int) {
        if (loadedRecipeId == recipeId) return
        loadedRecipeId = recipeId
        viewModelScope.launch {
            state = RecipeDetailState.Loading
            state = try {
                RecipeDetailState.Success(recipeRepository.getRecipe(recipeId))
            } catch (e: Exception) {
                RecipeDetailState.Error(e.toUserMessage())
            }
        }
    }
}

private fun Exception.toUserMessage(): String = when (this) {
    is NetworkUnavailableException -> message ?: "네트워크 연결을 확인해주세요."
    is ApiException -> message
    else -> message ?: "알 수 없는 오류가 발생했어요."
}
