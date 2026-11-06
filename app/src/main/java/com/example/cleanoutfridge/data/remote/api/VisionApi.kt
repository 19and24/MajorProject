package com.example.cleanoutfridge.data.remote.api

import com.example.cleanoutfridge.data.remote.dto.ApiResponse
import com.example.cleanoutfridge.data.remote.dto.VisionScanResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** AI photo scan API: recognizes multiple food items (name/category/expiry) from one photo. */
interface VisionApi {

    @Multipart
    @POST("vision/scan")
    suspend fun scanPhoto(
        @Part image: MultipartBody.Part,
    ): Response<ApiResponse<VisionScanResponseDto>>
}
