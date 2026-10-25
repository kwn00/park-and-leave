package io.github.kwn00.parkandleave.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ParkLeaveColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val ok: Color,
    val onOk: Color,
    val caution: Color,
    val onCaution: Color,
    val warning: Color,
    val onWarning: Color,
    val accent: Color,
    val onAccent: Color,
)

val DayColors = ParkLeaveColors(
    background = Color(0xFFF4F6F8),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121417),
    onSurfaceMuted = Color(0xFF2C3138),
    ok = Color(0xFFE5F6EC),
    onOk = Color(0xFF0B3D22),
    caution = Color(0xFFFFF1D6),
    onCaution = Color(0xFF5C3200),
    warning = Color(0xFFFDECEC),
    onWarning = Color(0xFF7A1212),
    accent = Color(0xFF123A66),
    onAccent = Color(0xFFF5F7FA),
)

val NightColors = ParkLeaveColors(
    background = Color(0xFF0E1216),
    surface = Color(0xFF1A2129),
    onSurface = Color(0xFFF5F7FA),
    onSurfaceMuted = Color(0xFFD5DCE6),
    ok = Color(0xFF123524),
    onOk = Color(0xFFB6F0CF),
    caution = Color(0xFF3A2A12),
    onCaution = Color(0xFFFFD89A),
    warning = Color(0xFF3B1515),
    onWarning = Color(0xFFFFB4B4),
    accent = Color(0xFF8FB4E8),
    onAccent = Color(0xFF0E1216),
)

val LocalParkLeaveColors = staticCompositionLocalOf { DayColors }
