package com.example.cleanoutfridge.data.remote

import com.example.cleanoutfridge.data.remote.dto.ApiErrorBody
import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.Response

/**
 * Runs a suspending Retrofit call and unwraps the [ApiResponse] envelope (§3):
 * - HTTP 2xx with `success: true` -> returns `data` (or Unit for 204 bodies).
 * - HTTP 2xx with `success: false`, or any non-2xx -> throws [ApiException] carrying the
 *   spec's `errorCode`/`message` (§12).
 * - Connectivity/timeout failures -> throws [NetworkUnavailableException].
 *
 * Repository functions should be one-liners around this: `apiCall { api.getFoo() }`.
 */
suspend fun <T> apiCall(block: suspend () -> Response<ApiResponse<T>>): T {
    val response = safeExecute(block)

    if (response.isSuccessful) {
        val body = response.body()
        if (body != null && body.success) {
            @Suppress("UNCHECKED_CAST")
            return (body.data ?: Unit) as T
        }
        // 2xx but success:false (spec doesn't define this combination, but guard anyway).
        throw ApiException(
            httpStatus = response.code(),
            errorCode = ApiErrorCode.from(body?.errorCode),
            message = body?.message ?: "요청이 실패했습니다.",
        )
    }

    throw parseErrorBody(response)
}

/** Variant for endpoints that return 204 No Content with no envelope body at all (§5.5, §6.5). */
suspend fun apiCallUnit(block: suspend () -> Response<Unit>): Unit {
    val response = safeExecute(block)
    if (response.isSuccessful) return
    throw parseErrorBody(response)
}

private suspend fun <B> safeExecute(block: suspend () -> Response<B>): Response<B> =
    try {
        block()
    } catch (e: IOException) {
        throw NetworkUnavailableException(e)
    }

private fun parseErrorBody(response: Response<*>): ApiException {
    val raw = response.errorBody()?.string()
    val parsed = raw?.let {
        try {
            ApiClient.json.decodeFromString(ApiErrorBody.serializer(), it)
        } catch (e: SerializationException) {
            null
        }
    }
    return ApiException(
        httpStatus = response.code(),
        errorCode = ApiErrorCode.from(parsed?.errorCode),
        message = parsed?.message ?: "요청 처리 중 오류가 발생했습니다. (HTTP ${response.code()})",
        rawData = parsed?.data,
    )
}
