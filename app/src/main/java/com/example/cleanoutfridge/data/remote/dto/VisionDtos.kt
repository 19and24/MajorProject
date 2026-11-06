package com.example.cleanoutfridge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /vision/scan response item: one detected food item from a photo. */
@Serializable
data class VisionScanItemDto(
    val name: String,
    val category: String,
    val emoji: String,
    val confidence: Int,
    @SerialName("expiry_days") val expiryDays: Int,
)

@Serializable
data class VisionScanResponseDto(
    val items: List<VisionScanItemDto> = emptyList(),
)
