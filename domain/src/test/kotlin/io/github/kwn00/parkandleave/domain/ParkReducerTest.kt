package io.github.kwn00.parkandleave.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkReducerTest {
    @Test
    fun enteringParkRereadsAndStayingParkedDoesNot() {
        val parked = ParkFrame(
            gear = Gear.PARK,
            restricted = false,
            manual = false,
            link = SdkLink.CONNECTED,
            snapshot = FakePresets.allClear(),
        )

        val fromDrive = reducePark(ParkCursor(Gear.DRIVE, SdkLink.CONNECTED), emptyList(), parked)
        assertTrue(fromDrive.reread)
        assertEquals(SessionPhase.ACTIVE, fromDrive.session.phase)
        assertTrue(fromDrive.report!!.allClear)
        assertEquals("모두 정상", fromDrive.report.headline)

        val stillParked = reducePark(ParkCursor(Gear.PARK, SdkLink.CONNECTED), fromDrive.seen, parked)
        assertFalse(stillParked.reread)

        val leave = reducePark(
            ParkCursor(Gear.PARK, SdkLink.CONNECTED),
            stillParked.seen,
            parked.copy(gear = Gear.DRIVE),
        )
        assertFalse(leave.reread)
        assertEquals(SessionPhase.IDLE, leave.session.phase)
        assertNull(leave.report)
        assertTrue(leave.seen.isEmpty())

        val again = reducePark(ParkCursor(Gear.DRIVE, SdkLink.CONNECTED), leave.seen, parked)
        assertTrue(again.reread)
        assertTrue(again.report!!.allClear)
    }

    @Test
    fun reconnectingWhileParkedRereads() {
        val parked = ParkFrame(
            gear = Gear.PARK,
            restricted = false,
            manual = false,
            link = SdkLink.CONNECTED,
            snapshot = FakePresets.allClear(),
        )

        val reduction = reducePark(ParkCursor(Gear.PARK, SdkLink.DISCONNECTED), emptyList(), parked)
        assertTrue(reduction.reread)
    }

    @Test
    fun restrictionDoesNotOpenOrReread() {
        val frame = ParkFrame(
            gear = Gear.PARK,
            restricted = true,
            manual = false,
            link = SdkLink.CONNECTED,
            snapshot = FakePresets.allClear(),
        )

        val reduction = reducePark(ParkCursor(Gear.DRIVE, SdkLink.CONNECTED), emptyList(), frame)
        assertFalse(reduction.reread)
        assertEquals(SessionPhase.FROZEN, reduction.session.phase)
        assertNull(reduction.report)
    }

    @Test
    fun notConnectedServiceIsNotNormal() {
        val frame = ParkFrame(
            gear = Gear.PARK,
            restricted = false,
            manual = false,
            link = SdkLink.NOT_CONNECTED,
            snapshot = VehicleSnapshot(ready = false, link = SdkLink.NOT_CONNECTED),
        )

        val reduction = reducePark(ParkCursor(Gear.UNKNOWN, SdkLink.NOT_CONNECTED), emptyList(), frame)
        assertTrue(reduction.reread)
        val report = reduction.report!!
        assertFalse(report.allClear)
        assertEquals("확인된 이상 없음", report.headline)
        assertEquals(listOf("차량 서비스: 연결되지 않음"), report.uncheckedLines)
        assertTrue(reduction.cards.isEmpty())
    }

    @Test
    fun disconnectedServiceDoesNotClaimAllClear() {
        val frame = ParkFrame(
            gear = Gear.PARK,
            restricted = false,
            manual = false,
            link = SdkLink.DISCONNECTED,
            snapshot = FakePresets.allClear().copy(link = SdkLink.DISCONNECTED),
        )

        val reduction = reducePark(ParkCursor(Gear.PARK, SdkLink.CONNECTED), emptyList(), frame)
        assertFalse(reduction.reread)
        assertFalse(reduction.report!!.allClear)
        assertTrue(reduction.report.uncheckedLines.single().contains("차량 서비스"))
    }

    @Test
    fun nullPropertyIsUncheckedAndDoesNotCountAsNormal() {
        val frame = ParkFrame(
            gear = Gear.PARK,
            restricted = false,
            manual = false,
            link = SdkLink.CONNECTED,
            snapshot = FakePresets.allClear().copy(
                washerLow = null,
                unread = listOf(UnreadSignal(SignalSource.WASHER, UnreadCause.NULL_VALUE)),
            ),
        )

        val reduction = reducePark(ParkCursor(Gear.PARK, SdkLink.CONNECTED), emptyList(), frame)
        assertFalse(reduction.reread)
        assertFalse(reduction.report!!.allClear)
        assertEquals("확인된 이상 없음", reduction.report.headline)
        assertEquals(listOf("워셔액: 확인할 수 없음"), reduction.report.uncheckedLines)
        assertTrue(reduction.report.items.none { it.id == SignalId.WASHER })
    }

    @Test
    fun readableIssuesStayVisibleBesideATelltalePermissionFailure() {
        val frame = ParkFrame(
            gear = Gear.PARK,
            restricted = false,
            manual = false,
            link = SdkLink.CONNECTED,
            snapshot = FakePresets.allClear().copy(
                hvacOn = true,
                unread = listOf(UnreadSignal(SignalSource.TELLTALES, UnreadCause.PERMISSION)),
            ),
        )

        val reduction = reducePark(ParkCursor(Gear.DRIVE, SdkLink.CONNECTED), emptyList(), frame)
        assertTrue(reduction.reread)
        assertEquals(listOf(SignalId.HVAC), reduction.report!!.items.map { it.id })
        assertEquals("확인 1건", reduction.report.headline)
        assertEquals(listOf("경고등: 이 차량에서 앱이 읽을 수 없음"), reduction.report.uncheckedLines)
        assertEquals(SignalId.HVAC, reduction.cards.single().id)
    }

    @Test
    fun oneDisconnectedSourceDoesNotEraseAnotherFailure() {
        val collapsed = collapseDisconnected(
            listOf(
                UnreadSignal(SignalSource.WINDOWS, UnreadCause.PERMISSION),
                UnreadSignal(SignalSource.HVAC, UnreadCause.DISCONNECTED),
            ),
        )
        assertEquals(2, collapsed.size)

        val onlyDown = collapseDisconnected(
            listOf(
                UnreadSignal(SignalSource.WINDOWS, UnreadCause.DISCONNECTED),
                UnreadSignal(SignalSource.HVAC, UnreadCause.DISCONNECTED),
            ),
        )
        assertEquals(listOf(SignalSource.VEHICLE_SERVICE), onlyDown.map { it.source })
    }

    @Test
    fun failureTextMapsOntoCauses() {
        assertEquals(UnreadCause.PERMISSION, causeOfReadFailure(true, "getTelltalesInfo need Permission"))
        assertEquals(UnreadCause.PERMISSION, causeOfReadFailure(false, "getTelltalesInfo need Permission"))
        assertEquals(UnreadCause.DISCONNECTED, causeOfReadFailure(false, "Service connection failed"))
        assertEquals(UnreadCause.NOT_AVAILABLE, causeOfReadFailure(false, "Property not available"))
        assertEquals(UnreadCause.FAILED, causeOfReadFailure(false, "Timed out waiting for 2500 ms"))
        assertEquals(
            "경고등: 이 차량에서 앱이 읽을 수 없음",
            UnreadSignal(SignalSource.TELLTALES, UnreadCause.PERMISSION).line(),
        )
    }
}
