package io.github.kwn00.parkandleave.data

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.listener.DoorLockStateListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.HvacPowerStateListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.LowWasherFluidWaringListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.content.Context
import io.github.kwn00.parkandleave.domain.DoorReading
import io.github.kwn00.parkandleave.domain.ReadingLightReading
import io.github.kwn00.parkandleave.domain.SeatReading
import io.github.kwn00.parkandleave.domain.TelltaleReading
import io.github.kwn00.parkandleave.domain.TireReading
import io.github.kwn00.parkandleave.domain.VehicleArea
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.WindowReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reads Vehicle SDK 2.3.2 state. Telltales are queried once per scan:
 * `getTelltalesInfo` and `registerTelltales` both require `PRIVATE_CAR_SAFETY`
 * in this SDK build, so a normal app may see that call fail. Tire pressure
 * registration is private as well, so low-pressure is read once per scan.
 * `getDoorLockState` Boolean is treated as locked=true.
 */
class PleosSignalSource(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val vehicle = Vehicle(context.applicationContext)
    val snapshot = MutableStateFlow(VehicleSnapshot(ready = false))
    val status = MutableStateFlow("차량 서비스 연결 전")

    private var started = false
    private var released = false
    private val unregisterActions = mutableListOf<() -> Unit>()
    private val windowTracks = mutableListOf<Tracked<WindowArea>>()
    private val doorPositionTracks = mutableListOf<Tracked<DoorArea>>()
    private val doorLockTracks = mutableListOf<Tracked<DoorArea>>()
    private val readingTracks = mutableListOf<Tracked<SeatArea>>()
    private val seatTracks = mutableListOf<Tracked<SeatArea>>()
    private val hvacTracks = mutableListOf<Tracked<SeatArea>>()
    private var cabinSupported = false
    private var lowBeamSupported = false
    private var tailSupported = false
    private var washerSupported = false
    private var socSupported = false
    private var fuelSupported = false
    private var tripSupported = false

    fun start() {
        if (started || released) return
        started = true
        try {
            vehicle.initialize()
            status.value = "Vehicle.initialize 완료"
            scope.launch { rescan() }
        } catch (error: Exception) {
            status.value = "Vehicle.initialize 실패"
            snapshot.value = VehicleSnapshot(
                ready = true,
                readErrors = listOf("Vehicle.initialize: ${error.shortMessage()}"),
            )
        }
    }

    fun refresh() {
        if (!started || released) return
        scope.launch { rescan() }
    }

    fun release() {
        released = true
        stopListeners()
        if (!started) return
        try {
            vehicle.release()
        } catch (_: Exception) {
        }
    }

    private suspend fun rescan() {
        if (released) return
        stopListeners()
        val next = scan()
        if (released) return
        snapshot.value = next
        startListeners()
        status.value = if (next.readErrors.isEmpty()) "차량 신호 수신" else "일부 신호 실패 ${next.readErrors.size}건"
    }

    private suspend fun scan(): VehicleSnapshot {
        val bag = ScanBag()
        val windows = readWindows(bag)
        val doors = readDoors(bag)
        val cabin = readCabin(bag)
        val reading = readReadingLights(bag)
        val lowBeam = readLowBeam(bag)
        val tail = readTailLamp(bag)
        val seats = readSeats(bag)
        val hvac = readHvac(bag)
        val tires = readTires(bag)
        val washer = readWasher(bag)
        val telltales = readTelltales(bag)
        val soc = readSoc(bag)
        val fuel = readFuel(bag)
        val trip = readTrip(bag)
        return VehicleSnapshot(
            ready = true,
            windows = windows,
            doors = doors,
            cabinLightOn = cabin,
            readingLights = reading,
            lowBeamOn = lowBeam,
            tailLampOn = tail,
            seats = seats,
            hvacOn = hvac.any { it.on == true }.takeIf { hvac.isNotEmpty() } ?: if (hvac.isEmpty()) null else false,
            hvacZones = hvac,
            tires = tires,
            washerLow = washer,
            telltales = telltales,
            socPercent = soc,
            fuelPercent = fuel,
            tripDistance = trip,
            readErrors = bag.errors.toList(),
            notes = bag.notes.toList(),
        )
    }

    private suspend fun readWindows(bag: ScanBag): List<WindowReading> {
        windowTracks.clear()
        val checked = awaitSdk { complete: (Pair<CapabilityConfig, Map<WindowArea, *>>) -> Unit, fail ->
            vehicle.getWindow().checkWindowPositionCapabilities(
                onComplete = { config, bounds -> complete(config to bounds) },
                onFailed = fail,
            )
        }
        val (config, bounds) = checked.getOrElse { error ->
            bag.errors += "창문 capability: ${error.shortMessage()}"
            return emptyList()
        }
        if (!config.accessType.canRead()) {
            bag.notes += "창문 위치 미지원 (${config.accessType.name})"
            return emptyList()
        }
        @Suppress("UNCHECKED_CAST")
        val areaBounds = bounds as Map<WindowArea, ai.pleos.playground.vehicle.api.model.capability.AreaBounds<Int>>
        return areaBounds.mapNotNull { (area, bound) ->
            val domain = area.values.firstOrNull()?.toArea() ?: return@mapNotNull null
            val position = awaitSdk<Int?> { complete, fail ->
                vehicle.getWindow().getCurrentWindowPosition(area, complete, fail)
            }.getOrElse { error ->
                bag.errors += "창문 ${domain.name}: ${error.shortMessage()}"
                null
            }
            if (position != null) windowTracks += Tracked(area, domain)
            WindowReading(
                area = domain,
                position = position ?: 0,
                maxPosition = bound.maxValue ?: 0,
                supported = position != null,
            )
        }
    }

    private suspend fun readDoors(bag: ScanBag): List<DoorReading> {
        doorPositionTracks.clear()
        doorLockTracks.clear()
        val positions = linkedMapOf<VehicleArea, Int?>()
        val locks = linkedMapOf<VehicleArea, Boolean?>()
        val positionCaps = awaitSdk { complete: (Pair<CapabilityConfig, Map<DoorArea, *>>) -> Unit, fail ->
            vehicle.getDoor().checkDoorPositionCapabilities(
                onComplete = { config, bounds -> complete(config to bounds) },
                onFailed = fail,
            )
        }
        positionCaps.fold(
            onSuccess = { (config, bounds) ->
                if (!config.accessType.canRead()) {
                    bag.notes += "도어 위치 미지원 (${config.accessType.name})"
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val areaBounds = bounds as Map<DoorArea, ai.pleos.playground.vehicle.api.model.capability.AreaBounds<Int>>
                    areaBounds.forEach { (area, _) ->
                        val domain = area.values.firstOrNull()?.toArea() ?: return@forEach
                        val position = awaitSdk<Int?> { complete, fail ->
                            vehicle.getDoor().getCurrentDoorPosition(area, complete, fail)
                        }.getOrElse { error ->
                            bag.errors += "도어 위치 ${domain.name}: ${error.shortMessage()}"
                            null
                        }
                        positions[domain] = position
                        if (position != null) doorPositionTracks += Tracked(area, domain)
                    }
                }
            },
            onFailure = { bag.errors += "도어 위치 capability: ${it.shortMessage()}" },
        )
        val lockCaps = awaitSdk { complete: (Pair<CapabilityConfig, List<DoorArea>>) -> Unit, fail ->
            vehicle.getDoor().checkDoorLockCapabilities(
                onComplete = { config, areas -> complete(config to areas) },
                onFailed = fail,
            )
        }
        lockCaps.fold(
            onSuccess = { (config, areas) ->
                if (!config.accessType.canRead()) {
                    bag.notes += "도어 잠금 미지원 (${config.accessType.name})"
                } else {
                    areas.forEach { area ->
                        val domain = area.values.firstOrNull()?.toArea() ?: return@forEach
                        val locked = awaitSdk<Boolean?> { complete, fail ->
                            vehicle.getDoor().getDoorLockState(area, complete, fail)
                        }.getOrElse { error ->
                            bag.errors += "도어 잠금 ${domain.name}: ${error.shortMessage()}"
                            null
                        }
                        locks[domain] = locked
                        if (locked != null) doorLockTracks += Tracked(area, domain)
                    }
                }
            },
            onFailure = { bag.errors += "도어 잠금 capability: ${it.shortMessage()}" },
        )
        val areas = (positions.keys + locks.keys).distinct()
        return areas.map { area -> DoorReading(area, positions[area], locks[area]) }
    }

    private suspend fun readCabin(bag: ScanBag): Boolean? {
        cabinSupported = false
        return readGlobalLight(
            bag = bag,
            label = "실내등",
            check = { complete, fail ->
                vehicle.getLight().checkCabinLightStateCapabilities(
                    onComplete = { config, _ -> complete(config) },
                    onFailed = fail,
                )
            },
            read = { complete, fail -> vehicle.getLight().getCurrentCabinLightState(complete, fail) },
            onSupported = { cabinSupported = true },
        )
    }

    private suspend fun readLowBeam(bag: ScanBag): Boolean? {
        lowBeamSupported = false
        return readGlobalLight(
            bag = bag,
            label = "하향등",
            check = { complete, fail ->
                vehicle.getLight().checkLowBeamLightStateCapabilities(
                    onComplete = { config, _ -> complete(config) },
                    onFailed = fail,
                )
            },
            read = { complete, fail -> vehicle.getLight().getCurrentLowBeamLightState(complete, fail) },
            onSupported = { lowBeamSupported = true },
        )
    }

    private suspend fun readTailLamp(bag: ScanBag): Boolean? {
        tailSupported = false
        return readGlobalLight(
            bag = bag,
            label = "미등",
            check = { complete, fail ->
                vehicle.getLight().checkTailLampLightStateCapabilities(
                    onComplete = { config, _ -> complete(config) },
                    onFailed = fail,
                )
            },
            read = { complete, fail -> vehicle.getLight().getCurrentTailLampLightState(complete, fail) },
            onSupported = { tailSupported = true },
        )
    }

    private suspend fun readReadingLights(bag: ScanBag): List<ReadingLightReading> {
        readingTracks.clear()
        val checked = awaitSdk { complete: (Pair<CapabilityConfig, Map<SeatArea, *>>) -> Unit, fail ->
            vehicle.getLight().checkReadingLightStateCapabilities(
                onComplete = { config, areas -> complete(config to areas) },
                onFailed = fail,
            )
        }
        val (config, areas) = checked.getOrElse { error ->
            bag.errors += "독서등 capability: ${error.shortMessage()}"
            return emptyList()
        }
        if (!config.accessType.canRead()) {
            bag.notes += "독서등 미지원 (${config.accessType.name})"
            return emptyList()
        }
        return areas.keys.mapNotNull { area ->
            val domain = area.values.firstOrNull()?.toArea() ?: return@mapNotNull null
            val state = awaitSdk { complete: (ai.pleos.playground.vehicle.constant.control.LightState?) -> Unit, fail ->
                vehicle.getLight().getCurrentReadingLightState(area, complete, fail)
            }.getOrElse { error ->
                bag.errors += "독서등 ${domain.name}: ${error.shortMessage()}"
                null
            }
            if (state != null) readingTracks += Tracked(area, domain)
            ReadingLightReading(domain, state.asOn())
        }
    }

    private suspend fun readSeats(bag: ScanBag): List<SeatReading> {
        seatTracks.clear()
        val checked = awaitSdk { complete: (Pair<CapabilityConfig, List<SeatArea>>) -> Unit, fail ->
            vehicle.getSeat().checkSeatOccupancyCapabilities(
                onComplete = { config, areas -> complete(config to areas) },
                onFailed = fail,
            )
        }
        val (config, areas) = checked.getOrElse { error ->
            bag.errors += "좌석 점유 capability: ${error.shortMessage()}"
            return emptyList()
        }
        if (!config.accessType.canRead()) {
            bag.notes += "좌석 점유 미지원 (${config.accessType.name})"
            return emptyList()
        }
        return areas.mapNotNull { area ->
            val domain = area.values.firstOrNull()?.toArea() ?: return@mapNotNull null
            val occupied = awaitSdk<Boolean?> { complete, fail ->
                vehicle.getSeat().getCurrentSeatOccupancy(area, complete, fail)
            }.getOrElse { error ->
                bag.errors += "좌석 ${domain.name}: ${error.shortMessage()}"
                null
            }
            if (occupied != null) seatTracks += Tracked(area, domain)
            SeatReading(domain, occupied)
        }
    }

    private suspend fun readHvac(bag: ScanBag): List<ReadingLightReading> {
        hvacTracks.clear()
        val checked = awaitSdk { complete: (Pair<CapabilityConfig, List<SeatArea>>) -> Unit, fail ->
            vehicle.getHVAC().checkHvacPowerCapabilities(
                onComplete = { config, areas -> complete(config to areas) },
                onFailed = fail,
            )
        }
        val (config, areas) = checked.getOrElse { error ->
            bag.errors += "공조 capability: ${error.shortMessage()}"
            return emptyList()
        }
        if (!config.accessType.canRead()) {
            bag.notes += "공조 미지원 (${config.accessType.name})"
            return emptyList()
        }
        return areas.mapNotNull { area ->
            val domain = area.values.firstOrNull()?.toArea() ?: return@mapNotNull null
            val power = awaitSdk<Boolean?> { complete, fail ->
                vehicle.getHVAC().getHvacPowerState(area, complete, fail)
            }.getOrElse { error ->
                bag.errors += "공조 ${domain.name}: ${error.shortMessage()}"
                null
            }
            if (power != null) hvacTracks += Tracked(area, domain)
            ReadingLightReading(domain, power)
        }
    }

    private suspend fun readTires(bag: ScanBag): List<TireReading> {
        val checked = awaitSdk { complete: (Pair<CapabilityConfig, List<WheelArea>>) -> Unit, fail ->
            vehicle.getTire().checkLowTirePressureCapabilities(
                onComplete = { config, areas -> complete(config to areas) },
                onFailed = fail,
            )
        }
        val (config, areas) = checked.getOrElse { error ->
            bag.errors += "타이어 capability: ${error.shortMessage()}"
            return emptyList()
        }
        if (!config.accessType.canRead()) {
            bag.notes += "타이어 저압 미지원 (${config.accessType.name})"
            return emptyList()
        }
        return areas.mapNotNull { area ->
            val domain = area.values.firstOrNull()?.toArea() ?: return@mapNotNull null
            val value = awaitSdk<Float?> { complete, fail ->
                vehicle.getTire().getLowTirePressure(area, complete, fail)
            }.getOrElse { error ->
                bag.errors += "타이어 ${domain.name}: ${error.shortMessage()}"
                null
            }
            TireReading(domain, value)
        }
    }

    private suspend fun readWasher(bag: ScanBag): Boolean? {
        washerSupported = false
        val config = awaitSdk<CapabilityConfig> { complete, fail ->
            vehicle.getSafety().checkLowWasherFluidWarningStateCapabilities(complete, fail)
        }.getOrElse { error ->
            bag.errors += "워셔액 capability: ${error.shortMessage()}"
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "워셔액 미지원 (${config.accessType.name})"
            return null
        }
        washerSupported = true
        return awaitSdk<Boolean?> { complete, fail ->
            vehicle.getSafety().hasLowWasherFluidWarning(complete, fail)
        }.getOrElse { error ->
            bag.errors += "워셔액: ${error.shortMessage()}"
            null
        }
    }

    private suspend fun readTelltales(bag: ScanBag): List<TelltaleReading> {
        val info = awaitSdk { complete: (ai.pleos.playground.vehicle.api.model.telltale.Telltales) -> Unit, fail ->
            vehicle.getSafety().getTelltalesInfo(complete, fail)
        }.getOrElse { error ->
            bag.errors += "경고등: ${error.shortMessage()} (SDK 2.3.2 getTelltalesInfo 는 PRIVATE_CAR_SAFETY)"
            return emptyList()
        }
        return info.telltales.map { data ->
            TelltaleReading(
                kind = data.type.toKind(),
                active = data.state.isActive(),
                rawName = data.type.name,
            )
        }
    }

    private suspend fun readSoc(bag: ScanBag): Int? {
        socSupported = false
        return readScalar(
            bag = bag,
            label = "SOC",
            check = { complete, fail ->
                vehicle.getEvBattery().checkEvBatteryStateOfChargeCapabilities(complete, fail)
            },
            read = { complete, fail -> vehicle.getEvBattery().getEvBatteryStateOfCharge(complete, fail) },
            onSupported = { socSupported = true },
        )
    }

    private suspend fun readFuel(bag: ScanBag): Int? {
        fuelSupported = false
        return readScalar(
            bag = bag,
            label = "연료",
            check = { complete, fail -> vehicle.getFuel().checkFuelPercentCapabilities(complete, fail) },
            read = { complete, fail -> vehicle.getFuel().getFuelPercent(complete, fail) },
            onSupported = { fuelSupported = true },
        )
    }

    private suspend fun readTrip(bag: ScanBag): Float? {
        tripSupported = false
        val config = awaitSdk<CapabilityConfig> { complete, fail ->
            vehicle.getOdometer().checkTripCurrentDrivingDistanceCapabilities(complete, fail)
        }.getOrElse { error ->
            bag.errors += "트립 capability: ${error.shortMessage()}"
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "트립 거리 미지원 (${config.accessType.name})"
            return null
        }
        tripSupported = true
        return awaitSdk<Float?> { complete, fail ->
            vehicle.getOdometer().getTripCurrentDrivingDistance(complete, fail)
        }.getOrElse { error ->
            bag.errors += "트립: ${error.shortMessage()}"
            null
        }
    }

    private fun startListeners() {
        val windowApi = vehicle.getWindow()
        windowTracks.forEach { tracked ->
            val listener = object : WindowPositionListener {
                override fun onWindowPositionUpdated(position: Int?) = updateWindow(tracked.area, position)
                override fun onFailed(exception: Exception) = addError("창문 ${tracked.area.name}: ${exception.shortMessage()}")
            }
            safeRegister("창문 ${tracked.area.name}") {
                windowApi.registerWindowPosition(tracked.sdk, listener)
                unregisterActions += { windowApi.unregisterWindowPosition(tracked.sdk, listener) }
            }
        }
        val doorApi = vehicle.getDoor()
        doorPositionTracks.forEach { tracked ->
            val listener = object : DoorPositionListener {
                override fun onPositionUpdated(position: Int?) = updateDoorPosition(tracked.area, position)
                override fun onFailed(exception: Exception) = addError("도어 ${tracked.area.name}: ${exception.shortMessage()}")
            }
            safeRegister("도어 위치 ${tracked.area.name}") {
                doorApi.registerDoorPosition(tracked.sdk, listener)
                unregisterActions += { doorApi.unregisterDoorPosition(tracked.sdk, listener) }
            }
        }
        doorLockTracks.forEach { tracked ->
            val listener = object : DoorLockStateListener {
                override fun onDoorLockStateUpdated(locked: Boolean?) = updateDoorLock(tracked.area, locked)
                override fun onFailed(exception: Exception) = addError("잠금 ${tracked.area.name}: ${exception.shortMessage()}")
            }
            safeRegister("도어 잠금 ${tracked.area.name}") {
                doorApi.registerDoorLockState(tracked.sdk, listener)
                unregisterActions += { doorApi.unregisterDoorLockState(tracked.sdk, listener) }
            }
        }
        val lightApi = vehicle.getLight()
        if (cabinSupported) {
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    snapshot.update { it.copy(cabinLightOn = state.asOn()) }
                }
                override fun onFailed(exception: Exception) = addError("실내등: ${exception.shortMessage()}")
            }
            safeRegister("실내등") {
                lightApi.registerCabinLightState(listener)
                unregisterActions += { lightApi.unregisterCabinLightState(listener) }
            }
        }
        if (lowBeamSupported) {
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    snapshot.update { it.copy(lowBeamOn = state.asOn()) }
                }
                override fun onFailed(exception: Exception) = addError("하향등: ${exception.shortMessage()}")
            }
            safeRegister("하향등") {
                lightApi.registerLowBeamLightState(listener)
                unregisterActions += { lightApi.unregisterLowBeamLightState(listener) }
            }
        }
        if (tailSupported) {
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    snapshot.update { it.copy(tailLampOn = state.asOn()) }
                }
                override fun onFailed(exception: Exception) = addError("미등: ${exception.shortMessage()}")
            }
            safeRegister("미등") {
                lightApi.registerTailLampLightState(listener)
                unregisterActions += { lightApi.unregisterTailLampLightState(listener) }
            }
        }
        readingTracks.forEach { tracked ->
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    updateReading(tracked.area, state.asOn())
                }
                override fun onFailed(exception: Exception) = addError("독서등 ${tracked.area.name}: ${exception.shortMessage()}")
            }
            safeRegister("독서등 ${tracked.area.name}") {
                lightApi.registerReadingLightState(tracked.sdk, listener)
                unregisterActions += { lightApi.unregisterReadingLightState(tracked.sdk, listener) }
            }
        }
        val seatApi = vehicle.getSeat()
        seatTracks.forEach { tracked ->
            val listener = object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(occupied: Boolean?) = updateSeat(tracked.area, occupied)
                override fun onFailed(exception: Exception) = addError("좌석 ${tracked.area.name}: ${exception.shortMessage()}")
            }
            safeRegister("좌석 ${tracked.area.name}") {
                seatApi.registerSeatOccupancy(tracked.sdk, listener)
                unregisterActions += { seatApi.unregisterSeatOccupancy(tracked.sdk, listener) }
            }
        }
        val hvacApi = vehicle.getHVAC()
        hvacTracks.forEach { tracked ->
            val listener = object : HvacPowerStateListener {
                override fun onHvacPowerStateUpdated(power: Boolean?) = updateHvac(tracked.area, power)
                override fun onFailed(exception: Exception) = addError("공조 ${tracked.area.name}: ${exception.shortMessage()}")
            }
            safeRegister("공조 ${tracked.area.name}") {
                hvacApi.registerHvacPowerState(tracked.sdk, listener)
                unregisterActions += { hvacApi.unregisterHvacPowerState(tracked.sdk, listener) }
            }
        }
        if (washerSupported) {
            val listener = object : LowWasherFluidWaringListener {
                override fun onLowWasherFluidWaringUpdated(low: Boolean?) {
                    snapshot.update { it.copy(washerLow = low) }
                }
                override fun onFailed(exception: Exception) = addError("워셔액: ${exception.shortMessage()}")
            }
            safeRegister("워셔액") {
                vehicle.getSafety().registerLowWasherFluidWaringState(listener)
                unregisterActions += { vehicle.getSafety().unregisterLowWasherFluidWaringState(listener) }
            }
        }
        if (socSupported) {
            val listener = object : EvBatteryStateOfChargeListener {
                override fun onEvBatteryStateOfChargeUpdated(soc: Int?) {
                    snapshot.update { it.copy(socPercent = soc) }
                }
                override fun onFailed(exception: Exception) = addError("SOC: ${exception.shortMessage()}")
            }
            safeRegister("SOC") {
                vehicle.getEvBattery().registerEvBatteryStateOfCharge(listener)
                unregisterActions += { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(listener) }
            }
        }
        if (fuelSupported) {
            val listener = object : FuelPercentListener {
                override fun onFuelPercentUpdated(fuel: Int?) {
                    snapshot.update { it.copy(fuelPercent = fuel) }
                }
                override fun onFailed(exception: Exception) = addError("연료: ${exception.shortMessage()}")
            }
            safeRegister("연료") {
                vehicle.getFuel().registerFuelPercent(listener)
                unregisterActions += { vehicle.getFuel().unregisterFuelPercent(listener) }
            }
        }
        if (tripSupported) {
            val listener = object : DistanceDrivenListener {
                override fun onDistanceDrivenUpdated(distance: Float?) {
                    snapshot.update { it.copy(tripDistance = distance) }
                }
                override fun onFailed(exception: Exception) = addError("트립: ${exception.shortMessage()}")
            }
            safeRegister("트립") {
                vehicle.getOdometer().registerTripCurrentDrivingDistance(listener)
                unregisterActions += { vehicle.getOdometer().unregisterTripCurrentDrivingDistance(listener) }
            }
        }
    }

    private fun stopListeners() {
        unregisterActions.forEach { action ->
            try {
                action()
            } catch (_: Exception) {
            }
        }
        unregisterActions.clear()
    }

    private fun safeRegister(label: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            addError("$label 구독: ${error.shortMessage()}")
        }
    }

    private fun addError(message: String) {
        snapshot.update { current ->
            if (message in current.readErrors) current else current.copy(readErrors = current.readErrors + message)
        }
    }

    private fun updateWindow(area: VehicleArea, position: Int?) {
        if (position == null) return
        snapshot.update { current ->
            current.copy(
                windows = current.windows.map { window ->
                    if (window.area == area) window.copy(position = position, supported = true) else window
                },
            )
        }
    }

    private fun updateDoorPosition(area: VehicleArea, position: Int?) {
        snapshot.update { current ->
            current.copy(
                doors = current.doors.map { door ->
                    if (door.area == area) door.copy(position = position) else door
                },
            )
        }
    }

    private fun updateDoorLock(area: VehicleArea, locked: Boolean?) {
        snapshot.update { current ->
            current.copy(
                doors = current.doors.map { door ->
                    if (door.area == area) door.copy(locked = locked) else door
                },
            )
        }
    }

    private fun updateReading(area: VehicleArea, on: Boolean?) {
        snapshot.update { current ->
            current.copy(
                readingLights = current.readingLights.map { light ->
                    if (light.area == area) light.copy(on = on) else light
                },
            )
        }
    }

    private fun updateSeat(area: VehicleArea, occupied: Boolean?) {
        snapshot.update { current ->
            current.copy(
                seats = current.seats.map { seat ->
                    if (seat.area == area) seat.copy(occupied = occupied) else seat
                },
            )
        }
    }

    private fun updateHvac(area: VehicleArea, power: Boolean?) {
        snapshot.update { current ->
            val zones = current.hvacZones.map { zone ->
                if (zone.area == area) zone.copy(on = power) else zone
            }
            current.copy(
                hvacZones = zones,
                hvacOn = zones.any { it.on == true },
            )
        }
    }

    private suspend fun readGlobalLight(
        bag: ScanBag,
        label: String,
        check: (onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        read: (onComplete: (ai.pleos.playground.vehicle.constant.control.LightState?) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onSupported: () -> Unit,
    ): Boolean? {
        val config = awaitSdk(call = check).getOrElse { error ->
            bag.errors += "$label capability: ${error.shortMessage()}"
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "$label 미지원 (${config.accessType.name})"
            return null
        }
        onSupported()
        val state = awaitSdk(call = read).getOrElse { error ->
            bag.errors += "$label: ${error.shortMessage()}"
            return null
        }
        return state.asOn()
    }

    private suspend fun readScalar(
        bag: ScanBag,
        label: String,
        check: (onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        read: (onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onSupported: () -> Unit,
    ): Int? {
        val config = awaitSdk(call = check).getOrElse { error ->
            bag.errors += "$label capability: ${error.shortMessage()}"
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "$label 미지원 (${config.accessType.name})"
            return null
        }
        onSupported()
        return awaitSdk(call = read).getOrElse { error ->
            bag.errors += "$label: ${error.shortMessage()}"
            null
        }
    }

    private class ScanBag {
        val errors = mutableListOf<String>()
        val notes = mutableListOf<String>()
    }

    private data class Tracked<T>(val sdk: T, val area: VehicleArea)
}
