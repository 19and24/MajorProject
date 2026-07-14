package com.example.majorproject.food.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// The source design uses Nunito (headings/labels), DM Sans (body) and DM Mono (dates/tags/badges).
// No font files are bundled for this pass, so these approximate with system fonts; swap in real
// res/font ttf files + these FontFamily vals later without touching call sites.
object AppFonts {
    val nunito: FontFamily = FontFamily.Default
    val dmSans: FontFamily = FontFamily.Default
    val dmMono: FontFamily = FontFamily.Monospace
}

val FoodTrackerTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(fontFamily = AppFonts.dmSans, fontWeight = FontWeight.Normal),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = AppFonts.dmSans, fontWeight = FontWeight.Normal),
    titleLarge = Typography().titleLarge.copy(fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black),
    titleMedium = Typography().titleMedium.copy(fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold),
    labelLarge = Typography().labelLarge.copy(fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold),
)
