package com.example.cleanoutfridge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object AppFonts {
    val nunito: FontFamily = FontFamily.Default
    val dmSans: FontFamily = FontFamily.Default
    val dmMono: FontFamily = FontFamily.Monospace
}

val AppTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(fontFamily = AppFonts.dmSans, fontWeight = FontWeight.Normal),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = AppFonts.dmSans, fontWeight = FontWeight.Normal),
    titleLarge = Typography().titleLarge.copy(fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black),
    titleMedium = Typography().titleMedium.copy(fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold),
    labelLarge = Typography().labelLarge.copy(fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold),
)
