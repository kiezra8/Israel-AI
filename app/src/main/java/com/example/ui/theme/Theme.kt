package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = Color.Black,
    primaryContainer = ArcSurfaceCard,
    onPrimaryContainer = ArcCyanGlow,
    secondary = ArcGold,
    onSecondary = Color.Black,
    tertiary = ArcCyanGlow,
    background = ArcDeepBackground,
    onBackground = ArcTextPrimary,
    surface = ArcSurfaceCard,
    onSurface = ArcTextPrimary,
    surfaceVariant = ArcSurfaceCardBorder,
    onSurfaceVariant = ArcTextSecondary,
    error = ArcRedAlert,
    onError = Color.White
)

@Composable
fun IsraelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}

