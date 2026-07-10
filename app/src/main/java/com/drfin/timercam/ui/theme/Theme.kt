package com.drfin.timercam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TimerCamColorScheme = darkColorScheme(
    primary = AccentAmber,
    background = SurfaceBlack,
    surface = SurfaceBlack,
)

@Composable
fun TimerCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TimerCamColorScheme,
        content = content,
    )
}
