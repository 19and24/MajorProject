package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.api.DashboardApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.dto.DashboardSummaryDto

/** §10 대시보드 API wrapper. */
class DashboardRepository(
    private val api: DashboardApi = ApiClient.dashboardApi,
) {
    suspend fun getDashboard(): DashboardSummaryDto = apiCall {
        api.getDashboard()
    }
}
