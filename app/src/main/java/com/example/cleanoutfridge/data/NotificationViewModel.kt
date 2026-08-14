package com.example.cleanoutfridge.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.NetworkUnavailableException
import com.example.cleanoutfridge.data.remote.dto.NotificationDto
import com.example.cleanoutfridge.data.repository.NotificationRepository
import kotlinx.coroutines.launch

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(val notifications: List<NotificationDto>) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}

/** §6 알림 화면 상태. */
class NotificationViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepository(),
) : ViewModel() {

    var uiState: NotificationUiState by mutableStateOf(NotificationUiState.Loading)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = NotificationUiState.Loading
            uiState = try {
                val page = notificationRepository.getNotifications(size = 100)
                NotificationUiState.Success(page.items.sortedBy { it.notifyTime })
            } catch (e: Exception) {
                NotificationUiState.Error(e.toUserMessage())
            }
        }
    }

    fun delete(notificationId: Int) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                refresh()
            } catch (e: Exception) {
                uiState = NotificationUiState.Error(e.toUserMessage())
            }
        }
    }
}

private fun Exception.toUserMessage(): String = when (this) {
    is NetworkUnavailableException -> message ?: "네트워크 연결을 확인해주세요."
    is ApiException -> message
    else -> message ?: "알 수 없는 오류가 발생했어요."
}
