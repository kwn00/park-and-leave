package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ParkLeaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) NightColors else DayColors
    val type = parkLeaveType()
    val scheme = if (darkTheme) {
        darkColorScheme(
            background = colors.background,
            onBackground = colors.onSurface,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.onSurfaceMuted,
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            error = colors.critical,
            onError = colors.onAccent,
            outline = colors.outline,
            outlineVariant = colors.outline,
            scrim = colors.background,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            onBackground = colors.onSurface,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.onSurfaceMuted,
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            error = colors.critical,
            onError = colors.onAccent,
            outline = colors.outline,
            outlineVariant = colors.outline,
            scrim = colors.background,
        )
    }
    CompositionLocalProvider(
        LocalParkLeaveColors provides colors,
        LocalParkLeaveType provides type,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = parkTypography(type),
            shapes = parkShapes(),
            content = content,
        )
    }
}
