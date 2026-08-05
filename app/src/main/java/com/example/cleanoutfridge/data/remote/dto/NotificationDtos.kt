package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.Serializable

/** Minimal fridge item reference embedded in a notification list row (§6.1). */
@Serializable
data class NotificationFridgeItemRefDto(
    val id: Int,
    val ingredientName: String? = null,
    val ingredient: IngredientDto? = null,
    val deadline: String? = null,
)

/** §6.1/6.2 Notification representation. */
@Serializable
data class NotificationDto(
    val id: Int,
    val fridgeItem: NotificationFridgeItemRefDto? = null,
    val fridgeItemId: Int? = null,
    val title: String? = null,
    val content: String? = null,
    val notifyTime: String,
    val sent: Boolean = false,
)

/** §6.3 POST /notifications request body. */
@Serializable
data class CreateNotificationRequest(
    val fridgeItemId: Int,
    val title: String,
    val content: String? = null,
    val notifyTime: String,
)

/** §6.4 PATCH /notifications/{id} request body — only sent fields are updated. */
@Serializable
data class UpdateNotificationRequest(
    val title: String? = null,
    val content: String? = null,
    val notifyTime: String? = null,
)

/** §6.6 POST /fridge-items/{id}/notifications/default request body. */
@Serializable
data class CreateDefaultNotificationsRequest(
    val daysBefore: List<Int>,
    val notifyHour: Int,
)

/** §6.6 single generated notification entry. */
@Serializable
data class DefaultNotificationEntryDto(
    val id: Int,
    val notifyTime: String,
    val daysBefore: Int,
)

/** §6.6 response payload. */
@Serializable
data class CreateDefaultNotificationsResponse(
    val createdNotifications: List<DefaultNotificationEntryDto> = emptyList(),
)
