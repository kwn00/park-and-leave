package io.github.kwn00.parkandleave.domain

enum class VehicleArea {
    ROW_1_LEFT,
    ROW_1_CENTER,
    ROW_1_RIGHT,
    ROW_2_LEFT,
    ROW_2_CENTER,
    ROW_2_RIGHT,
    ROW_3_LEFT,
    ROW_3_CENTER,
    ROW_3_RIGHT,
    HOOD,
    TRUNK,
    FRONT_WINDSHIELD,
    REAR_WINDSHIELD,
    ROOF,
    WHEEL_LEFT_FRONT,
    WHEEL_RIGHT_FRONT,
    WHEEL_LEFT_REAR,
    WHEEL_RIGHT_REAR,
}

enum class TelltaleKind {
    TRUNK_OPEN,
    DOOR_OPEN,
    TPMS,
    LOW_FUEL,
    LOW_EV_BATTERY,
    ENGINE,
    MASTER_WARNING,
    ABS,
    AIRBAG,
    LOW_VOLT_BATTERY,
    ENGINE_OIL,
    ENGINE_TEMP,
    POWER_DOWN,
    EV_SYSTEM,
    OTHER,
}

data class WindowReading(
    val area: VehicleArea,
    val position: Int,
    val maxPosition: Int,
    val supported: Boolean,
)

data class DoorReading(
    val area: VehicleArea,
    val position: Int?,
    val locked: Boolean?,
)

data class ReadingLightReading(
    val area: VehicleArea,
    val on: Boolean?,
)

data class TireReading(
    val area: VehicleArea,
    /**
     * Raw `getLowTirePressure` float.
     * 0 means not low. (0, 1] is treated as a warning flag.
     * Larger values are kept for the debug screen and are not flagged,
     * because the SDK does not document whether the float is kPa.
     */
    val lowPressure: Float?,
)

data class TelltaleReading(
    val kind: TelltaleKind,
    val active: Boolean,
    val rawName: String,
)

data class DebugRow(
    val label: String,
    val value: String,
)

data class VehicleSnapshot(
    val ready: Boolean = true,
    val windows: List<WindowReading> = emptyList(),
    val doors: List<DoorReading> = emptyList(),
    val cabinLightOn: Boolean? = null,
    val readingLights: List<ReadingLightReading> = emptyList(),
    val lowBeamOn: Boolean? = null,
    val tailLampOn: Boolean? = null,
    val hvacOn: Boolean? = null,
    val hvacZones: List<ReadingLightReading> = emptyList(),
    val tires: List<TireReading> = emptyList(),
    val washerLow: Boolean? = null,
    val telltales: List<TelltaleReading> = emptyList(),
    val socPercent: Int? = null,
    val fuelPercent: Int? = null,
    val tripDistance: Float? = null,
    val readErrors: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val link: SdkLink = SdkLink.CONNECTED,
    /**
     * Sources that were attempted and could not be confirmed.
     * A null field with no entry here means that signal was not part of this reading.
     * [UnreadCause.NULL_VALUE] means the vehicle returned no value, which is not normal.
     */
    val unread: List<UnreadSignal> = emptyList(),
)

fun VehicleArea.label(): String = when (this) {
    VehicleArea.ROW_1_LEFT -> "1열 왼쪽"
    VehicleArea.ROW_1_CENTER -> "1열 가운데"
    VehicleArea.ROW_1_RIGHT -> "1열 오른쪽"
    VehicleArea.ROW_2_LEFT -> "2열 왼쪽"
    VehicleArea.ROW_2_CENTER -> "2열 가운데"
    VehicleArea.ROW_2_RIGHT -> "2열 오른쪽"
    VehicleArea.ROW_3_LEFT -> "3열 왼쪽"
    VehicleArea.ROW_3_CENTER -> "3열 가운데"
    VehicleArea.ROW_3_RIGHT -> "3열 오른쪽"
    VehicleArea.HOOD -> "보닛"
    VehicleArea.TRUNK -> "트렁크"
    VehicleArea.FRONT_WINDSHIELD -> "앞 유리"
    VehicleArea.REAR_WINDSHIELD -> "뒷 유리"
    VehicleArea.ROOF -> "루프"
    VehicleArea.WHEEL_LEFT_FRONT -> "앞 왼쪽 타이어"
    VehicleArea.WHEEL_RIGHT_FRONT -> "앞 오른쪽 타이어"
    VehicleArea.WHEEL_LEFT_REAR -> "뒤 왼쪽 타이어"
    VehicleArea.WHEEL_RIGHT_REAR -> "뒤 오른쪽 타이어"
}
