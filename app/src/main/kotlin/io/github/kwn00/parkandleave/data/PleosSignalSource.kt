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
import io.github.kwn00.parkandleave.domain.SdkLink
import io.github.kwn00.parkandleave.domain.label
import io.github.kwn00.parkandleave.domain.SeatReading
import io.github.kwn00.parkandleave.domain.SignalSource
import io.github.kwn00.parkandleave.domain.TelltaleReading
import io.github.kwn00.parkandleave.domain.TireReading
import io.github.kwn00.parkandleave.domain.UnreadCause
import io.github.kwn00.parkandleave.domain.UnreadSignal
import io.github.kwn00.parkandleave.domain.VehicleArea
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.WindowReading
import io.github.kwn00.parkandleave.domain.causeOfReadFailure
import io.github.kwn00.parkandleave.domain.collapseDisconnected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reads Vehicle SDK 2.3.2 state once per scan. Each source is isolated:
 * a SecurityException or a not-available result on one call does not skip the rest.
 *
 * Telltales stay unavailable for a third-party app. Vehicle SDK 2.3.2 `Safety`
 * KDoc says `getTelltalesInfo`, `checkTelltalesCapabilities`, and
 * `registerTelltales` require `pleos.car.permission.PRIVATE_CAR_SAFETY`
 * (`signature|privileged`). `SafetyImpl` enforces that with
 * `@RequiresCustomPermission`, and `PermissionUtil` throws SecurityException
 * when the permission is missing. The interface method is also annotated
 * `@RequiresPermission(CAR_SAFETY)`, but the KDoc and the implementation check
 * both name `PRIVATE_CAR_SAFETY`. Android grants `signature|privileged` only to
 * the declaring certificate or a privileged system app
 * (https://developer.android.com/guide/topics/manifest/permission-element#plevel),
 * so this app records the failure and does not treat it as an unresolved fault.
 *
 * Tire-pressure registration is the same private permission, so low pressure is
 * read once per scan. `setDoorLock` documents true as locked and false as
 * unlocked, so the Boolean is stored that way. Hood and trunk latch release is
 * interpreted as an open closure in the check rules, not as a cabin door lock.
 * A null property value is "확인할 수 없음", not off, closed, or empty.
 */
class PleosSignalSource(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val vehicle = Vehicle(context.applicationContext)
    val snapshot = MutableStateFlow(
        VehicleSnapshot(ready = false, link = SdkLink.NOT_CONNECTED),
    )
    val link = MutableStateFlow(SdkLink.NOT_CONNECTED)
    val status = MutableStateFlow("차량 서비스 연결 전")
    private val scanMutex = Mutex()

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
            publish(
                VehicleSnapshot(
                    ready = true,
                    link = SdkLink.NOT_CONNECTED,
                    unread = listOf(UnreadSignal(SignalSource.VEHICLE_SERVICE, UnreadCause.DISCONNECTED)),
                    readErrors = listOf("차량 서비스: ${error.shortMessage()}"),
                ),
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
        scanMutex.withLock {
            if (released) return
            stopListeners()
            val next = try {
                scan()
            } catch (error: Exception) {
                VehicleSnapshot(
                    ready = true,
                    link = SdkLink.DISCONNECTED,
                    unread = listOf(UnreadSignal(SignalSource.VEHICLE_SERVICE, UnreadCause.DISCONNECTED)),
                    readErrors = listOf("차량 서비스: ${error.shortMessage()}"),
                )
            }
            if (released) return
            publish(next)
            if (next.link == SdkLink.CONNECTED) startListeners()
            status.value = when (next.link) {
                SdkLink.CONNECTED -> if (next.unread.isEmpty()) "차량 신호 수신" else "일부 신호 실패 ${next.unread.size}건"
                SdkLink.NOT_CONNECTED,
                SdkLink.DISCONNECTED,
                -> "차량 서비스에 연결되지 않음"
            }
        }
    }

    private fun publish(next: VehicleSnapshot) {
        snapshot.value = next
        link.value = next.link
    }

    private suspend fun scan(): VehicleSnapshot {
        val bag = ScanBag()
        val windows = isolate(bag, SignalSource.WINDOWS, emptyList()) { readWindows(bag) }
        val doors = isolate(bag, SignalSource.DOOR_POSITION, emptyList()) { readDoors(bag) }
        val cabin = isolate(bag, SignalSource.CABIN_LIGHT, null) { readCabin(bag) }
        val reading = isolate(bag, SignalSource.READING_LIGHT, emptyList()) { readReadingLights(bag) }
        val lowBeam = isolate(bag, SignalSource.LOW_BEAM, null) { readLowBeam(bag) }
        val tail = isolate(bag, SignalSource.TAIL_LAMP, null) { readTailLamp(bag) }
        val seats = isolate(bag, SignalSource.SEATS, emptyList()) { readSeats(bag) }
        val hvac = isolate(bag, SignalSource.HVAC, emptyList()) { readHvac(bag) }
        val tires = isolate(bag, SignalSource.TIRES, emptyList()) { readTires(bag) }
        val washer = isolate(bag, SignalSource.WASHER, null) { readWasher(bag) }
        val telltales = isolate(bag, SignalSource.TELLTALES, emptyList()) { readTelltales(bag) }
        val soc = isolate(bag, SignalSource.SOC, null) { readSoc(bag) }
        val fuel = isolate(bag, SignalSource.FUEL, null) { readFuel(bag) }
        val trip = isolate(bag, SignalSource.TRIP, null) { readTrip(bag) }
        val unread = collapseDisconnected(bag.unread)
        val serviceDown = unread.any { it.source == SignalSource.VEHICLE_SERVICE }
        val hvacOn = when {
            hvac.isEmpty() -> null
            hvac.any { it.on == true } -> true
            hvac.any { it.on == null } -> null
            else -> false
        }
        return VehicleSnapshot(
            ready = true,
            windows = windows,
            doors = doors,
            cabinLightOn = cabin,
            readingLights = reading,
            lowBeamOn = lowBeam,
            tailLampOn = tail,
            seats = seats,
            hvacOn = hvacOn,
            hvacZones = hvac,
            tires = tires,
            washerLow = washer,
            telltales = telltales,
            socPercent = soc,
            fuelPercent = fuel,
            tripDistance = trip,
            readErrors = bag.errors.toList(),
            notes = bag.notes.toList(),
            link = if (serviceDown) SdkLink.DISCONNECTED else SdkLink.CONNECTED,
            unread = unread,
        )
    }

    private suspend fun <T> isolate(
        bag: ScanBag,
        source: SignalSource,
        fallback: T,
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (error: Exception) {
            bag.fail(source, error, "${source.label()}: ${error.shortMessage()}")
            fallback
        }
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
            bag.fail(SignalSource.WINDOWS, error, "창문 capability: ${error.shortMessage()}")
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
                bag.fail(SignalSource.WINDOWS, error, "창문 ${domain.name}: ${error.shortMessage()}")
                null
            }
            if (position == null) bag.missing(SignalSource.WINDOWS)
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
                            bag.fail(SignalSource.DOOR_POSITION, error, "도어 위치 ${domain.name}: ${error.shortMessage()}")
                            null
                        }
                        if (position == null) bag.missing(SignalSource.DOOR_POSITION)
                        positions[domain] = position
                        if (position != null) doorPositionTracks += Tracked(area, domain)
                    }
                }
            },
            onFailure = { bag.fail(SignalSource.DOOR_POSITION, it, "도어 위치 capability: ${it.shortMessage()}") },
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
                            bag.fail(SignalSource.DOOR_LOCK, error, "도어 잠금 ${domain.name}: ${error.shortMessage()}")
                            null
                        }
                        if (locked == null) bag.missing(SignalSource.DOOR_LOCK)
                        locks[domain] = locked
                        if (locked != null) doorLockTracks += Tracked(area, domain)
                    }
                }
            },
            onFailure = { bag.fail(SignalSource.DOOR_LOCK, it, "도어 잠금 capability: ${it.shortMessage()}") },
        )
        val areas = (positions.keys + locks.keys).distinct()
        return areas.map { area -> DoorReading(area, positions[area], locks[area]) }
    }

    private suspend fun readCabin(bag: ScanBag): Boolean? {
        cabinSupported = false
        return readGlobalLight(
            bag = bag,
            source = SignalSource.CABIN_LIGHT,
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
            source = SignalSource.LOW_BEAM,
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
            source = SignalSource.TAIL_LAMP,
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
            bag.fail(SignalSource.READING_LIGHT, error, "독서등 capability: ${error.shortMessage()}")
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
                bag.fail(SignalSource.READING_LIGHT, error, "독서등 ${domain.name}: ${error.shortMessage()}")
                null
            }
            if (state == null) bag.missing(SignalSource.READING_LIGHT)
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
            bag.fail(SignalSource.SEATS, error, "좌석 점유 capability: ${error.shortMessage()}")
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
                bag.fail(SignalSource.SEATS, error, "좌석 ${domain.name}: ${error.shortMessage()}")
                null
            }
            if (occupied == null) bag.missing(SignalSource.SEATS)
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
            bag.fail(SignalSource.HVAC, error, "공조 capability: ${error.shortMessage()}")
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
                bag.fail(SignalSource.HVAC, error, "공조 ${domain.name}: ${error.shortMessage()}")
                null
            }
            if (power == null) bag.missing(SignalSource.HVAC)
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
            bag.fail(SignalSource.TIRES, error, "타이어 capability: ${error.shortMessage()}")
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
                bag.fail(SignalSource.TIRES, error, "타이어 ${domain.name}: ${error.shortMessage()}")
                null
            }
            if (value == null) bag.missing(SignalSource.TIRES)
            TireReading(domain, value)
        }
    }

    private suspend fun readWasher(bag: ScanBag): Boolean? {
        washerSupported = false
        val config = awaitSdk<CapabilityConfig> { complete, fail ->
            vehicle.getSafety().checkLowWasherFluidWarningStateCapabilities(complete, fail)
        }.getOrElse { error ->
            bag.fail(SignalSource.WASHER, error, "워셔액 capability: ${error.shortMessage()}")
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "워셔액 미지원 (${config.accessType.name})"
            return null
        }
        washerSupported = true
        val low = awaitSdk<Boolean?> { complete, fail ->
            vehicle.getSafety().hasLowWasherFluidWarning(complete, fail)
        }.getOrElse { error ->
            bag.fail(SignalSource.WASHER, error, "워셔액: ${error.shortMessage()}")
            return null
        }
        if (low == null) bag.missing(SignalSource.WASHER)
        return low
    }

    private suspend fun readTelltales(bag: ScanBag): List<TelltaleReading> {
        val info = awaitSdk { complete: (ai.pleos.playground.vehicle.api.model.telltale.Telltales) -> Unit, fail ->
            vehicle.getSafety().getTelltalesInfo(complete, fail)
        }.getOrElse { error ->
            // PRIVATE_CAR_SAFETY is signature|privileged. See the class KDoc.
            bag.fail(SignalSource.TELLTALES, error, "경고등: ${error.shortMessage()}")
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
            source = SignalSource.SOC,
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
            source = SignalSource.FUEL,
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
            bag.fail(SignalSource.TRIP, error, "트립 capability: ${error.shortMessage()}")
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "트립 거리 미지원 (${config.accessType.name})"
            return null
        }
        tripSupported = true
        val distance = awaitSdk<Float?> { complete, fail ->
            vehicle.getOdometer().getTripCurrentDrivingDistance(complete, fail)
        }.getOrElse { error ->
            bag.fail(SignalSource.TRIP, error, "트립: ${error.shortMessage()}")
            return null
        }
        if (distance == null) bag.missing(SignalSource.TRIP)
        return distance
    }

    private fun startListeners() {
        val windowApi = vehicle.getWindow()
        windowTracks.forEach { tracked ->
            val listener = object : WindowPositionListener {
                override fun onWindowPositionUpdated(position: Int?) = updateWindow(tracked.area, position)
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.WINDOWS, exception)
            }
            safeRegister(SignalSource.WINDOWS) {
                windowApi.registerWindowPosition(tracked.sdk, listener)
                unregisterActions += { windowApi.unregisterWindowPosition(tracked.sdk, listener) }
            }
        }
        val doorApi = vehicle.getDoor()
        doorPositionTracks.forEach { tracked ->
            val listener = object : DoorPositionListener {
                override fun onPositionUpdated(position: Int?) = updateDoorPosition(tracked.area, position)
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.DOOR_POSITION, exception)
            }
            safeRegister(SignalSource.DOOR_POSITION) {
                doorApi.registerDoorPosition(tracked.sdk, listener)
                unregisterActions += { doorApi.unregisterDoorPosition(tracked.sdk, listener) }
            }
        }
        doorLockTracks.forEach { tracked ->
            val listener = object : DoorLockStateListener {
                override fun onDoorLockStateUpdated(locked: Boolean?) = updateDoorLock(tracked.area, locked)
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.DOOR_LOCK, exception)
            }
            safeRegister(SignalSource.DOOR_LOCK) {
                doorApi.registerDoorLockState(tracked.sdk, listener)
                unregisterActions += { doorApi.unregisterDoorLockState(tracked.sdk, listener) }
            }
        }
        val lightApi = vehicle.getLight()
        if (cabinSupported) {
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    if (state == null) markMissing(SignalSource.CABIN_LIGHT)
                    snapshot.update { it.copy(cabinLightOn = state.asOn()) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.CABIN_LIGHT, exception)
            }
            safeRegister(SignalSource.CABIN_LIGHT) {
                lightApi.registerCabinLightState(listener)
                unregisterActions += { lightApi.unregisterCabinLightState(listener) }
            }
        }
        if (lowBeamSupported) {
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    if (state == null) markMissing(SignalSource.LOW_BEAM)
                    snapshot.update { it.copy(lowBeamOn = state.asOn()) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.LOW_BEAM, exception)
            }
            safeRegister(SignalSource.LOW_BEAM) {
                lightApi.registerLowBeamLightState(listener)
                unregisterActions += { lightApi.unregisterLowBeamLightState(listener) }
            }
        }
        if (tailSupported) {
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    if (state == null) markMissing(SignalSource.TAIL_LAMP)
                    snapshot.update { it.copy(tailLampOn = state.asOn()) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.TAIL_LAMP, exception)
            }
            safeRegister(SignalSource.TAIL_LAMP) {
                lightApi.registerTailLampLightState(listener)
                unregisterActions += { lightApi.unregisterTailLampLightState(listener) }
            }
        }
        readingTracks.forEach { tracked ->
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: ai.pleos.playground.vehicle.constant.control.LightState?) {
                    updateReading(tracked.area, state.asOn())
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.READING_LIGHT, exception)
            }
            safeRegister(SignalSource.READING_LIGHT) {
                lightApi.registerReadingLightState(tracked.sdk, listener)
                unregisterActions += { lightApi.unregisterReadingLightState(tracked.sdk, listener) }
            }
        }
        val seatApi = vehicle.getSeat()
        seatTracks.forEach { tracked ->
            val listener = object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(occupied: Boolean?) = updateSeat(tracked.area, occupied)
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.SEATS, exception)
            }
            safeRegister(SignalSource.SEATS) {
                seatApi.registerSeatOccupancy(tracked.sdk, listener)
                unregisterActions += { seatApi.unregisterSeatOccupancy(tracked.sdk, listener) }
            }
        }
        val hvacApi = vehicle.getHVAC()
        hvacTracks.forEach { tracked ->
            val listener = object : HvacPowerStateListener {
                override fun onHvacPowerStateUpdated(power: Boolean?) = updateHvac(tracked.area, power)
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.HVAC, exception)
            }
            safeRegister(SignalSource.HVAC) {
                hvacApi.registerHvacPowerState(tracked.sdk, listener)
                unregisterActions += { hvacApi.unregisterHvacPowerState(tracked.sdk, listener) }
            }
        }
        if (washerSupported) {
            val listener = object : LowWasherFluidWaringListener {
                override fun onLowWasherFluidWaringUpdated(low: Boolean?) {
                    if (low == null) markMissing(SignalSource.WASHER)
                    snapshot.update { it.copy(washerLow = low) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.WASHER, exception)
            }
            safeRegister(SignalSource.WASHER) {
                vehicle.getSafety().registerLowWasherFluidWaringState(listener)
                unregisterActions += { vehicle.getSafety().unregisterLowWasherFluidWaringState(listener) }
            }
        }
        if (socSupported) {
            val listener = object : EvBatteryStateOfChargeListener {
                override fun onEvBatteryStateOfChargeUpdated(soc: Int?) {
                    if (soc == null) markMissing(SignalSource.SOC)
                    snapshot.update { it.copy(socPercent = soc) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.SOC, exception)
            }
            safeRegister(SignalSource.SOC) {
                vehicle.getEvBattery().registerEvBatteryStateOfCharge(listener)
                unregisterActions += { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(listener) }
            }
        }
        if (fuelSupported) {
            val listener = object : FuelPercentListener {
                override fun onFuelPercentUpdated(fuel: Int?) {
                    if (fuel == null) markMissing(SignalSource.FUEL)
                    snapshot.update { it.copy(fuelPercent = fuel) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.FUEL, exception)
            }
            safeRegister(SignalSource.FUEL) {
                vehicle.getFuel().registerFuelPercent(listener)
                unregisterActions += { vehicle.getFuel().unregisterFuelPercent(listener) }
            }
        }
        if (tripSupported) {
            val listener = object : DistanceDrivenListener {
                override fun onDistanceDrivenUpdated(distance: Float?) {
                    if (distance == null) markMissing(SignalSource.TRIP)
                    snapshot.update { it.copy(tripDistance = distance) }
                }
                override fun onFailed(exception: Exception) = noteFailure(SignalSource.TRIP, exception)
            }
            safeRegister(SignalSource.TRIP) {
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

    private fun safeRegister(source: SignalSource, block: () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            noteFailure(source, error)
        }
    }

    private fun noteFailure(source: SignalSource, error: Throwable) {
        val cause = causeOfReadFailure(error is SecurityException, error.message)
        val recordedSource = if (cause == UnreadCause.DISCONNECTED) SignalSource.VEHICLE_SERVICE else source
        val message = "${source.label()}: ${error.shortMessage()}"
        snapshot.update { current ->
            val unread = if (current.unread.any { it.source == recordedSource }) {
                current.unread
            } else {
                current.unread + UnreadSignal(recordedSource, cause)
            }
            val errors = if (message in current.readErrors) current.readErrors else current.readErrors + message
            val linkNow = if (cause == UnreadCause.DISCONNECTED) SdkLink.DISCONNECTED else current.link
            current.copy(readErrors = errors, unread = unread, link = linkNow)
        }
        if (cause == UnreadCause.DISCONNECTED) {
            link.value = SdkLink.DISCONNECTED
        }
    }

    private fun markMissing(source: SignalSource) {
        snapshot.update { current ->
            if (current.unread.any { it.source == source }) {
                current
            } else {
                current.copy(unread = current.unread + UnreadSignal(source, UnreadCause.NULL_VALUE))
            }
        }
    }

    private fun updateWindow(area: VehicleArea, position: Int?) {
        if (position == null) {
            markMissing(SignalSource.WINDOWS)
            return
        }
        snapshot.update { current ->
            current.copy(
                windows = current.windows.map { window ->
                    if (window.area == area) window.copy(position = position, supported = true) else window
                },
            )
        }
    }

    private fun updateDoorPosition(area: VehicleArea, position: Int?) {
        if (position == null) markMissing(SignalSource.DOOR_POSITION)
        snapshot.update { current ->
            current.copy(
                doors = current.doors.map { door ->
                    if (door.area == area) door.copy(position = position) else door
                },
            )
        }
    }

    private fun updateDoorLock(area: VehicleArea, locked: Boolean?) {
        if (locked == null) markMissing(SignalSource.DOOR_LOCK)
        snapshot.update { current ->
            current.copy(
                doors = current.doors.map { door ->
                    if (door.area == area) door.copy(locked = locked) else door
                },
            )
        }
    }

    private fun updateReading(area: VehicleArea, on: Boolean?) {
        if (on == null) markMissing(SignalSource.READING_LIGHT)
        snapshot.update { current ->
            current.copy(
                readingLights = current.readingLights.map { light ->
                    if (light.area == area) light.copy(on = on) else light
                },
            )
        }
    }

    private fun updateSeat(area: VehicleArea, occupied: Boolean?) {
        if (occupied == null) markMissing(SignalSource.SEATS)
        snapshot.update { current ->
            current.copy(
                seats = current.seats.map { seat ->
                    if (seat.area == area) seat.copy(occupied = occupied) else seat
                },
            )
        }
    }

    private fun updateHvac(area: VehicleArea, power: Boolean?) {
        if (power == null) markMissing(SignalSource.HVAC)
        snapshot.update { current ->
            val zones = current.hvacZones.map { zone ->
                if (zone.area == area) zone.copy(on = power) else zone
            }
            val hvacOn = when {
                zones.isEmpty() -> null
                zones.any { it.on == true } -> true
                zones.any { it.on == null } -> null
                else -> false
            }
            current.copy(hvacZones = zones, hvacOn = hvacOn)
        }
    }

    private suspend fun readGlobalLight(
        bag: ScanBag,
        source: SignalSource,
        label: String,
        check: (onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        read: (onComplete: (ai.pleos.playground.vehicle.constant.control.LightState?) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onSupported: () -> Unit,
    ): Boolean? {
        val config = awaitSdk(call = check).getOrElse { error ->
            bag.fail(source, error, "$label capability: ${error.shortMessage()}")
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "$label 미지원 (${config.accessType.name})"
            return null
        }
        onSupported()
        val state = awaitSdk(call = read).getOrElse { error ->
            bag.fail(source, error, "$label: ${error.shortMessage()}")
            return null
        }
        if (state == null) {
            bag.missing(source)
            return null
        }
        return state.asOn()
    }

    private suspend fun readScalar(
        bag: ScanBag,
        source: SignalSource,
        label: String,
        check: (onComplete: (CapabilityConfig) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        read: (onComplete: (Int?) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onSupported: () -> Unit,
    ): Int? {
        val config = awaitSdk(call = check).getOrElse { error ->
            bag.fail(source, error, "$label capability: ${error.shortMessage()}")
            return null
        }
        if (!config.accessType.canRead()) {
            bag.notes += "$label 미지원 (${config.accessType.name})"
            return null
        }
        onSupported()
        val value = awaitSdk(call = read).getOrElse { error ->
            bag.fail(source, error, "$label: ${error.shortMessage()}")
            return null
        }
        if (value == null) bag.missing(source)
        return value
    }

    private class ScanBag {
        val errors = mutableListOf<String>()
        val notes = mutableListOf<String>()
        val unread = mutableListOf<UnreadSignal>()

        fun fail(source: SignalSource, error: Throwable, debug: String) {
            errors += debug
            val cause = causeOfReadFailure(error is SecurityException, error.message)
            val index = unread.indexOfFirst { it.source == source }
            if (index < 0) {
                unread += UnreadSignal(source, cause)
            } else if (unread[index].cause == UnreadCause.NULL_VALUE && cause != UnreadCause.NULL_VALUE) {
                unread[index] = UnreadSignal(source, cause)
            }
        }

        fun missing(source: SignalSource) {
            if (unread.any { it.source == source }) return
            unread += UnreadSignal(source, UnreadCause.NULL_VALUE)
            errors += "${source.label()}: null"
        }
    }

    private data class Tracked<T>(val sdk: T, val area: VehicleArea)
}
