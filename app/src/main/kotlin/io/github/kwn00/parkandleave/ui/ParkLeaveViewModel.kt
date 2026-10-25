package io.github.kwn00.parkandleave.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kwn00.parkandleave.audio.FallbackDepartureSpeaker
import io.github.kwn00.parkandleave.data.AaosSignals
import io.github.kwn00.parkandleave.data.FakeSignalBoard
import io.github.kwn00.parkandleave.data.PleosSignalSource
import io.github.kwn00.parkandleave.domain.CheckSession
import io.github.kwn00.parkandleave.domain.DebugRow
import io.github.kwn00.parkandleave.domain.FakePreset
import io.github.kwn00.parkandleave.domain.Gear
import io.github.kwn00.parkandleave.domain.GearReading
import io.github.kwn00.parkandleave.domain.ParkingCheckEvaluator
import io.github.kwn00.parkandleave.domain.ParkingReport
import io.github.kwn00.parkandleave.domain.SessionPhase
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.checkSession
import io.github.kwn00.parkandleave.domain.formatDebugRows
import io.github.kwn00.parkandleave.future.DefaultPlaceProfileSource
import io.github.kwn00.parkandleave.future.UnavailableOnDeviceSpeech
import io.github.kwn00.parkandleave.future.UnavailableVehicleControl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DataOrigin {
    LIVE,
    FAKE,
}

enum class AppScreen {
    SUMMARY,
    DEBUG,
}

private data class Selection(
    val origin: DataOrigin,
    val screen: AppScreen,
    val manual: Boolean,
    val gear: GearReading,
)

private data class VehicleSide(
    val snapshot: VehicleSnapshot,
    val restricted: Boolean,
    val liveStatus: String,
    val vehicleStatus: String,
)

data class ParkUiState(
    val origin: DataOrigin,
    val screen: AppScreen,
    val gear: GearReading,
    val snapshot: VehicleSnapshot,
    val session: CheckSession,
    val report: ParkingReport?,
    val scanning: Boolean,
    val showManualCheck: Boolean,
    val debugRows: List<DebugRow>,
    val liveStatus: String,
    val vehicleStatus: String,
    val speechMode: String,
    val controlAvailable: Boolean,
    val placeLabel: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ParkLeaveViewModel(application: Application) : AndroidViewModel(application) {
    private val evaluator = ParkingCheckEvaluator()
    private val aaos = AaosSignals(application)
    private val vehicle = PleosSignalSource(application, viewModelScope)
    private val fake = FakeSignalBoard()
    private val control = UnavailableVehicleControl()
    private val places = DefaultPlaceProfileSource()
    private val speaker = FallbackDepartureSpeaker(application, UnavailableOnDeviceSpeech())

    private val origin = MutableStateFlow(DataOrigin.LIVE)
    private val screen = MutableStateFlow(AppScreen.SUMMARY)
    private val manual = MutableStateFlow(false)
    private var spoken = false

    private val selectedGear = origin.flatMapLatest { selected ->
        when (selected) {
            DataOrigin.LIVE -> aaos.gear
            DataOrigin.FAKE -> fake.gear
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, aaos.gear.value)

    private val selectedSnapshot = origin.flatMapLatest { selected ->
        when (selected) {
            DataOrigin.LIVE -> vehicle.snapshot
            DataOrigin.FAKE -> fake.snapshot
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, vehicle.snapshot.value)

    val state: StateFlow<ParkUiState> = combine(
        combine(origin, screen, manual, selectedGear) { selectedOrigin, selectedScreen, manualRequested, gear ->
            Selection(selectedOrigin, selectedScreen, manualRequested, gear)
        },
        combine(selectedSnapshot, aaos.restricted, aaos.status, vehicle.status) { snapshot, restricted, liveStatus, vehicleStatus ->
            VehicleSide(snapshot, restricted, liveStatus, vehicleStatus)
        },
    ) { selection, vehicleSide ->
        val restricted = selection.origin == DataOrigin.LIVE && vehicleSide.restricted
        val session = checkSession(selection.gear.gear, restricted, selection.manual)
        val report = if (vehicleSide.snapshot.ready && session.phase == SessionPhase.ACTIVE) {
            evaluator.evaluate(vehicleSide.snapshot)
        } else {
            null
        }
        val originLabel = when (selection.origin) {
            DataOrigin.LIVE -> "실측"
            DataOrigin.FAKE -> "모의"
        }
        ParkUiState(
            origin = selection.origin,
            screen = selection.screen,
            gear = selection.gear,
            snapshot = vehicleSide.snapshot,
            session = session,
            report = report,
            scanning = session.phase == SessionPhase.ACTIVE && !vehicleSide.snapshot.ready,
            showManualCheck = session.phase == SessionPhase.IDLE && selection.gear.gear == Gear.UNKNOWN,
            debugRows = formatDebugRows(originLabel, selection.gear, vehicleSide.snapshot),
            liveStatus = vehicleSide.liveStatus,
            vehicleStatus = vehicleSide.vehicleStatus,
            speechMode = speaker.modeLabel,
            controlAvailable = control.available,
            placeLabel = places.activeProfile().name,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        initialState(),
    )

    init {
        aaos.start()
        vehicle.start()
        viewModelScope.launch {
            state.collect { ui ->
                val line = ui.report?.speechLine
                if (ui.session.phase != SessionPhase.ACTIVE || line == null) {
                    if (ui.session.phase != SessionPhase.ACTIVE) {
                        spoken = false
                        speaker.stop()
                    }
                } else if (!spoken) {
                    spoken = true
                    speaker.speak(line)
                }
            }
        }
    }

    fun openDebug() {
        screen.value = AppScreen.DEBUG
    }

    fun closeDebug() {
        screen.value = AppScreen.SUMMARY
    }

    fun useLive() {
        origin.value = DataOrigin.LIVE
    }

    fun useFake() {
        origin.value = DataOrigin.FAKE
    }

    fun requestManualCheck() {
        manual.value = true
    }

    fun refresh() {
        aaos.refreshGear()
        vehicle.refresh()
    }

    fun applyPreset(preset: FakePreset) {
        origin.value = DataOrigin.FAKE
        fake.apply(preset)
    }

    fun setFakeGear(gear: Gear) {
        origin.value = DataOrigin.FAKE
        fake.setGear(gear)
    }

    fun closeFakeWindows() = mutateFake { fake.closeWindows() }

    fun turnOffFakeReadingLights() = mutateFake { fake.turnOffReadingLights() }

    fun clearFakeRearSeat() = mutateFake { fake.clearRearSeat() }

    fun closeFakeTrunk() = mutateFake { fake.closeTrunk() }

    override fun onCleared() {
        speaker.stop()
        vehicle.release()
        aaos.release()
    }

    private fun mutateFake(block: () -> Unit) {
        origin.value = DataOrigin.FAKE
        block()
    }

    private fun initialState(): ParkUiState {
        val gear = GearReading(rawValue = null, gear = Gear.UNKNOWN, error = "시작 중")
        val snapshot = VehicleSnapshot(ready = false)
        return ParkUiState(
            origin = DataOrigin.LIVE,
            screen = AppScreen.SUMMARY,
            gear = gear,
            snapshot = snapshot,
            session = checkSession(Gear.UNKNOWN, restricted = false, manual = false),
            report = null,
            scanning = false,
            showManualCheck = true,
            debugRows = formatDebugRows("실측", gear, snapshot),
            liveStatus = "시작 중",
            vehicleStatus = "시작 중",
            speechMode = speaker.modeLabel,
            controlAvailable = false,
            placeLabel = places.activeProfile().name,
        )
    }
}
