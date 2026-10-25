package io.github.kwn00.parkandleave.domain

enum class CardTone {
    CRITICAL,
    WARN,
    INFO,
}

enum class HeadlineTone {
    NEUTRAL,
    CRITICAL,
    WARN,
    OK,
}

data class SeenIssue(
    val id: SignalId,
    val title: String,
    val severity: Severity,
)

data class SummaryCard(
    val id: SignalId,
    val tone: CardTone,
    val title: String,
    val detail: String,
    val resolved: Boolean,
    /** One-line action. Empty when the card is resolved. */
    val guidance: String = "",
)

const val RESOLVED_DETAIL: String = "해결됨"

fun Severity.toCardTone(): CardTone = when (this) {
    Severity.WARNING -> CardTone.CRITICAL
    Severity.CAUTION -> CardTone.WARN
}

/**
 * How many columns a row can hold. The count does not shrink the slot:
 * one card stays the same width as a card in a full row and aligns left.
 */
fun summaryGridColumns(widthDp: Int, cardCount: Int): Int {
    if (cardCount <= 0) return 1
    return when {
        widthDp >= 1500 -> 4
        widthDp >= 1080 -> 3
        widthDp >= 720 -> 2
        else -> 1
    }
}

fun rememberSeen(previous: List<SeenIssue>, items: List<CheckItem>): List<SeenIssue> {
    val ordered = linkedMapOf<SignalId, SeenIssue>()
    previous.forEach { issue -> ordered[issue.id] = issue }
    items.forEach { item ->
        ordered[item.id] = SeenIssue(id = item.id, title = item.title, severity = item.severity)
    }
    return ordered.values.toList()
}

fun seenForSession(
    phase: SessionPhase,
    previous: List<SeenIssue>,
    report: ParkingReport?,
): List<SeenIssue> = when (phase) {
    SessionPhase.IDLE,
    SessionPhase.FROZEN,
    -> emptyList()
    SessionPhase.ACTIVE -> if (report == null) previous else rememberSeen(previous, report.items)
}

/**
 * Active cards keep evaluator order. Issues that were seen earlier in this
 * parked session and then disappeared stay as resolved info cards.
 * A partial read (no items, not all-clear) does not claim those issues are resolved.
 */
fun summaryCards(report: ParkingReport, seen: List<SeenIssue>): List<SummaryCard> {
    if (report.items.isEmpty() && !report.allClear) return emptyList()
    val activeIds = report.items.map { it.id }.toSet()
    val active = report.items.map { item ->
        SummaryCard(
            id = item.id,
            tone = item.severity.toCardTone(),
            title = item.title,
            detail = item.detail,
            resolved = false,
            guidance = item.guidance,
        )
    }
    val resolved = seen
        .filter { it.id !in activeIds }
        .map { issue ->
            SummaryCard(
                id = issue.id,
                tone = CardTone.INFO,
                title = issue.title,
                detail = RESOLVED_DETAIL,
                resolved = true,
            )
        }
    return active + resolved
}

fun headlineTone(allClear: Boolean, cards: List<SummaryCard>): HeadlineTone = when {
    allClear -> HeadlineTone.OK
    cards.any { !it.resolved && it.tone == CardTone.CRITICAL } -> HeadlineTone.CRITICAL
    cards.any { !it.resolved && it.tone == CardTone.WARN } -> HeadlineTone.WARN
    else -> HeadlineTone.NEUTRAL
}

/**
 * Names of signals that were read and are fine, in display order.
 *
 * [cardIds] are already on the board, including resolved cards, so they are not repeated.
 * An empty list was not read. Trip distance is not a pass or fail.
 * A disconnected link confirms nothing.
 */
fun confirmedLabels(snapshot: VehicleSnapshot, cardIds: Set<SignalId>): List<String> {
    if (snapshot.link != SdkLink.CONNECTED) return emptyList()
    return buildList {
        addConfirmed(cardIds, SignalId.WINDOW, "창문", windowsConfirmed(snapshot))
        addConfirmed(cardIds, SignalId.DOOR_OPEN, "도어", cabinDoorsClosed(snapshot))
        addConfirmed(cardIds, SignalId.DOOR_UNLOCKED, "도어 잠금", cabinDoorsLocked(snapshot))
        addConfirmed(cardIds, SignalId.HOOD, "보닛", closureConfirmed(snapshot, VehicleArea.HOOD))
        addConfirmed(cardIds, SignalId.TRUNK, "트렁크", closureConfirmed(snapshot, VehicleArea.TRUNK))
        addConfirmed(
            cardIds,
            SignalId.CABIN_LIGHT,
            "실내등",
            snapshot.cabinLightOn == false && !snapshot.missed(SignalSource.CABIN_LIGHT),
        )
        addConfirmed(cardIds, SignalId.READING_LIGHT, "독서등", readingLightsConfirmed(snapshot))
        addConfirmed(cardIds, SignalId.EXTERIOR_LIGHT, "외부등", exteriorConfirmed(snapshot))
        addConfirmed(
            cardIds,
            SignalId.HVAC,
            "공조",
            snapshot.hvacOn == false && !snapshot.missed(SignalSource.HVAC),
        )
        addConfirmed(cardIds, SignalId.TIRE, "타이어", tiresConfirmed(snapshot))
        addConfirmed(
            cardIds,
            SignalId.WASHER,
            "워셔액",
            snapshot.washerLow == false && !snapshot.missed(SignalSource.WASHER),
        )
        addConfirmed(cardIds, SignalId.LOW_SOC, "배터리", socConfirmed(snapshot))
        addConfirmed(cardIds, SignalId.LOW_FUEL, "연료", fuelConfirmed(snapshot))
        addConfirmed(cardIds, SignalId.TELLTALE, "경고등", telltalesConfirmed(snapshot))
    }
}

private fun MutableList<String>.addConfirmed(
    cardIds: Set<SignalId>,
    id: SignalId,
    label: String,
    ok: Boolean,
) {
    if (ok && id !in cardIds) add(label)
}

private fun VehicleSnapshot.missed(source: SignalSource): Boolean =
    unread.any { it.source == source }

private fun windowsConfirmed(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.missed(SignalSource.WINDOWS)) return false
    val supported = snapshot.windows.filter { it.supported }
    return supported.isNotEmpty() && supported.all { it.position <= 0 }
}

private fun cabinDoorsClosed(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.missed(SignalSource.DOOR_POSITION)) return false
    val known = snapshot.doors.filter { it.area.isCabinDoor() && it.position != null }
    return known.isNotEmpty() && known.all { (it.position ?: 0) <= 0 }
}

private fun cabinDoorsLocked(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.missed(SignalSource.DOOR_LOCK)) return false
    val known = snapshot.doors.filter { it.area.isCabinDoor() && it.locked != null }
    return known.isNotEmpty() && known.all { it.locked == true }
}

private fun closureConfirmed(snapshot: VehicleSnapshot, area: VehicleArea): Boolean {
    val door = snapshot.doors.find { it.area == area } ?: return false
    if (door.position == null && door.locked == null) return false
    val open = (door.position ?: 0) > 0 || door.locked == false
    return !open
}

private fun readingLightsConfirmed(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.missed(SignalSource.READING_LIGHT)) return false
    val known = snapshot.readingLights.filter { it.on != null }
    return known.isNotEmpty() && known.all { it.on == false }
}

private fun exteriorConfirmed(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.lowBeamOn == null && snapshot.tailLampOn == null) return false
    if (snapshot.missed(SignalSource.LOW_BEAM) || snapshot.missed(SignalSource.TAIL_LAMP)) return false
    return snapshot.lowBeamOn != true && snapshot.tailLampOn != true
}

private fun tiresConfirmed(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.missed(SignalSource.TIRES)) return false
    val known = snapshot.tires.mapNotNull { it.lowPressure }
    return known.isNotEmpty() && known.none { it > 0f && it <= ParkingThresholds.TIRE_FLAG_MAX }
}

private fun socConfirmed(snapshot: VehicleSnapshot): Boolean {
    val soc = snapshot.socPercent ?: return false
    if (snapshot.missed(SignalSource.SOC)) return false
    return soc >= ParkingThresholds.SOC_ITEM_PERCENT
}

private fun fuelConfirmed(snapshot: VehicleSnapshot): Boolean {
    val fuel = snapshot.fuelPercent ?: return false
    if (snapshot.missed(SignalSource.FUEL)) return false
    return fuel >= ParkingThresholds.FUEL_ITEM_PERCENT
}

private fun telltalesConfirmed(snapshot: VehicleSnapshot): Boolean {
    if (snapshot.missed(SignalSource.TELLTALES)) return false
    if (snapshot.telltales.isEmpty()) return false
    return snapshot.telltales.none { it.active }
}

private fun VehicleArea.isCabinDoor(): Boolean = when (this) {
    VehicleArea.ROW_1_LEFT,
    VehicleArea.ROW_1_CENTER,
    VehicleArea.ROW_1_RIGHT,
    VehicleArea.ROW_2_LEFT,
    VehicleArea.ROW_2_CENTER,
    VehicleArea.ROW_2_RIGHT,
    VehicleArea.ROW_3_LEFT,
    VehicleArea.ROW_3_CENTER,
    VehicleArea.ROW_3_RIGHT,
    -> true
    VehicleArea.HOOD,
    VehicleArea.TRUNK,
    VehicleArea.FRONT_WINDSHIELD,
    VehicleArea.REAR_WINDSHIELD,
    VehicleArea.ROOF,
    VehicleArea.WHEEL_LEFT_FRONT,
    VehicleArea.WHEEL_RIGHT_FRONT,
    VehicleArea.WHEEL_LEFT_REAR,
    VehicleArea.WHEEL_RIGHT_REAR,
    -> false
}
