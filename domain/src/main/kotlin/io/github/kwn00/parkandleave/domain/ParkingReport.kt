package io.github.kwn00.parkandleave.domain

enum class SignalId {
    REAR_OCCUPANCY,
    TRUNK,
    DOOR_OPEN,
    TIRE,
    TELLTALE,
    WINDOW,
    DOOR_UNLOCKED,
    CABIN_LIGHT,
    READING_LIGHT,
    EXTERIOR_LIGHT,
    HVAC,
    WASHER,
    LOW_SOC,
    LOW_FUEL,
}

enum class Severity {
    WARNING,
    CAUTION,
}

data class CheckItem(
    val id: SignalId,
    val severity: Severity,
    val title: String,
    val detail: String,
    val guidance: String,
)

data class ParkingReport(
    val items: List<CheckItem>,
    val headline: String,
    val allClear: Boolean,
    val tomorrowLine: String?,
    val clearanceLine: String?,
    val speechLine: String?,
    /** Calm lines such as "경고등: 이 차량에서 앱이 읽을 수 없음". */
    val uncheckedLines: List<String> = emptyList(),
)

enum class SessionPhase {
    IDLE,
    ACTIVE,
    FROZEN,
}

data class CheckSession(
    val phase: SessionPhase,
    val motionAllowed: Boolean,
)

/**
 * Parked-only session. A manual check is accepted only when gear cannot be read,
 * so a known Drive gear never opens the check screen.
 */
fun checkSession(gear: Gear, restricted: Boolean, manual: Boolean): CheckSession {
    val phase = when {
        restricted -> SessionPhase.FROZEN
        gear == Gear.PARK || (manual && gear == Gear.UNKNOWN) -> SessionPhase.ACTIVE
        else -> SessionPhase.IDLE
    }
    return CheckSession(
        phase = phase,
        motionAllowed = phase == SessionPhase.ACTIVE && gear == Gear.PARK && !restricted,
    )
}
