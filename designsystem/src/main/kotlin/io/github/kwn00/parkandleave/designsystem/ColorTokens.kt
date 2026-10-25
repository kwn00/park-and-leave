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
    val critical: Color,
    val onCritical: Color,
    val warn: Color,
    val onWarn: Color,
    val info: Color,
    val onInfo: Color,
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
    critical = Color(0xFFFDECEC),
    onCritical = Color(0xFF7A1212),
    warn = Color(0xFFFFF1D6),
    onWarn = Color(0xFF5C3200),
    info = Color(0xFFE7F0FA),
    onInfo = Color(0xFF0A2F55),
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
    critical = Color(0xFF3B1515),
    onCritical = Color(0xFFFFB4B4),
    warn = Color(0xFF3A2A12),
    onWarn = Color(0xFFFFD89A),
    info = Color(0xFF16304A),
    onInfo = Color(0xFFD6E8FF),
    accent = Color(0xFF8FB4E8),
    onAccent = Color(0xFF0E1216),
)

val LocalParkLeaveColors = staticCompositionLocalOf { DayColors }
