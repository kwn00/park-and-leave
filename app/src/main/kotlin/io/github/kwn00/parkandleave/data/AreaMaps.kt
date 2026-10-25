package io.github.kwn00.parkandleave.data

import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType
import io.github.kwn00.parkandleave.domain.TelltaleKind
import io.github.kwn00.parkandleave.domain.VehicleArea

internal fun AccessType.canRead(): Boolean = when (this) {
    AccessType.READ, AccessType.READ_WRITE -> true
    AccessType.NONE, AccessType.WRITE -> false
}

internal fun WindowValue.toArea(): VehicleArea = when (this) {
    WindowValue.ROW_1_LEFT -> VehicleArea.ROW_1_LEFT
    WindowValue.ROW_1_RIGHT -> VehicleArea.ROW_1_RIGHT
    WindowValue.ROW_2_LEFT -> VehicleArea.ROW_2_LEFT
    WindowValue.ROW_2_RIGHT -> VehicleArea.ROW_2_RIGHT
    WindowValue.FRONT_WINDSHIELD -> VehicleArea.FRONT_WINDSHIELD
    WindowValue.REAR_WINDSHIELD -> VehicleArea.REAR_WINDSHIELD
    WindowValue.ROOF_TOP_1, WindowValue.ROOF_TOP_2 -> VehicleArea.ROOF
}

internal fun DoorValue.toArea(): VehicleArea = when (this) {
    DoorValue.ROW_1_LEFT -> VehicleArea.ROW_1_LEFT
    DoorValue.ROW_1_RIGHT -> VehicleArea.ROW_1_RIGHT
    DoorValue.ROW_2_LEFT -> VehicleArea.ROW_2_LEFT
    DoorValue.ROW_2_RIGHT -> VehicleArea.ROW_2_RIGHT
    DoorValue.ROW_3_LEFT -> VehicleArea.ROW_3_LEFT
    DoorValue.ROW_3_RIGHT -> VehicleArea.ROW_3_RIGHT
    DoorValue.HOOD -> VehicleArea.HOOD
    DoorValue.REAR -> VehicleArea.TRUNK
}

internal fun SeatValue.toArea(): VehicleArea = when (this) {
    SeatValue.ROW_1_LEFT -> VehicleArea.ROW_1_LEFT
    SeatValue.ROW_1_CENTER -> VehicleArea.ROW_1_CENTER
    SeatValue.ROW_1_RIGHT -> VehicleArea.ROW_1_RIGHT
    SeatValue.ROW_2_LEFT -> VehicleArea.ROW_2_LEFT
    SeatValue.ROW_2_CENTER -> VehicleArea.ROW_2_CENTER
    SeatValue.ROW_2_RIGHT -> VehicleArea.ROW_2_RIGHT
    SeatValue.ROW_3_LEFT -> VehicleArea.ROW_3_LEFT
    SeatValue.ROW_3_CENTER -> VehicleArea.ROW_3_CENTER
    SeatValue.ROW_3_RIGHT -> VehicleArea.ROW_3_RIGHT
}

internal fun WheelValue.toArea(): VehicleArea? = when (this) {
    WheelValue.LEFT_FRONT -> VehicleArea.WHEEL_LEFT_FRONT
    WheelValue.RIGHT_FRONT -> VehicleArea.WHEEL_RIGHT_FRONT
    WheelValue.LEFT_REAR -> VehicleArea.WHEEL_LEFT_REAR
    WheelValue.RIGHT_REAR -> VehicleArea.WHEEL_RIGHT_REAR
    WheelValue.UNKNOWN -> null
}

internal fun LightState?.asOn(): Boolean? = when (this) {
    null -> null
    LightState.OFF -> false
    LightState.ON, LightState.DAYTIME_RUNNING -> true
}

internal fun TelltaleState.isActive(): Boolean = when (this) {
    TelltaleState.ON, TelltaleState.BLINK -> true
    TelltaleState.OFF -> false
}

internal fun TelltaleType.toKind(): TelltaleKind = when (this) {
    TelltaleType.DOOR_OPEN -> TelltaleKind.DOOR_OPEN
    TelltaleType.TRUNK_OPEN -> TelltaleKind.TRUNK_OPEN
    TelltaleType.TPMS -> TelltaleKind.TPMS
    TelltaleType.LOW_FUEL, TelltaleType.LOW_FUEL_FCEV -> TelltaleKind.LOW_FUEL
    TelltaleType.LOW_EV_BATTERY -> TelltaleKind.LOW_EV_BATTERY
    TelltaleType.ENGINE -> TelltaleKind.ENGINE
    TelltaleType.MASTER_WARNING -> TelltaleKind.MASTER_WARNING
    TelltaleType.ABS -> TelltaleKind.ABS
    TelltaleType.AIRBAG -> TelltaleKind.AIRBAG
    TelltaleType.LOW_VOLT_BATTERY -> TelltaleKind.LOW_VOLT_BATTERY
    TelltaleType.ENGINE_OIL_PRESSURE -> TelltaleKind.ENGINE_OIL
    TelltaleType.ENGINE_TEMPERATURE -> TelltaleKind.ENGINE_TEMP
    TelltaleType.POWER_DOWN -> TelltaleKind.POWER_DOWN
    TelltaleType.EV_SYSTEM -> TelltaleKind.EV_SYSTEM
    TelltaleType.PARKING_BRAKE,
    TelltaleType.ELECTRONIC_PARKING_BRAKE,
    TelltaleType.SEAT_BELT,
    TelltaleType.MDPS,
    TelltaleType.ESC,
    TelltaleType.ESC_OFF,
    TelltaleType.AFS,
    TelltaleType.AUTO_HOLD,
    TelltaleType.AUTO_HIGH_BEAM,
    TelltaleType.HEADLAMP_LOW,
    TelltaleType.HEADLAMP_HIGH,
    TelltaleType.POSITION_LAMP,
    TelltaleType.FRONT_FOG_LAMP,
    TelltaleType.REAR_FOG_LAMP,
    TelltaleType.EMERGENCY_CALL,
    TelltaleType.RBS,
    TelltaleType.IMMOBILIZER,
    TelltaleType.LED_HEADLAMP,
    TelltaleType.PREHEAT,
    TelltaleType.PARTICLE_FILTER,
    TelltaleType.H2_SENSOR,
    TelltaleType.H2_LEAK,
    TelltaleType.PASSENGER_AIRBAG_OFF,
    TelltaleType.PASSENGER_AIRBAG_ON,
    TelltaleType.TURN_SIGNAL_LEFT,
    TelltaleType.TURN_SIGNAL_RIGHT,
    TelltaleType.ICY_ROAD,
    TelltaleType.OVER_SPEED,
    TelltaleType.WATER_SEPARATOR,
    TelltaleType.SCR,
    TelltaleType.HEV_SYSTEM,
    TelltaleType.FCEV_SYSTEM,
    TelltaleType.EREV_SYSTEM,
    TelltaleType.LPI,
    TelltaleType.EV_CHARGING_CONNECTOR,
    TelltaleType.AWD_LOCK,
    TelltaleType.HEV_MODE,
    TelltaleType.ISG,
    TelltaleType.DBC,
    TelltaleType.REV,
    TelltaleType.XEV_READY,
    TelltaleType.I_PEDAL,
    TelltaleType.EV_BATTERY_CONDITIONING,
    TelltaleType.CHILD_PRESENCE_DETECTION_OFF,
    TelltaleType.DRIVE_MODE,
    TelltaleType.DRIVE_MODE_SPORT,
    TelltaleType.DRIVE_MODE_ECO,
    TelltaleType.DRIVE_MODE_COMFORT,
    TelltaleType.DRIVE_MODE_TERRAIN,
    TelltaleType.FORCED_ALL_WHEEL_DRIVE,
    TelltaleType.FOUR_WHEEL_DRIVE_MODE_2H,
    TelltaleType.FOUR_WHEEL_DRIVE_MODE_4A,
    TelltaleType.FOUR_WHEEL_DRIVE_MODE_4H,
    TelltaleType.FOUR_WHEEL_DRIVE_MODE_4L,
    TelltaleType.TOW_MODE,
    TelltaleType.ESC_SPORT,
    TelltaleType.CREEP_OFF,
    TelltaleType.DRIFT_MODE,
    TelltaleType.TRACK_MODE,
    TelltaleType.UTILITY_MODE,
    TelltaleType.GREEN_ZONE,
    TelltaleType.PHEV_MODE,
    TelltaleType.PHEV_MODE_HEV,
    TelltaleType.PHEV_MODE_AUTO,
    TelltaleType.PHEV_MODE_EV_PLUS,
    TelltaleType.EREV_MODE,
    TelltaleType.LAUNCH_MODE,
    TelltaleType.ACCEL_RESPONSE_DASH,
    TelltaleType.ACCEL_RESPONSE_ACCEL,
    TelltaleType.N_BATTERY_DRAG,
    TelltaleType.N_BATTERY_SPRINT,
    TelltaleType.N_BATTERY_ENDURANCE,
    TelltaleType.FCA,
    TelltaleType.LKA,
    TelltaleType.LFA,
    TelltaleType.LCA_LEFT,
    TelltaleType.LCA_RIGHT,
    TelltaleType.ELK,
    TelltaleType.ICC,
    TelltaleType.DRIVER_ATTENTION,
    -> TelltaleKind.OTHER
}
