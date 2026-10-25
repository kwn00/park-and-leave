package io.github.kwn00.parkandleave.data

import io.github.kwn00.parkandleave.domain.FakePreset
import io.github.kwn00.parkandleave.domain.FakePresets
import io.github.kwn00.parkandleave.domain.Gear
import io.github.kwn00.parkandleave.domain.GearReading
import io.github.kwn00.parkandleave.domain.TelltaleKind
import io.github.kwn00.parkandleave.domain.VehicleArea
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.rawCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSignalBoard {
    val gear = MutableStateFlow(parked())
    val snapshot = MutableStateFlow(FakePresets.allClear())

    fun apply(preset: FakePreset) {
        gear.value = parked()
        snapshot.value = FakePresets.snapshot(preset)
    }

    fun setGear(next: Gear) {
        gear.value = GearReading(
            rawValue = next.rawCode(),
            gear = next,
            error = null,
        )
    }

    fun closeWindows() = snapshot.update { current ->
        current.copy(windows = current.windows.map { it.copy(position = 0) })
    }

    fun turnOffReadingLights() = snapshot.update { current ->
        current.copy(readingLights = current.readingLights.map { it.copy(on = false) })
    }

    fun closeTrunk() = snapshot.update { current ->
        current.copy(
            doors = current.doors.map { door ->
                if (door.area == VehicleArea.TRUNK) door.copy(position = 0) else door
            },
            telltales = current.telltales.map { telltale ->
                if (telltale.kind == TelltaleKind.TRUNK_OPEN) telltale.copy(active = false) else telltale
            },
        )
    }

    private fun parked(): GearReading = GearReading(
        rawValue = Gear.PARK.rawCode(),
        gear = Gear.PARK,
        error = null,
    )
}
