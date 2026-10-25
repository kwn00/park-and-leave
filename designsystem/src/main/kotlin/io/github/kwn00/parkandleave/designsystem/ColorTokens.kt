package io.github.kwn00.parkandleave.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One neutral surface scale, one accent, and semantic warning/critical colors.
 * Positive status reuses [accent] so a success green does not join the palette.
 */
@Immutable
data class ParkLeaveColors(
    val background: Color,
    val surface: Color,
    val outline: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val critical: Color,
    val warn: Color,
    val buttonContainer: Color,
    val buttonContent: Color,
    val buttonBorder: Color,
)

val DayColors = ParkLeaveColors(
    background = Color(0xFFE6E8EC),
    surface = Color(0xFFFFFFFF),
    outline = Color(0xFFC5CCD4),
    onSurface = Color(0xFF1A1C1F),
    onSurfaceMuted = Color(0xFF515864),
    accent = Color(0xFF1E3A56),
    onAccent = Color(0xFFF4F6F8),
    critical = Color(0xFF9A1B22),
    warn = Color(0xFF8C5A00),
    buttonContainer = Color(0xFF1E3A56),
    buttonContent = Color(0xFFF4F6F8),
    buttonBorder = Color(0xFF1E3A56),
)

val NightColors = ParkLeaveColors(
    background = Color(0xFF0B0C0E),
    surface = Color(0xFF1C2026),
    outline = Color(0xFF5C6672),
    onSurface = Color(0xFFE7E9EC),
    onSurfaceMuted = Color(0xFFB5BBC4),
    accent = Color(0xFFD3DCE6),
    onAccent = Color(0xFF121417),
    critical = Color(0xFFF2B7B1),
    warn = Color(0xFFE8C48A),
    buttonContainer = Color(0xFF262B33),
    buttonContent = Color(0xFFE7E9EC),
    buttonBorder = Color(0xFF5C6672),
)

val LocalParkLeaveColors = staticCompositionLocalOf { DayColors }
