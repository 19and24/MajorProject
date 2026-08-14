package com.example.cleanoutfridge.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanoutfridge.data.remote.dto.DashboardSummaryDto
import com.example.cleanoutfridge.data.repository.DashboardRepository
import kotlinx.coroutines.launch

sealed interface DashboardState {
    data object Loading : DashboardState
    data class Success(val summary: DashboardSummaryDto) : DashboardState
    data class Error(val message: String) : DashboardState
}

/** §10 대시보드 요약 (홈 화면 추천 카드 / 알림 배지에 사용). */
class DashboardViewModel(
    private val dashboardRepository: DashboardRepository = DashboardRepository(),
) : ViewModel() {

    var state: DashboardState by mutableStateOf(DashboardState.Loading)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state = try {
                DashboardState.Success(dashboardRepository.getDashboard())
            } catch (e: Exception) {
                DashboardState.Error(e.message ?: "대시보드를 불러오지 못했어요.")
            }
        }
    }
}
