package com.linkget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Card,
    primaryContainer = BlueSoft,
    onPrimaryContainer = BlueDeep,
    secondary = InkSoft,
    onSecondary = Card,
    secondaryContainer = BluePale,
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Color(0xFFEDF1F7),
    error = Danger,
    onError = Card,
    errorContainer = DangerSoft,
    onErrorContainer = Color(0xFF991B1B)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun LinkGetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
