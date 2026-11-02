package com.reveilpronote.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1B3A6B),
    secondary = Color(0xFF2E7D6B),
    tertiary = Color(0xFFB4532A),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA9C4F2),
    secondary = Color(0xFF8ED1BF),
    tertiary = Color(0xFFF2A884),
)

@Composable
fun ReveilTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
