package com.example.cleanoutfridge.data.remote

/** §12 error codes. UNKNOWN covers anything the server adds later or a malformed error body. */
enum class ApiErrorCode {
    INGREDIENT_NOT_FOUND,
    FRIDGE_ITEM_NOT_FOUND,
    RECIPE_NOT_FOUND,
    NOTIFICATION_NOT_FOUND,
    INVALID_QUANTITY,
    INSUFFICIENT_QUANTITY,
    INVALID_DEADLINE,
    INVALID_NOTIFY_TIME,
    DEADLINE_REQUIRED,
    DUPLICATE_NOTIFICATION,
    RECOMMENDATION_INGREDIENT_REQUIRED,
    EXPIRED_FRIDGE_ITEM_SELECTED,
    RECIPE_RECOMMENDATION_FAILED,
    AI_RECOMMENDATION_FAILED,
    UNKNOWN;

    companion object {
        fun from(raw: String?): ApiErrorCode =
            raw?.let { code -> entries.find { it.name == code } } ?: UNKNOWN
    }
}

/**
 * Thrown for any non-2xx response or a 2xx response with `success: false`.
 * [rawData] retains the parsed JSON payload (e.g. `invalidFridgeItemIds`) for callers
 * that need more than the message/errorCode, without forcing every DTO to carry it.
 */
class ApiException(
    val httpStatus: Int,
    val errorCode: ApiErrorCode,
    override val message: String,
    val rawData: kotlinx.serialization.json.JsonElement? = null,
) : Exception(message)

/** Thrown when the request never reached the server (no connectivity, timeout, DNS, etc). */
class NetworkUnavailableException(cause: Throwable) : Exception("네트워크 연결을 확인해주세요.", cause)
