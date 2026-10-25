package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun ParkLeaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) NightColors else DayColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.onSurface,
            onSurface = colors.onSurface,
            primary = colors.accent,
            onPrimary = colors.onAccent,
            error = colors.critical,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.onSurface,
            onSurface = colors.onSurface,
            primary = colors.accent,
            onPrimary = colors.onAccent,
            error = colors.critical,
        )
    }
    CompositionLocalProvider(
        LocalParkLeaveColors provides colors,
        LocalParkLeaveType provides DefaultParkLeaveType,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = parkTypography(),
            content = content,
        )
    }
}

object ParkLeaveTheme {
    val colors: ParkLeaveColors
        @Composable
        @ReadOnlyComposable
        get() = LocalParkLeaveColors.current

    val type: ParkLeaveType
        @Composable
        @ReadOnlyComposable
        get() = LocalParkLeaveType.current
}
