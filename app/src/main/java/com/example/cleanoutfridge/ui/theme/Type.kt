package com.example.cleanoutfridge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.R

/**
 * Fraunces (display, stamped-label headlines), Inter (body/UI), JetBrains Mono (dates, counters,
 * anything that should read like a produce stamp). All three ship as variable fonts; weights are
 * pulled via [FontVariation] instances so a single asset covers the whole range.
 */
@OptIn(ExperimentalTextApi::class)
object AppFonts {
    private fun frauncesWeight(weight: Int, wonk: Float = 1f) = Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight(weight),
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.opticalSizing(72.sp),
            FontVariation.Setting("WONK", wonk),
            FontVariation.Setting("SOFT", 0f),
        ),
    )

    val fraunces: FontFamily = FontFamily(
        frauncesWeight(500),
        frauncesWeight(600),
        frauncesWeight(700),
        frauncesWeight(900),
        Font(
            resId = R.font.fraunces_variable,
            weight = FontWeight(900),
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(900),
                FontVariation.opticalSizing(72.sp),
                FontVariation.italic(1f),
                FontVariation.Setting("WONK", 1f),
            ),
        ),
    )

    val inter: FontFamily = FontFamily(
        Font(R.font.inter_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.inter_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.inter_variable, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.inter_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )

    val mono: FontFamily = FontFamily(
        Font(R.font.jetbrainsmono_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.jetbrainsmono_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.jetbrainsmono_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
        Font(R.font.jetbrainsmono_variable, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    )

    // Legacy aliases kept during the visual-refresh migration.
    val nunito: FontFamily get() = fraunces
    val dmSans: FontFamily get() = inter
    val dmMono: FontFamily get() = mono
}

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(900), fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.3).sp),
    displayMedium = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(900), fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp),
    headlineLarge = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = AppFonts.fraunces, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = AppFonts.inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = AppFonts.inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = AppFonts.inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = AppFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = AppFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = AppFonts.mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)
