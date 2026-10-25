package io.github.kwn00.parkandleave.ui

import io.github.kwn00.parkandleave.designsystem.ParkIconKind
import io.github.kwn00.parkandleave.domain.SignalId

fun glyphFor(id: SignalId): ParkIconKind = when (id) {
    SignalId.REAR_OCCUPANCY -> ParkIconKind.OCCUPANT
    SignalId.TRUNK -> ParkIconKind.TRUNK
    SignalId.HOOD -> ParkIconKind.HOOD
    SignalId.DOOR_OPEN -> ParkIconKind.DOOR
    SignalId.TIRE -> ParkIconKind.TIRE
    SignalId.TELLTALE -> ParkIconKind.ALERT
    SignalId.WINDOW -> ParkIconKind.WINDOW
    SignalId.DOOR_UNLOCKED -> ParkIconKind.UNLOCK
    SignalId.CABIN_LIGHT -> ParkIconKind.CABIN
    SignalId.READING_LIGHT -> ParkIconKind.READING
    SignalId.EXTERIOR_LIGHT -> ParkIconKind.LAMP
    SignalId.HVAC -> ParkIconKind.HVAC
    SignalId.WASHER -> ParkIconKind.DROP
    SignalId.LOW_SOC -> ParkIconKind.BATTERY
    SignalId.LOW_FUEL -> ParkIconKind.FUEL
}
