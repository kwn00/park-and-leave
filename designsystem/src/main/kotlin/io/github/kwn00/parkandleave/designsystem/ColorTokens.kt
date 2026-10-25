package io.github.kwn00.parkandleave.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One neutral ramp, one accent, and three semantic hues.
 *
 * Every semantic hue has a `*Tint` partner for small containers (icon tiles,
 * badges). Tints never fill a whole card. Foreground roles keep at least
 * 4.5:1 against [surface], [background], and their own tint in both themes.
 */
@Immutable
data class ParkLeaveColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val hairline: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceSubtle: Color,
    val accent: Color,
    val accentTint: Color,
    val onAccent: Color,
    val critical: Color,
    val criticalTint: Color,
    val criticalEdge: Color,
    val warn: Color,
    val warnTint: Color,
    val ok: Color,
    val okTint: Color,
)

val DayColors = ParkLeaveColors(
    background = Color(0xFFEDEFF2),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3F4F6),
    hairline = Color(0xFFDCE0E5),
    onSurface = Color(0xFF0E1014),
    onSurfaceMuted = Color(0xFF3F4650),
    onSurfaceSubtle = Color(0xFF5F6672),
    accent = Color(0xFF1F55D1),
    accentTint = Color(0xFFE8EEFC),
    onAccent = Color(0xFFFFFFFF),
    critical = Color(0xFFC4302B),
    criticalTint = Color(0xFFFBEAE8),
    criticalEdge = Color(0xFFE8A6A0),
    warn = Color(0xFF9A5B00),
    warnTint = Color(0xFFFDF1DD),
    ok = Color(0xFF177A48),
    okTint = Color(0xFFE3F3EA),
)

val NightColors = ParkLeaveColors(
    background = Color(0xFF08090B),
    surface = Color(0xFF131519),
    surfaceMuted = Color(0xFF1B1E23),
    hairline = Color(0xFF262A31),
    onSurface = Color(0xFFE8EAED),
    onSurfaceMuted = Color(0xFFB4BAC3),
    onSurfaceSubtle = Color(0xFF8E95A0),
    accent = Color(0xFF8CB2FF),
    accentTint = Color(0xFF18233A),
    onAccent = Color(0xFF08090B),
    critical = Color(0xFFFF8A80),
    criticalTint = Color(0xFF33191A),
    criticalEdge = Color(0xFF7E3631),
    warn = Color(0xFFF2B65A),
    warnTint = Color(0xFF2E2414),
    ok = Color(0xFF5CCB8E),
    okTint = Color(0xFF13291E),
)

val LocalParkLeaveColors = staticCompositionLocalOf { DayColors }
