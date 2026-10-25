package io.github.kwn00.parkandleave.ui

import io.github.kwn00.parkandleave.domain.DoorReading
import io.github.kwn00.parkandleave.domain.FakePresets
import io.github.kwn00.parkandleave.domain.ParkingCheckEvaluator
import io.github.kwn00.parkandleave.domain.ReadingLightReading
import io.github.kwn00.parkandleave.domain.SeatOccupancy
import io.github.kwn00.parkandleave.domain.SeatReading
import io.github.kwn00.parkandleave.domain.SignalSource
import io.github.kwn00.parkandleave.domain.TelltaleKind
import io.github.kwn00.parkandleave.domain.TelltaleReading
import io.github.kwn00.parkandleave.domain.TireReading
import io.github.kwn00.parkandleave.domain.UnreadCause
import io.github.kwn00.parkandleave.domain.UnreadSignal
import io.github.kwn00.parkandleave.domain.VehicleArea
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.WindowReading
import io.github.kwn00.parkandleave.domain.confirmedLabels
import io.github.kwn00.parkandleave.domain.rememberSeen
import io.github.kwn00.parkandleave.domain.summaryCards

/** Vehicle readings run through the real evaluator, so screens show the strings the app ships. */
internal object ParkedScenarios {
    private val cabinDoors = listOf(
        VehicleArea.ROW_1_LEFT,
        VehicleArea.ROW_1_RIGHT,
        VehicleArea.ROW_2_LEFT,
        VehicleArea.ROW_2_RIGHT,
    )

    /** Every signal read and fine, except the three the contest car cannot report. */
    val calm: VehicleSnapshot = FakePresets.allClear().copy(
        windows = cabinDoors.map { WindowReading(it, position = 0, maxPosition = 10, supported = true) },
        doors = cabinDoors.map { DoorReading(it, position = 0, locked = true) } + listOf(
            DoorReading(VehicleArea.HOOD, position = 0, locked = true),
            DoorReading(VehicleArea.TRUNK, position = 0, locked = true),
        ),
        readingLights = cabinDoors.map { ReadingLightReading(it, on = false) },
        seats = listOf(
            SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.VACANT),
            SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.VACANT),
        ),
        washerLow = null,
        tripDistance = null,
        unread = listOf(
            UnreadSignal(SignalSource.WASHER, UnreadCause.NOT_AVAILABLE),
            UnreadSignal(SignalSource.TELLTALES, UnreadCause.PERMISSION),
            UnreadSignal(SignalSource.TRIP, UnreadCause.FAILED),
        ),
    )

    val fiveIssues: VehicleSnapshot = calm.copy(
        seats = listOf(
            SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.OCCUPIED),
            SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.VACANT),
        ),
        doors = calm.doors.map { if (it.area == VehicleArea.HOOD) it.copy(position = 1) else it },
        windows = calm.windows.map { if (it.area == VehicleArea.ROW_2_RIGHT) it.copy(position = 3) else it },
        hvacOn = true,
        tailLampOn = true,
    )

    /** Low SOC also adds the tomorrow line, the tallest headline block. */
    val eightIssues: VehicleSnapshot = fiveIssues.copy(
        doors = fiveIssues.doors.map { if (it.area == VehicleArea.ROW_1_RIGHT) it.copy(locked = false) else it },
        readingLights = calm.readingLights.map { if (it.area.isFrontRow()) it.copy(on = true) else it },
        socPercent = 14,
    )

    /** Every evaluator item at once, each with one place: the bodies that should stay on one line. */
    val everyItemOnePlace: VehicleSnapshot = calm.copy(
        windows = listOf(WindowReading(VehicleArea.ROW_2_RIGHT, position = 10, maxPosition = 10, supported = true)),
        doors = listOf(
            DoorReading(VehicleArea.ROW_1_LEFT, position = 1, locked = true),
            DoorReading(VehicleArea.ROW_2_RIGHT, position = 0, locked = false),
            DoorReading(VehicleArea.HOOD, position = 1, locked = true),
            DoorReading(VehicleArea.TRUNK, position = 1, locked = true),
        ),
        cabinLightOn = true,
        readingLights = listOf(ReadingLightReading(VehicleArea.ROW_2_RIGHT, on = true)),
        lowBeamOn = true,
        tailLampOn = false,
        seats = listOf(SeatReading(VehicleArea.ROW_3_RIGHT, SeatOccupancy.OCCUPIED)),
        hvacOn = true,
        tires = listOf(TireReading(VehicleArea.WHEEL_RIGHT_FRONT, lowPressure = 1f)),
        washerLow = true,
        telltales = listOf(TelltaleReading(TelltaleKind.MASTER_WARNING, active = true, rawName = "MASTER_WARNING")),
        socPercent = 5,
        fuelPercent = 5,
        unread = emptyList(),
    )

    /** The longest bodies the evaluator builds: two places joined, or three collapsed to rows. */
    val everyItemManyPlaces: List<VehicleSnapshot> = listOf(
        everyItemOnePlace.copy(
            windows = listOf(
                WindowReading(VehicleArea.ROW_1_RIGHT, position = 10, maxPosition = 10, supported = true),
                WindowReading(VehicleArea.ROW_2_RIGHT, position = 3, maxPosition = 10, supported = true),
            ),
            doors = listOf(
                DoorReading(VehicleArea.ROW_1_RIGHT, position = 1, locked = false),
                DoorReading(VehicleArea.ROW_2_RIGHT, position = 1, locked = false),
                DoorReading(VehicleArea.TRUNK, position = 1, locked = true),
            ),
            readingLights = listOf(
                ReadingLightReading(VehicleArea.ROW_1_RIGHT, on = true),
                ReadingLightReading(VehicleArea.ROW_2_RIGHT, on = true),
            ),
            tailLampOn = true,
            seats = listOf(
                SeatReading(VehicleArea.ROW_2_RIGHT, SeatOccupancy.OCCUPIED),
                SeatReading(VehicleArea.ROW_3_RIGHT, SeatOccupancy.OCCUPIED),
            ),
            tires = listOf(
                TireReading(VehicleArea.WHEEL_RIGHT_FRONT, lowPressure = 1f),
                TireReading(VehicleArea.WHEEL_RIGHT_REAR, lowPressure = 1f),
            ),
            telltales = listOf(
                TelltaleReading(TelltaleKind.LOW_VOLT_BATTERY, active = true, rawName = "LOW_VOLT_BATTERY"),
                TelltaleReading(TelltaleKind.MASTER_WARNING, active = true, rawName = "MASTER_WARNING"),
            ),
        ),
        everyItemOnePlace.copy(
            windows = listOf(
                WindowReading(VehicleArea.ROW_1_LEFT, position = 10, maxPosition = 10, supported = true),
                WindowReading(VehicleArea.ROW_1_RIGHT, position = 10, maxPosition = 10, supported = true),
                WindowReading(VehicleArea.ROW_2_RIGHT, position = 3, maxPosition = 10, supported = true),
            ),
            doors = listOf(
                DoorReading(VehicleArea.ROW_1_LEFT, position = 1, locked = false),
                DoorReading(VehicleArea.ROW_1_RIGHT, position = 1, locked = false),
                DoorReading(VehicleArea.ROW_2_CENTER, position = 1, locked = false),
                DoorReading(VehicleArea.ROW_2_RIGHT, position = 1, locked = false),
            ),
            readingLights = listOf(
                ReadingLightReading(VehicleArea.ROW_1_LEFT, on = true),
                ReadingLightReading(VehicleArea.ROW_1_RIGHT, on = true),
                ReadingLightReading(VehicleArea.ROW_2_CENTER, on = true),
                ReadingLightReading(VehicleArea.ROW_2_RIGHT, on = true),
            ),
            seats = listOf(
                SeatReading(VehicleArea.ROW_2_LEFT, SeatOccupancy.OCCUPIED),
                SeatReading(VehicleArea.ROW_2_CENTER, SeatOccupancy.OCCUPIED),
                SeatReading(VehicleArea.ROW_3_RIGHT, SeatOccupancy.OCCUPIED),
            ),
            tires = listOf(
                TireReading(VehicleArea.WHEEL_LEFT_FRONT, lowPressure = 1f),
                TireReading(VehicleArea.WHEEL_RIGHT_FRONT, lowPressure = 1f),
                TireReading(VehicleArea.WHEEL_RIGHT_REAR, lowPressure = 1f),
            ),
            telltales = listOf(
                TelltaleReading(TelltaleKind.LOW_VOLT_BATTERY, active = true, rawName = "LOW_VOLT_BATTERY"),
                TelltaleReading(TelltaleKind.MASTER_WARNING, active = true, rawName = "MASTER_WARNING"),
                TelltaleReading(TelltaleKind.ENGINE_TEMP, active = true, rawName = "ENGINE_TEMP"),
            ),
        ),
        // Telltale-only fallbacks carry the raw SDK name in the body.
        calm.copy(
            doors = calm.doors.filter { it.area != VehicleArea.TRUNK },
            tires = emptyList(),
            socPercent = null,
            fuelPercent = null,
            unread = emptyList(),
            telltales = listOf(
                TelltaleReading(TelltaleKind.TRUNK_OPEN, active = true, rawName = "TRUNK_OPEN"),
                TelltaleReading(TelltaleKind.DOOR_OPEN, active = true, rawName = "DOOR_OPEN"),
                TelltaleReading(TelltaleKind.TPMS, active = true, rawName = "TPMS"),
                TelltaleReading(TelltaleKind.LOW_FUEL, active = true, rawName = "LOW_FUEL"),
                TelltaleReading(TelltaleKind.LOW_EV_BATTERY, active = true, rawName = "LOW_EV_BATTERY"),
            ),
        ),
    )

    /**
     * Every check except the grouped telltale fails, most with two places so their bodies take two
     * lines. Telltales read clean, so the panel still has a chip above the unchecked line.
     */
    val fourteenIssues: VehicleSnapshot = everyItemManyPlaces.first().let { many ->
        many.copy(
            doors = many.doors + DoorReading(VehicleArea.HOOD, position = 1, locked = true),
            telltales = listOf(TelltaleReading(TelltaleKind.ENGINE, active = false, rawName = "ENGINE")),
            tripDistance = null,
            unread = listOf(UnreadSignal(SignalSource.TRIP, UnreadCause.NOT_AVAILABLE)),
        )
    }

    /** Charge and fuel recover: twelve cards, three rows, three chips. */
    val twelveIssues: VehicleSnapshot = fourteenIssues.copy(socPercent = 80, fuelPercent = 60)

    /** Everything read and fine, telltales included, so leftover checks become chips. */
    private val allRead: VehicleSnapshot = calm.copy(
        washerLow = false,
        telltales = fourteenIssues.telltales,
        unread = fourteenIssues.unread,
    )

    /**
     * The tallest summary for [count] cards: the first [count] of [fourteenIssues] in evaluator
     * order put a two-line body in every row, the headline carries both context lines, and the
     * panel shows every remaining chip and the unchecked line.
     */
    fun tallest(count: Int): ParkUiState {
        val report = ParkingCheckEvaluator().evaluate(fourteenIssues)
        val cards = summaryCards(report, seen = emptyList()).take(count)
        return previewState(
            report = report.copy(items = report.items.take(count), headline = "확인 ${count}건"),
            cards = cards,
            confirmed = confirmedLabels(allRead, cards.map { it.id }.toSet()),
        )
    }

    /** All clear after [count] issues were fixed in this parked session. */
    fun resolved(count: Int): ParkUiState {
        val seen = rememberSeen(emptyList(), ParkingCheckEvaluator().evaluate(fourteenIssues).items.take(count))
        val report = ParkingCheckEvaluator().evaluate(allRead)
        val cards = summaryCards(report, seen)
        return previewState(
            report = report,
            cards = cards,
            confirmed = confirmedLabels(allRead, cards.map { it.id }.toSet()),
        )
    }

    fun state(snapshot: VehicleSnapshot): ParkUiState {
        val report = ParkingCheckEvaluator().evaluate(snapshot)
        val cards = summaryCards(report, seen = emptyList())
        return previewState(
            report = report,
            cards = cards,
            confirmed = confirmedLabels(snapshot, cards.map { it.id }.toSet()),
        )
    }

    private fun VehicleArea.isFrontRow(): Boolean =
        this == VehicleArea.ROW_1_LEFT || this == VehicleArea.ROW_1_RIGHT
}
