package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Envelope used by every endpoint in the API spec:
 * { "success": true, "data": {...}, "message": null }
 * or on failure: { "success": false, "data": null, "message": "...", "errorCode": "..." }
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null,
)

/**
 * Generic page envelope used inside `data` for list endpoints:
 * { "items": [...], "page": 1, "size": 20, "totalElements": 120, "totalPages": 6 }
 */
@Serializable
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val size: Int = 20,
    val totalElements: Int = 0,
    val totalPages: Int = 0,
)

/** Raw shape used only to pull `errorCode`/`message` out of a non-2xx error body. */
@Serializable
data class ApiErrorBody(
    val success: Boolean = false,
    val data: JsonElement? = null,
    val message: String? = null,
    @SerialName("errorCode") val errorCode: String? = null,
)
