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
    background = Gray10, // Fondo casi negro
    onBackground = Gray90, // Texto gris claro
    surface = Gray5, // Superficie un poco más clara que el fondo
    onSurface = Gray90 // Texto gris claro en superficies
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
    dynamicColor: Boolean = false, // Se mantiene deshabilitado
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
