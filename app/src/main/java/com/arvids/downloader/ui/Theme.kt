package com.arvids.downloader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArVidsColors = darkColorScheme(
    primary = Color(0xFF7C3AED),
    secondary = Color(0xFF2563EB),
    background = Color(0xFF0F0B1E),
    surface = Color(0xFF1B1430),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ArVidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ArVidsColors, content = content)
}
