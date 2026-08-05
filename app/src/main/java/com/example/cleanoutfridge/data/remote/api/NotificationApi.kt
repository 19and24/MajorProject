package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import com.example.cleanoutfridge.data.remote.dto.CreateNotificationRequest
import com.example.cleanoutfridge.data.remote.dto.NotificationDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse
import com.example.cleanoutfridge.data.remote.dto.UpdateNotificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** §6 알림 API. */
interface NotificationApi {

    /** §6.1 GET /notifications */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("fridgeItemId") fridgeItemId: Int? = null,
        @Query("sent") sent: Boolean? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): Response<ApiResponse<PageResponse<NotificationDto>>>

    /** §6.2 GET /notifications/{notificationId} */
    @GET("notifications/{notificationId}")
    suspend fun getNotification(
        @Path("notificationId") notificationId: Int,
    ): Response<ApiResponse<NotificationDto>>

    /** §6.3 POST /notifications */
    @POST("notifications")
    suspend fun createNotification(
        @Body request: CreateNotificationRequest,
    ): Response<ApiResponse<NotificationDto>>

    /** §6.4 PATCH /notifications/{notificationId} */
    @PATCH("notifications/{notificationId}")
    suspend fun updateNotification(
        @Path("notificationId") notificationId: Int,
        @Body request: UpdateNotificationRequest,
    ): Response<ApiResponse<NotificationDto>>

    /** §6.5 DELETE /notifications/{notificationId} */
    @DELETE("notifications/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: Int,
    ): Response<Unit>
}
