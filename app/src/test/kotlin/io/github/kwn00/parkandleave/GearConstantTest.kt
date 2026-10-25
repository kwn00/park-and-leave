package io.github.kwn00.parkandleave

import android.car.VehicleGear
import android.car.VehiclePropertyIds
import io.github.kwn00.parkandleave.domain.GearCodes
import org.junit.Assert.assertEquals
import org.junit.Test

class GearConstantTest {
    @Test
    fun domainCodesMatchAndroidCar() {
        assertEquals(VehiclePropertyIds.GEAR_SELECTION, GearCodes.PROPERTY_ID)
        assertEquals(VehicleGear.GEAR_PARK, GearCodes.PARK)
        assertEquals(VehicleGear.GEAR_REVERSE, GearCodes.REVERSE)
        assertEquals(VehicleGear.GEAR_NEUTRAL, GearCodes.NEUTRAL)
        assertEquals(VehicleGear.GEAR_DRIVE, GearCodes.DRIVE)
        assertEquals(VehicleGear.GEAR_UNKNOWN, GearCodes.UNKNOWN)
    }
}
