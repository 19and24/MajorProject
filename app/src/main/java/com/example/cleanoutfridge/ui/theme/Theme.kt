package com.example.cleanoutfridge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CleanOutFridgeColorScheme = lightColorScheme(
    primary = FoodColors.primary,
    onPrimary = FoodColors.primaryForeground,
    secondary = FoodColors.secondary,
    onSecondary = FoodColors.secondaryForeground,
    tertiary = FoodColors.urgent,
    onTertiary = Color.White,
    background = FoodColors.background,
    onBackground = FoodColors.foreground,
    surface = FoodColors.card,
    onSurface = FoodColors.cardForeground,
    surfaceVariant = FoodColors.muted,
    onSurfaceVariant = FoodColors.mutedForeground,
    error = FoodColors.destructive,
    onError = FoodColors.destructiveForeground,
    outline = FoodColors.border,
)

private val CleanOutFridgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
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
