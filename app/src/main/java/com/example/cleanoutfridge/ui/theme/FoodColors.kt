package com.example.cleanoutfridge.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Fresh produce market" palette — a crate-label system, not a fintech-app system.
 * Everything reads as stamped/printed onto kraft board: deep basil green, burnt-clay urgency,
 * brick-red spoilage, warm paper ground.
 */
object FoodColors {
    val background = Color(0xFFFBF7EE)
    val foreground = Color(0xFF20241A)
    val card = Color(0xFFFFFFFF)
    val cardForeground = Color(0xFF20241A)

    val primary = Color(0xFF2F6B3D)
    val primaryForeground = Color(0xFFFFFFFF)
    val primaryDark = Color(0xFF1F4B2A)
    val primarySoft = Color(0xFFE4EEE0)
    val primarySoftBorder = Color(0xFFBFD7B8)

    val secondary = Color(0xFFF3ECD9)
    val secondaryForeground = Color(0xFF20241A)

    val muted = Color(0xFFEFE8D8)
    val mutedForeground = Color(0xFF756F5E)

    // Urgency / decay scale — the app's signature "ripeness" gradient.
    val fresh = Color(0xFF2F6B3D)
    val freshSoft = Color(0xFFE4EEE0)
    val freshSoftBorder = Color(0xFFBFD7B8)

    val warn = Color(0xFFB8860F)
    val warnSoft = Color(0xFFF6EDD4)
    val warnSoftBorder = Color(0xFFE3CD8E)

    val urgent = Color(0xFFC4501E)
    val urgentSoft = Color(0xFFFBEADD)
    val urgentSoftBorder = Color(0xFFEAC19E)

    val expired = Color(0xFFA02B2B)
    val expiredSoft = Color(0xFFFAE6E2)
    val expiredSoftBorder = Color(0xFFE3B3A8)

    val accent = Color(0xFFC4501E)
    val accentForeground = Color(0xFFFFFFFF)
    val destructive = Color(0xFFA02B2B)
    val destructiveForeground = Color(0xFFFFFFFF)

    val border = Color(0x1F20241A)
    val borderStrong = Color(0x3D20241A)
    val kraft = Color(0xFFE8DCC0)

    val pieColors = listOf(
        Color(0xFF2F6B3D), Color(0xFFC4501E), Color(0xFFB8860F), Color(0xFF3E6E8E), Color(0xFF7A5C9E),
        Color(0xFFA0456B), Color(0xFF3E8E80), Color(0xFFA02B2B), Color(0xFF6B8E23), Color(0xFF756F5E),
    )
}
