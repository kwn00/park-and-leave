package io.github.kwn00.parkandleave.domain

/**
 * AAOS `VehiclePropertyIds.GEAR_SELECTION` / `VehicleGear` integer codes.
 * The Android layer reads the platform constants and these values are checked
 * against `android.car` in a unit test.
 */
object GearCodes {
    const val PROPERTY_ID: Int = 289408000
    const val UNKNOWN: Int = 0
    const val NEUTRAL: Int = 1
    const val REVERSE: Int = 2
    const val PARK: Int = 4
    const val DRIVE: Int = 8
}

enum class Gear {
    PARK,
    REVERSE,
    NEUTRAL,
    DRIVE,
    OTHER,
    UNKNOWN,
}

data class GearReading(
    val propertyId: Int = GearCodes.PROPERTY_ID,
    val rawValue: Int?,
    val gear: Gear,
    val error: String?,
)

fun gearFromRaw(raw: Int?): Gear = when (raw) {
    null -> Gear.UNKNOWN
    GearCodes.PARK -> Gear.PARK
    GearCodes.REVERSE -> Gear.REVERSE
    GearCodes.NEUTRAL -> Gear.NEUTRAL
    GearCodes.DRIVE -> Gear.DRIVE
    GearCodes.UNKNOWN -> Gear.UNKNOWN
    else -> Gear.OTHER
}

fun Gear.rawCode(): Int = when (this) {
    Gear.PARK -> GearCodes.PARK
    Gear.REVERSE -> GearCodes.REVERSE
    Gear.NEUTRAL -> GearCodes.NEUTRAL
    Gear.DRIVE -> GearCodes.DRIVE
    Gear.UNKNOWN -> GearCodes.UNKNOWN
    Gear.OTHER -> -1
}
