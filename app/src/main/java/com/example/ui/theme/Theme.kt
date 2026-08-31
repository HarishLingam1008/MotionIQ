package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SoftLightColorScheme = lightColorScheme(
    primary = SoftPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F8FF),
    onPrimaryContainer = Color(0xFF00688B),
    secondary = SoftSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEBFF),
    onSecondaryContainer = Color(0xFF432CB4),
    tertiary = SoftAccent,
    onTertiary = Color.White,
    background = SoftBackground,
    onBackground = SoftOnBackground,
    surface = SoftSurface,
    onSurface = SoftOnSurface,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = SoftOnSurfaceVariant,
    outline = SoftBorder,
    outlineVariant = SoftBorder
)

@Composable
fun MotionIQTheme(
    darkTheme: Boolean = false, // Enforce Soft White Theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoftLightColorScheme,
        typography = Typography,
        content = content
    )
}




