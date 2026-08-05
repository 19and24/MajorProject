package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import com.example.cleanoutfridge.data.remote.dto.DashboardSummaryDto
import retrofit2.Response
import retrofit2.http.GET

/** §10 대시보드 API. */
interface DashboardApi {

    /** §10.1 GET /dashboard */
    @GET("dashboard")
    suspend fun getDashboard(): Response<ApiResponse<DashboardSummaryDto>>
}
