package com.example.lostfoundai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary             = PrimaryIndigo,
    onPrimary           = Color.White,
    primaryContainer    = PrimaryContainer,
    onPrimaryContainer  = OnPrimaryContainer,
    secondary           = SecondaryTeal,
    onSecondary         = Color.White,
    secondaryContainer  = SecondaryContainer,
    onSecondaryContainer= OnSecondaryContainer,
    background          = BackgroundColor,
    onBackground        = OnSurfaceColor,
    surface             = SurfaceColor,
    onSurface           = OnSurfaceColor,
    surfaceVariant      = SurfaceVariantColor,
    onSurfaceVariant    = OnSurfaceVariantColor,
    outline             = OutlineColor,
    error               = ErrorRed,
    onError             = Color.White,
)

@Composable
fun LostFoundAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}