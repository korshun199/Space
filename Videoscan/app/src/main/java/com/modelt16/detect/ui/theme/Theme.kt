// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Тема оформления Compose

package com.modelt16.detect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HudCyan,
    secondary = HudGreen,
    tertiary = HudLockAmber,
    background = Color(0xFF030712),
    surface = HudPanelBg,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ModelT16DetectTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
