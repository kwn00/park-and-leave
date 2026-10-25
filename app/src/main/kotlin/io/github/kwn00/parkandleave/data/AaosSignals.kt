package io.github.kwn00.parkandleave.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import io.github.kwn00.parkandleave.domain.Gear
import io.github.kwn00.parkandleave.domain.GearReading
import io.github.kwn00.parkandleave.domain.gearFromRaw
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * AAOS gear and distraction restrictions. Gear is `VehiclePropertyIds.GEAR_SELECTION`
 * because Vehicle SDK 2.3.2 marks its own gear API deprecated.
 */
class AaosSignals(context: Context) {
    private val appContext = context.applicationContext
    val gear = MutableStateFlow(
        GearReading(rawValue = null, gear = Gear.UNKNOWN, error = "GEAR_SELECTION 대기"),
    )
    val restricted = MutableStateFlow(false)
    val status = MutableStateFlow("Car 서비스 연결 전")

    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var restrictions: CarUxRestrictionsManager? = null
    private var onSeatOccupancy: ((areaId: Int, raw: Int) -> Unit)? = null

    private val gearCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.propertyId != VehiclePropertyIds.GEAR_SELECTION) return
            publishGear(value.value)
        }

        override fun onErrorEvent(propId: Int, area: Int) {
            if (propId != VehiclePropertyIds.GEAR_SELECTION) return
            gear.value = gear.value.copy(error = "GEAR_SELECTION onErrorEvent area=$area")
        }
    }

    private val seatCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.propertyId != VehiclePropertyIds.SEAT_OCCUPANCY) return
            val raw = value.value as? Int ?: return
            onSeatOccupancy?.invoke(value.areaId, raw)
        }

        override fun onErrorEvent(propId: Int, area: Int) {
            if (propId != VehiclePropertyIds.SEAT_OCCUPANCY) return
            onSeatOccupancy?.invoke(area, SEAT_OCCUPANCY_UNKNOWN)
        }
    }

    private val restrictionListener =
        CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
            restricted.value = restrictions.isRequiresDistractionOptimization
        }

    fun start() {
        try {
            car = Car.createCar(
                appContext,
                null,
                CAR_CONNECT_TIMEOUT_MS,
                Car.CarServiceLifecycleListener { created, ready ->
                    if (!ready) {
                        status.value = "Car 서비스 준비 안 됨"
                        gear.value = GearReading(
                            rawValue = null,
                            gear = Gear.UNKNOWN,
                            error = "Car 서비스 준비 안 됨",
                        )
                        return@CarServiceLifecycleListener
                    }
                    connect(created)
                },
            )
        } catch (error: Exception) {
            val message = error.shortMessage()
            status.value = message
            gear.value = GearReading(rawValue = null, gear = Gear.UNKNOWN, error = message)
        }
    }

    fun setSeatOccupancyListener(listener: ((areaId: Int, raw: Int) -> Unit)?) {
        onSeatOccupancy = listener
    }

    /**
     * Raw `SEAT_OCCUPANCY` (`0x15400BB0`). 0 unknown, 1 vacant, 2 occupied.
     * Null when the platform read is unavailable; the SDK Boolean is the fallback.
     */
    fun readSeatOccupancy(areaId: Int): Int? {
        val manager = properties ?: return null
        return try {
            manager.getIntProperty(VehiclePropertyIds.SEAT_OCCUPANCY, areaId)
        } catch (_: Exception) {
            null
        }
    }

    fun refreshGear() {
        val manager = properties
        if (manager == null) {
            gear.value = gear.value.copy(error = gear.value.error ?: "CarPropertyManager 없음")
            return
        }
        try {
            publishGear(manager.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, GLOBAL_AREA))
        } catch (error: Exception) {
            gear.value = gear.value.copy(error = "getIntProperty: ${error.shortMessage()}")
        }
    }

    fun release() {
        try {
            @Suppress("DEPRECATION")
            properties?.unregisterCallback(gearCallback)
            @Suppress("DEPRECATION")
            properties?.unregisterCallback(seatCallback)
        } catch (_: Exception) {
        }
        try {
            restrictions?.unregisterListener()
        } catch (_: Exception) {
        }
        try {
            car?.disconnect()
        } catch (_: Exception) {
        }
        properties = null
        restrictions = null
        car = null
    }

    private fun connect(created: Car) {
        val manager = created.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        if (manager == null) {
            status.value = "CarPropertyManager 없음"
            gear.value = GearReading(rawValue = null, gear = Gear.UNKNOWN, error = "CarPropertyManager 없음")
        } else {
            properties = manager
            subscribeGear(manager)
            subscribeSeatOccupancy(manager)
        }
        val ux = created.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        if (ux == null) {
            status.value = status.value + " · UX 제한 매니저 없음"
        } else {
            restrictions = ux
            try {
                ux.registerListener(restrictionListener)
                restricted.value = ux.currentCarUxRestrictions?.isRequiresDistractionOptimization == true
            } catch (error: Exception) {
                status.value = "UX 제한 구독 실패: ${error.shortMessage()}"
            }
        }
    }

    private fun subscribeGear(manager: CarPropertyManager) {
        val registered = try {
            // API 34 exposes registerCallback. subscribePropertyEvents arrives later.
            @Suppress("DEPRECATION")
            manager.registerCallback(
                gearCallback,
                VehiclePropertyIds.GEAR_SELECTION,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
            )
        } catch (error: Exception) {
            gear.value = GearReading(
                rawValue = null,
                gear = Gear.UNKNOWN,
                error = "registerCallback: ${error.shortMessage()}",
            )
            status.value = "GEAR_SELECTION 구독 실패"
            return
        }
        if (!registered) {
            gear.value = GearReading(
                rawValue = null,
                gear = Gear.UNKNOWN,
                error = "GEAR_SELECTION 구독이 거부되었습니다. CAR_POWERTRAIN 권한을 확인하세요.",
            )
            status.value = "GEAR_SELECTION 구독 거부"
            return
        }
        status.value = "GEAR_SELECTION 구독 중"
        refreshGear()
    }

    private fun subscribeSeatOccupancy(manager: CarPropertyManager) {
        try {
            @Suppress("DEPRECATION")
            manager.registerCallback(
                seatCallback,
                VehiclePropertyIds.SEAT_OCCUPANCY,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
            )
        } catch (_: Exception) {
        }
    }

    private fun publishGear(value: Any?) {
        val raw = value as? Int
        gear.value = GearReading(
            propertyId = VehiclePropertyIds.GEAR_SELECTION,
            rawValue = raw,
            gear = gearFromRaw(raw),
            error = if (raw == null) "값 형식 ${value?.javaClass?.simpleName ?: "null"}" else null,
        )
        if (raw != null) {
            status.value = "GEAR_SELECTION=$raw"
        }
    }

    private companion object {
        const val CAR_CONNECT_TIMEOUT_MS = 2_000L
        const val GLOBAL_AREA = 0
        const val SEAT_OCCUPANCY_UNKNOWN = 0
    }
}
