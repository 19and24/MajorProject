package com.example.cleanoutfridge.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.NetworkUnavailableException
import com.example.cleanoutfridge.data.remote.dto.RecipeRecommendationDto
import com.example.cleanoutfridge.data.remote.dto.SelectableFridgeItemDto
import com.example.cleanoutfridge.data.repository.RecommendationRepository
import kotlinx.coroutines.launch

sealed interface SelectableItemsState {
    data object Loading : SelectableItemsState
    data class Success(val items: List<SelectableFridgeItemDto>) : SelectableItemsState
    data class Error(val message: String) : SelectableItemsState
}

sealed interface RecommendationState {
    data object Idle : RecommendationState
    data object Loading : RecommendationState
    data class Success(
        val recommendations: List<RecipeRecommendationDto>,
        val emptyReason: String?,
    ) : RecommendationState
    data class Error(val message: String) : RecommendationState
}

/** §8 냉털 레시피 추천 화면 상태. */
class CleanOutViewModel(
    private val recommendationRepository: RecommendationRepository = RecommendationRepository(),
) : ViewModel() {

    var selectableState: SelectableItemsState by mutableStateOf(SelectableItemsState.Loading)
        private set

    var recommendationState: RecommendationState by mutableStateOf(RecommendationState.Idle)
        private set

    var selectedFridgeItemIds: Set<Int> by mutableStateOf(emptySet())
        private set

    init {
        loadSelectableItems()
    }

    fun loadSelectableItems() {
        viewModelScope.launch {
            selectableState = SelectableItemsState.Loading
            selectableState = try {
                val items = recommendationRepository.getSelectableFridgeItems()
                selectedFridgeItemIds = items
                    .filter { it.selectedByDefault }
                    .map { it.fridgeItemId }
                    .toSet()
                SelectableItemsState.Success(items)
            } catch (e: Exception) {
                SelectableItemsState.Error(e.toUserMessage())
            }
        }
    }

    fun toggleSelection(fridgeItemId: Int) {
        selectedFridgeItemIds = if (fridgeItemId in selectedFridgeItemIds) {
            selectedFridgeItemIds - fridgeItemId
        } else {
            selectedFridgeItemIds + fridgeItemId
        }
        recommendationState = RecommendationState.Idle
    }

    fun requestRecommendations() {
        if (selectedFridgeItemIds.isEmpty()) return
        viewModelScope.launch {
            recommendationState = RecommendationState.Loading
            recommendationState = try {
                val result = recommendationRepository.recommendRecipes(
                    selectedFridgeItemIds = selectedFridgeItemIds.toList(),
                )
                RecommendationState.Success(result.recommendations, result.emptyReason)
            } catch (e: Exception) {
                RecommendationState.Error(e.toUserMessage())
            }
        }
    }
}

private fun Exception.toUserMessage(): String = when (this) {
    is NetworkUnavailableException -> message ?: "네트워크 연결을 확인해주세요."
    is ApiException -> message
    else -> message ?: "알 수 없는 오류가 발생했어요."
}
