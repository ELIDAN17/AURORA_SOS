package com.example.aurora_sos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PetroleumGreen80,
    onPrimary = PetroleumGreen15,
    secondary = PetroleumGreen60,
    onSecondary = PetroleumGreen90,
    background = Gray10,
    onBackground = Gray90,
    surface = Gray5,
    onSurface = Gray90
)

private val LightColorScheme = lightColorScheme(
    primary = PetroleumGreen40,
    onPrimary = PetroleumGreen100,
    secondary = PetroleumGreen60,
    onSecondary = PetroleumGreen100,
    background = PetroleumGreen90,
    onBackground = Gray50,
    surfaceContainerHighest = Gray80,
    surfaceContainerLow = Gray90
)

@Composable
fun AURORA_SOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
