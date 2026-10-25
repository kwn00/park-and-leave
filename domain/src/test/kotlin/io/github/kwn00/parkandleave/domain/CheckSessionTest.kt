package io.github.kwn00.parkandleave.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckSessionTest {
    @Test
    fun parkStartsAnActiveSession() {
        val session = checkSession(Gear.PARK, restricted = false, manual = false)
        assertEquals(SessionPhase.ACTIVE, session.phase)
        assertTrue(session.motionAllowed)
    }

    @Test
    fun driveStaysIdle() {
        val session = checkSession(Gear.DRIVE, restricted = false, manual = false)
        assertEquals(SessionPhase.IDLE, session.phase)
        assertFalse(session.motionAllowed)
    }

    @Test
    fun manualCheckDoesNotOpenWhileDriveIsKnown() {
        val session = checkSession(Gear.DRIVE, restricted = false, manual = true)
        assertEquals(SessionPhase.IDLE, session.phase)
    }

    @Test
    fun manualCheckOpensWhenGearIsUnknown() {
        val session = checkSession(Gear.UNKNOWN, restricted = false, manual = true)
        assertEquals(SessionPhase.ACTIVE, session.phase)
        assertFalse(session.motionAllowed)
    }

    @Test
    fun restrictionFreezesEvenInPark() {
        val session = checkSession(Gear.PARK, restricted = true, manual = true)
        assertEquals(SessionPhase.FROZEN, session.phase)
        assertFalse(session.motionAllowed)
    }

    @Test
    fun debugRowsShowRawGearSelection() {
        val rows = formatDebugRows(
            originLabel = "실측",
            gear = GearReading(rawValue = GearCodes.PARK, gear = Gear.PARK, error = null),
            snapshot = VehicleSnapshot(ready = true, cabinLightOn = true),
        )
        assertEquals("289408000", rows.first { it.label == "GEAR_SELECTION" }.value)
        assertEquals("4", rows.first { it.label == "gear.raw" }.value)
        assertEquals("PARK", rows.first { it.label == "gear" }.value)
        assertEquals("true", rows.first { it.label == "cabinLight" }.value)
    }

    @Test
    fun gearCodesRoundTripKnownPositions() {
        assertEquals(Gear.PARK, gearFromRaw(GearCodes.PARK))
        assertEquals(Gear.REVERSE, gearFromRaw(GearCodes.REVERSE))
        assertEquals(Gear.NEUTRAL, gearFromRaw(GearCodes.NEUTRAL))
        assertEquals(Gear.DRIVE, gearFromRaw(GearCodes.DRIVE))
        assertEquals(Gear.UNKNOWN, gearFromRaw(null))
        assertEquals(Gear.OTHER, gearFromRaw(16))
        assertEquals(GearCodes.PARK, Gear.PARK.rawCode())
    }
}
