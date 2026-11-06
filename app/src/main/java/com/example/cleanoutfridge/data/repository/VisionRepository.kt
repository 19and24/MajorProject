package com.example.cleanoutfridge.data.repository

import com.example.cleanoutfridge.data.remote.ApiClient
import com.example.cleanoutfridge.data.remote.api.VisionApi
import com.example.cleanoutfridge.data.remote.apiCall
import com.example.cleanoutfridge.data.remote.dto.VisionScanItemDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** AI photo scan API wrapper: uploads one photo, gets back every food item it recognized. */
class VisionRepository(
    private val api: VisionApi = ApiClient.visionApi,
) {
    suspend fun scanPhoto(imageBytes: ByteArray, mimeType: String = "image/jpeg"): List<VisionScanItemDto> {
        val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("image", "photo.jpg", requestBody)
        val result = apiCall { api.scanPhoto(part) }
        return result.items
    }
}
