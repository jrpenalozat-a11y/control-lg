package com.controllg.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PowerRed = Color(0xFFE5484D)

private val Colores = darkColorScheme(
    primary = Color(0xFFFF5C8A),
    onPrimary = Color(0xFF1B0A10),
    primaryContainer = Color(0xFF4A1F2D),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFF9AA0AA),
    onSecondary = Color(0xFF101114),
    background = Color(0xFF101114),
    onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF1B1D22),
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF2A2D34),
    onSurfaceVariant = Color(0xFFB4B9C2),
    outline = Color(0xFF4A4E57),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1B0A0A),
)

@Composable
fun ControlLgTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, content = content)
}
