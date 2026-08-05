package com.example.cleanoutfridge.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.NetworkUnavailableException
import com.example.cleanoutfridge.data.remote.dto.CreateFridgeItemRequest
import com.example.cleanoutfridge.data.repository.FridgeItemRepository
import com.example.cleanoutfridge.data.repository.IngredientRepository
import com.example.cleanoutfridge.model.FoodItem
import kotlinx.coroutines.launch

sealed interface FoodUiState {
    data object Loading : FoodUiState
    data class Success(val items: List<FoodItem>) : FoodUiState
    data class Error(val message: String) : FoodUiState
}

class FoodViewModel(
    private val fridgeItemRepository: FridgeItemRepository = FridgeItemRepository(),
    private val ingredientRepository: IngredientRepository = IngredientRepository(),
) : ViewModel() {

    var uiState: FoodUiState by mutableStateOf(FoodUiState.Loading)
        private set

    /** Convenience accessor for screens that only need the list (empty while loading/error). */
    val items: List<FoodItem>
        get() = (uiState as? FoodUiState.Success)?.items ?: emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = FoodUiState.Loading
            uiState = try {
                val page = fridgeItemRepository.getFridgeItems(size = 200)
                FoodUiState.Success(page.items.map { it.toFoodItem() })
            } catch (e: Exception) {
                FoodUiState.Error(e.toUserMessage())
            }
        }
    }

    /** Resolves [FoodItem.name] to a standard ingredient by keyword search, then creates the fridge item. */
    fun addItem(item: FoodItem) {
        viewModelScope.launch {
            try {
                val matches = ingredientRepository.getIngredients(keyword = item.name, size = 1)
                val ingredient = matches.items.firstOrNull()
                    ?: throw IllegalStateException("'${item.name}'과(와) 일치하는 표준 식재료를 찾을 수 없어요.")

                fridgeItemRepository.createFridgeItem(
                    CreateFridgeItemRequest(
                        ingredientId = ingredient.id,
                        quantity = item.quantity,
                        unit = item.unit,
                        deadline = item.expiryDate,
                    ),
                )
                refresh()
            } catch (e: Exception) {
                uiState = FoodUiState.Error(e.toUserMessage())
            }
        }
    }

    fun deleteItem(id: String) {
        val fridgeItemId = id.toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                fridgeItemRepository.deleteFridgeItem(fridgeItemId)
                refresh()
            } catch (e: Exception) {
                uiState = FoodUiState.Error(e.toUserMessage())
            }
        }
    }

    fun setStarred(id: String, starred: Boolean) {
        val fridgeItemId = id.toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                fridgeItemRepository.setStarred(fridgeItemId, starred)
                refresh()
            } catch (e: Exception) {
                uiState = FoodUiState.Error(e.toUserMessage())
            }
        }
    }
}

private fun Exception.toUserMessage(): String = when (this) {
    is NetworkUnavailableException -> message ?: "네트워크 연결을 확인해주세요."
    is ApiException -> message
    else -> message ?: "알 수 없는 오류가 발생했어요."
}
