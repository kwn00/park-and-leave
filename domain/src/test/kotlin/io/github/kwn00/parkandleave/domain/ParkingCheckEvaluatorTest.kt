package io.github.kwn00.parkandleave.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingCheckEvaluatorTest {
    private val evaluator = ParkingCheckEvaluator()

    @Test
    fun allClearPresetHasNoItems() {
        val report = evaluator.evaluate(FakePresets.allClear())

        assertTrue(report.allClear)
        assertTrue(report.items.isEmpty())
        assertEquals("모두 정상", report.headline)
        assertEquals("내려도 됩니다", report.clearanceLine)
        assertNull(report.speechLine)
        assertNull(report.tomorrowLine)
    }

    @Test
    fun openWindowAndReadingLightAreTwoCautions() {
        val report = evaluator.evaluate(FakePresets.windowAndReading())

        assertFalse(report.allClear)
        assertEquals("확인 2건", report.headline)
        assertEquals(listOf(SignalId.WINDOW, SignalId.READING_LIGHT), report.items.map { it.id })
        assertTrue(report.items.all { it.severity == Severity.CAUTION })
        val window = report.items.first { it.id == SignalId.WINDOW }
        assertTrue(window.detail.contains("2열 오른쪽"))
        assertTrue(window.detail.contains("30%"))
        assertNull(report.clearanceLine)
        assertTrue(report.speechLine!!.contains("2"))
    }

    @Test
    fun unsupportedWindowIsHidden() {
        val snapshot = VehicleSnapshot(
            windows = listOf(
                WindowReading(VehicleArea.ROW_2_RIGHT, position = 8, maxPosition = 10, supported = false),
            ),
        )

        val report = evaluator.evaluate(snapshot)

        assertTrue(report.items.none { it.id == SignalId.WINDOW })
        assertTrue(report.allClear)
    }

    @Test
    fun severalOpenWindowsStayOneItem() {
        val snapshot = FakePresets.allClear().copy(
            windows = listOf(
                WindowReading(VehicleArea.ROW_1_LEFT, position = 1, maxPosition = 10, supported = true),
                WindowReading(VehicleArea.ROW_2_RIGHT, position = 3, maxPosition = 10, supported = true),
            ),
        )

        val report = evaluator.evaluate(snapshot)

        assertEquals(1, report.items.size)
        assertEquals(SignalId.WINDOW, report.items.single().id)
        assertTrue(report.items.single().detail.contains("1열 왼쪽"))
        assertTrue(report.items.single().detail.contains("2열 오른쪽"))
    }

    @Test
    fun closedWindowDoesNotFlag() {
        val report = evaluator.evaluate(FakePresets.allClear())
        assertTrue(report.items.none { it.id == SignalId.WINDOW })
    }

    @Test
    fun rearOccupancyAndTrunkAreWarningsAndDoNotDoubleCountTelltale() {
        val report = evaluator.evaluate(FakePresets.rearSeatAndTrunk())

        assertEquals(listOf(SignalId.REAR_OCCUPANCY, SignalId.TRUNK), report.items.map { it.id })
        assertTrue(report.items.all { it.severity == Severity.WARNING })
        assertTrue(report.items.first { it.id == SignalId.REAR_OCCUPANCY }.detail.contains("2열 왼쪽"))
        assertTrue(report.items.first { it.id == SignalId.TRUNK }.title.contains("트렁크"))
    }

    @Test
    fun frontSeatOccupancyIsIgnored() {
        val snapshot = FakePresets.allClear().copy(
            seats = listOf(SeatReading(VehicleArea.ROW_1_LEFT, occupied = true)),
        )

        assertTrue(evaluator.evaluate(snapshot).items.none { it.id == SignalId.REAR_OCCUPANCY })
    }

    @Test
    fun unlockedDoorIsCautionAndOpenDoorIsWarning() {
        val unlocked = evaluator.evaluate(FakePresets.doorsUnlocked())
        assertEquals(SignalId.DOOR_UNLOCKED, unlocked.items.single().id)
        assertEquals(Severity.CAUTION, unlocked.items.single().severity)

        val open = evaluator.evaluate(
            FakePresets.allClear().copy(
                doors = listOf(DoorReading(VehicleArea.ROW_1_LEFT, position = 1, locked = false)),
            ),
        )
        assertEquals(listOf(SignalId.DOOR_OPEN, SignalId.DOOR_UNLOCKED), open.items.map { it.id })
        assertEquals(Severity.WARNING, open.items.first().severity)
    }

    @Test
    fun lockedClosedDoorIsClear() {
        val report = evaluator.evaluate(FakePresets.allClear())
        assertTrue(report.items.none { it.id == SignalId.DOOR_UNLOCKED || it.id == SignalId.DOOR_OPEN })
    }

    @Test
    fun cabinExteriorAndHvacFlags() {
        val snapshot = FakePresets.allClear().copy(
            cabinLightOn = true,
            lowBeamOn = true,
            tailLampOn = true,
            hvacOn = true,
        )

        val ids = evaluator.evaluate(snapshot).items.map { it.id }
        assertEquals(
            listOf(SignalId.CABIN_LIGHT, SignalId.EXTERIOR_LIGHT, SignalId.HVAC),
            ids,
        )
    }

    @Test
    fun tireFlagTreatsZeroAndPressureLikeValuesAsNotLow() {
        val zero = evaluator.evaluate(
            VehicleSnapshot(tires = listOf(TireReading(VehicleArea.WHEEL_LEFT_REAR, lowPressure = 0f))),
        )
        val pressure = evaluator.evaluate(
            VehicleSnapshot(tires = listOf(TireReading(VehicleArea.WHEEL_LEFT_REAR, lowPressure = 240f))),
        )
        val flagged = evaluator.evaluate(
            VehicleSnapshot(tires = listOf(TireReading(VehicleArea.WHEEL_LEFT_REAR, lowPressure = 1f))),
        )

        assertTrue(zero.items.none { it.id == SignalId.TIRE })
        assertTrue(pressure.items.none { it.id == SignalId.TIRE })
        assertEquals(SignalId.TIRE, flagged.items.single().id)
        assertEquals(Severity.WARNING, flagged.items.single().severity)
        assertTrue(flagged.tomorrowLine!!.contains("타이어"))
    }

    @Test
    fun washerLowIsCaution() {
        val report = evaluator.evaluate(FakePresets.allClear().copy(washerLow = true))
        assertEquals(SignalId.WASHER, report.items.single().id)
        assertEquals(Severity.CAUTION, report.items.single().severity)
    }

    @Test
    fun lowSocWithTripAddsItemAndTomorrowLine() {
        val report = evaluator.evaluate(FakePresets.lowSoc())

        assertEquals(SignalId.LOW_SOC, report.items.single().id)
        assertTrue(report.items.single().detail.contains("18"))
        assertEquals("내일 같은 거리라면 충전 권장", report.tomorrowLine)
        assertFalse(report.allClear)
    }

    @Test
    fun socAboveItemThresholdCanStillShowTomorrowLineWithoutCounting() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(socPercent = 25, tripDistance = 10f),
        )

        assertTrue(report.allClear)
        assertTrue(report.items.isEmpty())
        assertEquals("내일 같은 거리라면 충전 권장", report.tomorrowLine)
    }

    @Test
    fun lowSocWithoutTripUsesEnergyLine() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(socPercent = 18, tripDistance = null),
        )
        assertEquals("잔여 에너지가 낮습니다", report.tomorrowLine)
    }

    @Test
    fun lowFuelIsCaution() {
        val report = evaluator.evaluate(FakePresets.allClear().copy(fuelPercent = 10, socPercent = 80))
        assertEquals(SignalId.LOW_FUEL, report.items.single().id)
        assertEquals(Severity.CAUTION, report.items.single().severity)
        assertEquals("내일 출발 전 연료를 확인하세요", report.tomorrowLine)
    }

    @Test
    fun telltaleFaultsMapWithoutDuplicatingDirectSignals() {
        val both = evaluator.evaluate(
            VehicleSnapshot(
                tires = listOf(TireReading(VehicleArea.WHEEL_RIGHT_FRONT, lowPressure = 1f)),
                telltales = listOf(
                    TelltaleReading(TelltaleKind.TPMS, active = true, rawName = "TPMS"),
                    TelltaleReading(TelltaleKind.MASTER_WARNING, active = true, rawName = "MASTER_WARNING"),
                    TelltaleReading(TelltaleKind.OTHER, active = true, rawName = "DRIVE_MODE_ECO"),
                ),
            ),
        )
        assertEquals(listOf(SignalId.TIRE, SignalId.TELLTALE), both.items.map { it.id })
        assertTrue(both.items.first { it.id == SignalId.TELLTALE }.detail.contains("MASTER_WARNING"))
        assertFalse(both.items.first { it.id == SignalId.TELLTALE }.detail.contains("DRIVE_MODE_ECO"))

        val telltaleOnly = evaluator.evaluate(
            VehicleSnapshot(
                telltales = listOf(
                    TelltaleReading(TelltaleKind.TPMS, active = true, rawName = "TPMS"),
                    TelltaleReading(TelltaleKind.DOOR_OPEN, active = false, rawName = "DOOR_OPEN"),
                    TelltaleReading(TelltaleKind.LOW_FUEL, active = true, rawName = "LOW_FUEL"),
                ),
            ),
        )
        assertEquals(listOf(SignalId.TIRE, SignalId.LOW_FUEL), telltaleOnly.items.map { it.id })
    }

    @Test
    fun inactiveParkingBrakeTelltaleIsIgnored() {
        val report = evaluator.evaluate(
            VehicleSnapshot(
                telltales = listOf(
                    TelltaleReading(TelltaleKind.OTHER, active = true, rawName = "PARKING_BRAKE"),
                ),
            ),
        )
        assertTrue(report.allClear)
    }

    @Test
    fun telltalePermissionDoesNotBlockAllClear() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                unread = listOf(UnreadSignal(SignalSource.TELLTALES, UnreadCause.PERMISSION)),
                readErrors = listOf("경고등: getTelltalesInfo need Permission"),
            ),
        )

        assertTrue(report.allClear)
        assertEquals("모두 정상", report.headline)
        assertEquals("내려도 됩니다", report.clearanceLine)
        assertEquals(listOf("경고등: 이 차량에서 앱이 읽을 수 없음"), report.uncheckedLines)
    }

    @Test
    fun telltalePermissionDoesNotHideAnotherSource() {
        val report = evaluator.evaluate(
            FakePresets.windowAndReading().copy(
                unread = listOf(UnreadSignal(SignalSource.TELLTALES, UnreadCause.PERMISSION)),
            ),
        )

        assertFalse(report.allClear)
        assertEquals("확인 2건", report.headline)
        assertTrue(report.items.any { it.id == SignalId.WINDOW })
        assertTrue(report.items.any { it.id == SignalId.READING_LIGHT })
        assertEquals(listOf("경고등: 이 차량에서 앱이 읽을 수 없음"), report.uncheckedLines)
    }

    @Test
    fun nullPropertyIsUncheckedRatherThanNormal() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                cabinLightOn = null,
                unread = listOf(UnreadSignal(SignalSource.CABIN_LIGHT, UnreadCause.NULL_VALUE)),
            ),
        )

        assertFalse(report.allClear)
        assertEquals("확인된 이상 없음", report.headline)
        assertNull(report.clearanceLine)
        assertTrue(report.items.none { it.id == SignalId.CABIN_LIGHT })
        assertEquals(listOf("실내등: 확인할 수 없음"), report.uncheckedLines)
    }

    @Test
    fun absentFieldWithoutUnreadMarkerStaysOutOfTheReport() {
        val report = evaluator.evaluate(
            VehicleSnapshot(cabinLightOn = null, ready = true),
        )

        assertTrue(report.allClear)
        assertTrue(report.uncheckedLines.isEmpty())
    }

    @Test
    fun telltaleTimeoutStillBlocksClear() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                unread = listOf(UnreadSignal(SignalSource.TELLTALES, UnreadCause.FAILED)),
            ),
        )

        assertFalse(report.allClear)
        assertEquals("확인된 이상 없음", report.headline)
        assertEquals(listOf("경고등: 확인할 수 없음"), report.uncheckedLines)
    }

    @Test
    fun disconnectedLinkIsOneUncheckedLine() {
        val report = evaluator.evaluate(
            VehicleSnapshot(ready = true, link = SdkLink.DISCONNECTED),
        )

        assertFalse(report.allClear)
        assertEquals(listOf("차량 서비스: 연결되지 않음"), report.uncheckedLines)
    }

    @Test
    fun readErrorsBlockAllClear() {
        val report = evaluator.evaluate(
            VehicleSnapshot(readErrors = listOf("창문: timeout")),
        )
        assertFalse(report.allClear)
        assertEquals("확인된 이상 없음", report.headline)
        assertNull(report.clearanceLine)
        assertTrue(report.items.isEmpty())
    }

    @Test
    fun resolvingAWindowDropsThatItem() {
        val open = FakePresets.windowAndReading()
        val closed = open.copy(
            windows = open.windows.map { it.copy(position = 0) },
        )

        assertEquals(2, evaluator.evaluate(open).items.size)
        assertEquals(listOf(SignalId.READING_LIGHT), evaluator.evaluate(closed).items.map { it.id })
    }
}
