package com.example.cleanoutfridge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CleanOutFridgeColorScheme = lightColorScheme(
    primary = FoodColors.primary,
    onPrimary = FoodColors.primaryForeground,
    secondary = FoodColors.secondary,
    onSecondary = FoodColors.secondaryForeground,
    background = FoodColors.background,
    onBackground = FoodColors.foreground,
    surface = FoodColors.card,
    onSurface = FoodColors.cardForeground,
    error = FoodColors.destructive,
    onError = FoodColors.destructiveForeground,
    outline = FoodColors.border,
)

private val CleanOutFridgeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun CleanOutFridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CleanOutFridgeColorScheme,
        shapes = CleanOutFridgeShapes,
        typography = AppTypography,
        content = content,
    )
}
