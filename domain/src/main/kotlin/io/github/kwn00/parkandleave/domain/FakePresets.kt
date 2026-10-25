package io.github.kwn00.parkandleave.domain

enum class FakePreset {
    ALL_CLEAR,
    WINDOW_AND_READING,
    REAR_SEAT_AND_TRUNK,
    LOW_SOC,
    DOORS_UNLOCKED,
}

object FakePresets {
    fun snapshot(preset: FakePreset): VehicleSnapshot = when (preset) {
        FakePreset.ALL_CLEAR -> allClear()
        FakePreset.WINDOW_AND_READING -> windowAndReading()
        FakePreset.REAR_SEAT_AND_TRUNK -> rearSeatAndTrunk()
        FakePreset.LOW_SOC -> lowSoc()
        FakePreset.DOORS_UNLOCKED -> doorsUnlocked()
    }

    fun allClear(): VehicleSnapshot = VehicleSnapshot(
        windows = listOf(closedWindow(VehicleArea.ROW_2_RIGHT)),
        doors = listOf(closedLocked(VehicleArea.ROW_1_LEFT)),
        cabinLightOn = false,
        readingLights = listOf(ReadingLightReading(VehicleArea.ROW_1_LEFT, on = false)),
        lowBeamOn = false,
        tailLampOn = false,
        seats = listOf(SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.VACANT)),
        hvacOn = false,
        tires = listOf(TireReading(VehicleArea.WHEEL_LEFT_FRONT, lowPressure = 0f)),
        washerLow = false,
        telltales = emptyList(),
        socPercent = 70,
        fuelPercent = 60,
        tripDistance = 12f,
    )

    fun windowAndReading(): VehicleSnapshot = allClear().copy(
        windows = listOf(WindowReading(VehicleArea.ROW_2_RIGHT, position = 3, maxPosition = 10, supported = true)),
        readingLights = listOf(ReadingLightReading(VehicleArea.ROW_2_RIGHT, on = true)),
    )

    fun rearSeatAndTrunk(): VehicleSnapshot = allClear().copy(
        doors = listOf(
            closedLocked(VehicleArea.ROW_1_LEFT),
            DoorReading(VehicleArea.TRUNK, position = 1, locked = true),
        ),
        seats = listOf(SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.OCCUPIED)),
        telltales = listOf(TelltaleReading(TelltaleKind.TRUNK_OPEN, active = true, rawName = "TRUNK_OPEN")),
    )

    fun lowSoc(): VehicleSnapshot = allClear().copy(
        socPercent = 18,
        tripDistance = 22f,
    )

    fun doorsUnlocked(): VehicleSnapshot = allClear().copy(
        doors = listOf(DoorReading(VehicleArea.ROW_1_LEFT, position = 0, locked = false)),
    )

    private fun closedWindow(area: VehicleArea) = WindowReading(
        area = area,
        position = 0,
        maxPosition = 10,
        supported = true,
    )

    private fun closedLocked(area: VehicleArea) = DoorReading(
        area = area,
        position = 0,
        locked = true,
    )
}
