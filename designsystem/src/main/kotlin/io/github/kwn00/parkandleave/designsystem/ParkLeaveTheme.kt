package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ParkLeaveTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) NightColors else DayColors
    val scheme = if (dark) {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.onSurface,
            onSurface = colors.onSurface,
            primary = colors.accent,
            onPrimary = colors.onAccent,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.onSurface,
            onSurface = colors.onSurface,
            primary = colors.accent,
            onPrimary = colors.onAccent,
        )
    }
    CompositionLocalProvider(LocalParkLeaveColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = parkTypography(),
            content = content,
        )
    }
}
