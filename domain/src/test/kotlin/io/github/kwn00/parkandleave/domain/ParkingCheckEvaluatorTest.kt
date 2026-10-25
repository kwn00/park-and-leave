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
    fun aaosOccupancyOneIsVacantAndSdkBooleanTrueMatchesIt() {
        assertEquals(SeatOccupancy.UNKNOWN, SeatOccupancy.fromAaos(0))
        assertEquals(SeatOccupancy.VACANT, SeatOccupancy.fromAaos(1))
        assertEquals(SeatOccupancy.OCCUPIED, SeatOccupancy.fromAaos(2))
        assertEquals(SeatOccupancy.UNKNOWN, SeatOccupancy.fromAaos(null))
        assertEquals(SeatOccupancy.VACANT, SeatOccupancy.fromSdkBoolean(true))
        assertEquals(SeatOccupancy.UNKNOWN, SeatOccupancy.fromSdkBoolean(false))
        assertEquals(SeatOccupancy.UNKNOWN, SeatOccupancy.fromSdkBoolean(null))
        assertEquals(VehicleArea.ROW_2_CENTER, vehicleAreaForSeatId(32))
        assertEquals(16, VehicleArea.ROW_2_LEFT.seatAreaId())
        assertEquals(null, VehicleArea.HOOD.seatAreaId())
    }

    @Test
    fun unknownRearSeatIsUncheckedRatherThanOccupied() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                seats = listOf(
                    SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.UNKNOWN),
                    SeatReading(VehicleArea.ROW_2_CENTER, SeatOccupancy.VACANT),
                    SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.VACANT),
                    SeatReading(VehicleArea.ROW_1_LEFT, SeatOccupancy.UNKNOWN),
                ),
                unread = listOf(UnreadSignal(SignalSource.SEATS, UnreadCause.NULL_VALUE)),
            ),
        )

        assertTrue(report.items.none { it.id == SignalId.REAR_OCCUPANCY })
        assertFalse(report.allClear)
        assertEquals("확인된 이상 없음", report.headline)
        assertEquals(listOf("뒷좌석: 확인할 수 없음"), report.uncheckedLines)
    }

    @Test
    fun vacantRearSeatsStayClearWhileTheDriverIsOccupied() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                seats = listOf(
                    SeatReading(VehicleArea.ROW_1_LEFT, SeatOccupancy.OCCUPIED),
                    SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.VACANT),
                    SeatReading(VehicleArea.ROW_2_CENTER, SeatOccupancy.VACANT),
                    SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.VACANT),
                ),
            ),
        )

        assertTrue(report.items.none { it.id == SignalId.REAR_OCCUPANCY })
        assertTrue(report.allClear)
    }

    @Test
    fun buckledRearBeltDoesNotReplaceOccupancy() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                seats = listOf(
                    SeatReading(
                        VehicleArea.ROW_2_RIGHT,
                        SeatOccupancy.VACANT,
                        beltBuckled = true,
                    ),
                ),
            ),
        )

        assertTrue(report.items.none { it.id == SignalId.REAR_OCCUPANCY })
        assertTrue(report.allClear)
    }

    @Test
    fun occupiedRearSeatDoesNotRequireAVacantDriver() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                seats = listOf(
                    SeatReading(VehicleArea.ROW_1_LEFT, SeatOccupancy.OCCUPIED),
                    SeatReading(VehicleArea.ROW_2_CENTER, SeatOccupancy.OCCUPIED),
                ),
            ),
        )

        val item = report.items.single()
        assertEquals(SignalId.REAR_OCCUPANCY, item.id)
        assertEquals("2열 가운데 점유", item.detail)
    }

    @Test
    fun frontSeatOccupancyIsIgnored() {
        val snapshot = FakePresets.allClear().copy(
            seats = listOf(SeatReading(VehicleArea.ROW_1_LEFT, SeatOccupancy.OCCUPIED)),
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
    fun releasedHoodLatchIsItsOwnOpenCard() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                doors = listOf(
                    DoorReading(VehicleArea.ROW_1_LEFT, position = 0, locked = true),
                    DoorReading(VehicleArea.HOOD, position = 0, locked = false),
                ),
            ),
        )

        val hood = report.items.single { it.id == SignalId.HOOD }
        assertEquals("보닛 열림", hood.title)
        assertEquals(Severity.WARNING, hood.severity)
        assertTrue(report.items.none { it.id == SignalId.DOOR_UNLOCKED || it.id == SignalId.DOOR_OPEN })
    }

    @Test
    fun hoodPositionOpenStaysOffTheDoorCard() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                doors = listOf(DoorReading(VehicleArea.HOOD, position = 1, locked = true)),
            ),
        )

        assertEquals(listOf(SignalId.HOOD), report.items.map { it.id })
        assertEquals("보닛 열림", report.items.single().title)
    }

    @Test
    fun closedLockedHoodIsClear() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                doors = listOf(DoorReading(VehicleArea.HOOD, position = 0, locked = true)),
            ),
        )

        assertTrue(report.items.none { it.id == SignalId.HOOD || it.id == SignalId.DOOR_UNLOCKED })
    }

    @Test
    fun releasedTrunkLatchIsTrunkOpenNotADoorLock() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                doors = listOf(
                    DoorReading(VehicleArea.ROW_1_LEFT, position = 0, locked = false),
                    DoorReading(VehicleArea.TRUNK, position = 0, locked = false),
                    DoorReading(VehicleArea.HOOD, position = 0, locked = false),
                ),
            ),
        )

        assertEquals(
            listOf(SignalId.TRUNK, SignalId.HOOD, SignalId.DOOR_UNLOCKED),
            report.items.map { it.id },
        )
        assertEquals("트렁크 열림", report.items.first { it.id == SignalId.TRUNK }.title)
        assertEquals("1열 왼쪽 잠금 해제", report.items.first { it.id == SignalId.DOOR_UNLOCKED }.detail)
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
    fun reportedSocDropsUnreadFuelFromTheCompactNote() {
        val withSoc = evaluator.evaluate(
            FakePresets.allClear().copy(
                socPercent = 70,
                fuelPercent = null,
                unread = listOf(
                    UnreadSignal(SignalSource.WASHER, UnreadCause.NULL_VALUE),
                    UnreadSignal(SignalSource.TELLTALES, UnreadCause.PERMISSION),
                    UnreadSignal(SignalSource.FUEL, UnreadCause.NULL_VALUE),
                    UnreadSignal(SignalSource.TRIP, UnreadCause.NULL_VALUE),
                ),
            ),
        )

        assertEquals(
            listOf("워셔액: 확인할 수 없음", "경고등: 이 차량에서 앱이 읽을 수 없음", "주행 거리: 확인할 수 없음"),
            withSoc.uncheckedLines,
        )
        assertEquals(
            "확인할 수 없는 항목: 워셔액, 경고등, 주행 거리",
            uncheckedNote(withSoc.uncheckedLines),
        )
        assertFalse(withSoc.allClear)
        assertEquals("확인된 이상 없음", withSoc.headline)

        val withoutSoc = evaluator.evaluate(
            FakePresets.allClear().copy(
                socPercent = null,
                fuelPercent = null,
                unread = listOf(UnreadSignal(SignalSource.FUEL, UnreadCause.NULL_VALUE)),
            ),
        )
        assertEquals(listOf("연료: 확인할 수 없음"), withoutSoc.uncheckedLines)
        assertFalse(withoutSoc.allClear)
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

    @Test
    fun manyPlacesCollapseToAShortDetailAndKeepGuidance() {
        val lights = FakePresets.allClear().copy(
            readingLights = listOf(
                ReadingLightReading(VehicleArea.ROW_1_LEFT, on = true),
                ReadingLightReading(VehicleArea.ROW_1_RIGHT, on = true),
                ReadingLightReading(VehicleArea.ROW_2_LEFT, on = true),
                ReadingLightReading(VehicleArea.ROW_2_RIGHT, on = true),
            ),
        )
        val reading = evaluator.evaluate(lights).items.single { it.id == SignalId.READING_LIGHT }
        assertEquals("1열·2열 양쪽", reading.detail)
        assertEquals("독서등을 꺼 주세요", reading.guidance)

        val seats = FakePresets.allClear().copy(
            seats = listOf(
                SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.OCCUPIED),
                SeatReading(VehicleArea.ROW_2_CENTER, SeatOccupancy.OCCUPIED),
                SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.OCCUPIED),
            ),
        )
        val rear = evaluator.evaluate(seats).items.single { it.id == SignalId.REAR_OCCUPANCY }
        assertEquals("2열 전체 점유", rear.detail)
        assertEquals("뒷좌석을 확인하세요", rear.guidance)
    }
}
