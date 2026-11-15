package com.scanku.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E8C9E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F1F6),
    onPrimaryContainer = Color(0xFF00363E),
    secondary = Color(0xFF4A6368),
    secondaryContainer = Color(0xFFCDE7EC),
    background = Color(0xFFF7FAFB),
    surface = Color(0xFFF7FAFB)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF55D0E3),
    onPrimary = Color(0xFF00363E),
    primaryContainer = Color(0xFF004F5A),
    onPrimaryContainer = Color(0xFFD3F1F6),
    secondary = Color(0xFFB1CBD0),
    secondaryContainer = Color(0xFF334B50)
)

@Composable
fun ScanKuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
