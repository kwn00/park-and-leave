package io.github.kwn00.parkandleave.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryBoardTest {
    private val evaluator = ParkingCheckEvaluator()

    @Test
    fun fewCardsKeepTheSameSlotAsAFullRow() {
        assertEquals(4, summaryGridColumns(widthDp = 1682, cardCount = 1))
        assertEquals(4, summaryGridColumns(widthDp = 1682, cardCount = 2))
        assertEquals(4, summaryGridColumns(widthDp = 1682, cardCount = 5))
        assertEquals(3, summaryGridColumns(widthDp = 1200, cardCount = 2))
        assertEquals(1, summaryGridColumns(widthDp = 1682, cardCount = 0))
    }

    @Test
    fun wideWindowUsesFourColumnsWhenThereAreEnoughCards() {
        assertEquals(4, summaryGridColumns(widthDp = 1682, cardCount = 6))
        assertEquals(4, summaryGridColumns(widthDp = 1500, cardCount = 4))
    }

    @Test
    fun narrowerWindowsDropAColumnEvenForOneCard() {
        assertEquals(2, summaryGridColumns(widthDp = 1079, cardCount = 1))
        assertEquals(1, summaryGridColumns(widthDp = 719, cardCount = 3))
    }

    @Test
    fun narrowerWindowsDropAColumn() {
        assertEquals(3, summaryGridColumns(widthDp = 1499, cardCount = 6))
        assertEquals(3, summaryGridColumns(widthDp = 1080, cardCount = 6))
        assertEquals(2, summaryGridColumns(widthDp = 1079, cardCount = 6))
        assertEquals(2, summaryGridColumns(widthDp = 720, cardCount = 6))
        assertEquals(1, summaryGridColumns(widthDp = 719, cardCount = 6))
    }

    @Test
    fun warningMapsToCriticalAndCautionToWarn() {
        assertEquals(CardTone.CRITICAL, Severity.WARNING.toCardTone())
        assertEquals(CardTone.WARN, Severity.CAUTION.toCardTone())
    }

    @Test
    fun activeCardsKeepSeverityOrderAndAreNotResolved() {
        val report = evaluator.evaluate(
            FakePresets.allClear().copy(
                doors = FakePresets.allClear().doors + DoorReading(VehicleArea.TRUNK, position = 1, locked = true),
                hvacOn = true,
            ),
        )
        val cards = summaryCards(report, emptyList())
        assertEquals(CardTone.CRITICAL, cards.first().tone)
        assertTrue(cards.any { it.tone == CardTone.WARN })
        assertTrue(cards.none { it.resolved })
        assertEquals(report.items.map { it.id }, cards.map { it.id })
        assertEquals("트렁크를 닫아 주세요", cards.first().guidance)
        assertEquals(HeadlineTone.CRITICAL, headlineTone(report.allClear, cards))
    }

    @Test
    fun clearedIssueStaysVisibleAsResolvedInfo() {
        val before = evaluator.evaluate(FakePresets.windowAndReading())
        val seen = rememberSeen(emptyList(), before.items)
        val readingOnly = FakePresets.allClear().copy(
            readingLights = listOf(ReadingLightReading(VehicleArea.ROW_2_RIGHT, on = true)),
        )
        val after = evaluator.evaluate(readingOnly)
        val cards = summaryCards(after, seen)

        assertEquals(SignalId.READING_LIGHT, cards.first().id)
        assertEquals(CardTone.WARN, cards.first().tone)
        assertFalse(cards.first().resolved)

        val resolved = cards.single { it.id == SignalId.WINDOW }
        assertEquals(CardTone.INFO, resolved.tone)
        assertEquals(RESOLVED_DETAIL, resolved.detail)
        assertTrue(resolved.resolved)
        assertEquals("창문 열림", resolved.title)
        assertEquals("", resolved.guidance)
        assertEquals("독서등을 꺼 주세요", cards.first().guidance)
        assertEquals(HeadlineTone.WARN, headlineTone(after.allClear, cards))
    }

    @Test
    fun allClearKeepsResolvedCardsForTheParkedSession() {
        val before = evaluator.evaluate(FakePresets.windowAndReading())
        val seen = rememberSeen(emptyList(), before.items)
        val clear = evaluator.evaluate(FakePresets.allClear())
        val cards = summaryCards(clear, seen)

        assertTrue(clear.allClear)
        assertEquals(listOf(SignalId.WINDOW, SignalId.READING_LIGHT), cards.map { it.id })
        assertTrue(cards.all { it.resolved && it.tone == CardTone.INFO && it.detail == RESOLVED_DETAIL })
        assertEquals(HeadlineTone.OK, headlineTone(clear.allClear, cards))
    }

    @Test
    fun freshAllClearHasNoCards() {
        val clear = evaluator.evaluate(FakePresets.allClear())
        assertTrue(summaryCards(clear, emptyList()).isEmpty())
    }

    @Test
    fun partialReadDoesNotMarkSeenIssuesResolved() {
        val before = evaluator.evaluate(FakePresets.windowAndReading())
        val seen = rememberSeen(emptyList(), before.items)
        val failed = evaluator.evaluate(
            FakePresets.allClear().copy(readErrors = listOf("window read failed")),
        )
        assertFalse(failed.allClear)
        assertTrue(summaryCards(failed, seen).isEmpty())
        assertEquals(HeadlineTone.NEUTRAL, headlineTone(failed.allClear, emptyList()))
    }

    @Test
    fun rememberKeepsResolvedIdsAndRefreshesActiveTitles() {
        val first = SeenIssue(SignalId.WINDOW, "창문 열림", Severity.CAUTION)
        val updated = CheckItem(
            id = SignalId.WINDOW,
            severity = Severity.CAUTION,
            title = "창문 열림",
            detail = "10%",
            guidance = "창문을 닫으세요",
        )
        val again = rememberSeen(listOf(first), listOf(updated))
        assertEquals(listOf(first), again)

        val renamed = updated.copy(title = "창문")
        val refreshed = rememberSeen(again, listOf(renamed))
        assertEquals("창문", refreshed.single().title)
        assertEquals(Severity.CAUTION, refreshed.single().severity)

        val dropped = rememberSeen(refreshed, emptyList())
        assertEquals(refreshed, dropped)
    }

    @Test
    fun returningIssueIsActiveAgain() {
        val seen = listOf(SeenIssue(SignalId.WINDOW, "창문 열림", Severity.CAUTION))
        val report = evaluator.evaluate(FakePresets.windowAndReading())
        val cards = summaryCards(report, seen)
        assertFalse(cards.single { it.id == SignalId.WINDOW }.resolved)
        assertTrue(cards.none { it.detail == RESOLVED_DETAIL && it.id == SignalId.WINDOW })
    }

    @Test
    fun leavingParkClearsMemoryAndScanningKeepsIt() {
        val seen = listOf(SeenIssue(SignalId.HVAC, "공조", Severity.CAUTION))
        val report = evaluator.evaluate(FakePresets.allClear())
        assertTrue(seenForSession(SessionPhase.IDLE, seen, report).isEmpty())
        assertTrue(seenForSession(SessionPhase.FROZEN, seen, report).isEmpty())
        assertEquals(seen, seenForSession(SessionPhase.ACTIVE, seen, report = null))
        assertEquals(seen, seenForSession(SessionPhase.ACTIVE, seen, report))
    }

    @Test
    fun clearSnapshotListsSignalsThatWereReadAndFine() {
        assertEquals(
            listOf(
                "창문",
                "도어",
                "도어 잠금",
                "실내등",
                "독서등",
                "외부등",
                "공조",
                "타이어",
                "워셔액",
                "배터리",
                "연료",
            ),
            confirmedLabels(FakePresets.allClear(), emptySet()),
        )
    }

    @Test
    fun openWindowAndOpenTrunkDropThoseChips() {
        val windows = confirmedLabels(FakePresets.windowAndReading(), emptySet())
        assertFalse(windows.contains("창문"))
        assertFalse(windows.contains("독서등"))
        assertTrue(windows.contains("실내등"))

        val trunk = FakePresets.trunkOpen()
        val openIds = evaluator.evaluate(trunk).items.map { it.id }.toSet()
        val labels = confirmedLabels(trunk, openIds)
        assertFalse(labels.contains("트렁크"))
        assertFalse(labels.contains("경고등"))
        assertTrue(labels.contains("창문"))
    }

    @Test
    fun unreadOrMissingValueIsNotAConfirmedChip() {
        val cabinUnread = FakePresets.allClear().copy(
            unread = listOf(UnreadSignal(SignalSource.CABIN_LIGHT, UnreadCause.NULL_VALUE)),
        )
        assertFalse(confirmedLabels(cabinUnread, emptySet()).contains("실내등"))

        val noSoc = FakePresets.allClear().copy(socPercent = null)
        val noSocLabels = confirmedLabels(noSoc, emptySet())
        assertFalse(noSocLabels.contains("배터리"))
        assertTrue(noSocLabels.contains("연료"))

        val evWithoutFuel = FakePresets.allClear().copy(
            fuelPercent = null,
            unread = listOf(UnreadSignal(SignalSource.FUEL, UnreadCause.NULL_VALUE)),
        )
        assertFalse(confirmedLabels(evWithoutFuel, emptySet()).contains("연료"))
        assertTrue(confirmedLabels(evWithoutFuel, emptySet()).contains("배터리"))
    }

    @Test
    fun cardIdsDoNotInventAFineChip() {
        val openHood = FakePresets.allClear().copy(
            doors = FakePresets.allClear().doors + DoorReading(
                VehicleArea.HOOD,
                position = 0,
                locked = false,
            ),
        )
        assertFalse(confirmedLabels(openHood, emptySet()).contains("보닛"))
        assertFalse(confirmedLabels(FakePresets.allClear(), setOf(SignalId.WINDOW)).contains("창문"))
        assertTrue(
            confirmedLabels(FakePresets.allClear().copy(link = SdkLink.DISCONNECTED), emptySet()).isEmpty(),
        )
    }
}
