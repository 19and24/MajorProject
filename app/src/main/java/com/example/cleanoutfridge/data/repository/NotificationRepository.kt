package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.api.NotificationApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.apiCallUnit
import com.example.cleanoutfridge.data.remote.dto.CreateNotificationRequest
import com.example.cleanoutfridge.data.remote.dto.NotificationDto
import com.example.cleanoutfridge.data.remote.dto.PageResponse
import com.example.cleanoutfridge.data.remote.dto.UpdateNotificationRequest

/** §6 알림 API wrapper. */
class NotificationRepository(
    private val api: NotificationApi = ApiClient.notificationApi,
) {
    suspend fun getNotifications(
        fridgeItemId: Int? = null,
        sent: Boolean? = null,
        from: String? = null,
        to: String? = null,
        page: Int? = null,
        size: Int? = null,
    ): PageResponse<NotificationDto> = apiCall {
        api.getNotifications(fridgeItemId = fridgeItemId, sent = sent, from = from, to = to, page = page, size = size)
    }

    suspend fun getNotification(notificationId: Int): NotificationDto = apiCall {
        api.getNotification(notificationId)
    }

    suspend fun createNotification(
        fridgeItemId: Int,
        title: String,
        content: String? = null,
        notifyTime: String,
    ): NotificationDto = apiCall {
        api.createNotification(CreateNotificationRequest(fridgeItemId, title, content, notifyTime))
    }

    suspend fun updateNotification(
        notificationId: Int,
        title: String? = null,
        content: String? = null,
        notifyTime: String? = null,
    ): NotificationDto = apiCall {
        api.updateNotification(notificationId, UpdateNotificationRequest(title, content, notifyTime))
    }

    suspend fun deleteNotification(notificationId: Int): Unit = apiCallUnit {
        api.deleteNotification(notificationId)
    }
}
