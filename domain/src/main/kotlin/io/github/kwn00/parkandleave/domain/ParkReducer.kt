package io.github.kwn00.parkandleave.domain

data class ParkCursor(
    val gear: Gear,
    val link: SdkLink,
)

data class ParkFrame(
    val gear: Gear,
    val restricted: Boolean,
    val manual: Boolean,
    val link: SdkLink,
    val snapshot: VehicleSnapshot,
)

data class ParkReduction(
    val session: CheckSession,
    val report: ParkingReport?,
    val reread: Boolean,
    val seen: List<SeenIssue>,
    val cards: List<SummaryCard>,
)

/**
 * Pure parked-check step.
 * Entering P, or the service reconnecting while already in P, asks for a fresh read.
 * A missing service or a null property becomes an unchecked line, not "모두 정상".
 */
fun reducePark(
    previous: ParkCursor,
    seen: List<SeenIssue>,
    frame: ParkFrame,
    evaluator: ParkingCheckEvaluator = ParkingCheckEvaluator(),
): ParkReduction {
    val session = checkSession(frame.gear, frame.restricted, frame.manual)
    val enteredPark = frame.gear == Gear.PARK && previous.gear != Gear.PARK
    val reconnectedWhileParked = frame.gear == Gear.PARK &&
        frame.link == SdkLink.CONNECTED &&
        previous.link != SdkLink.CONNECTED
    val reread = !frame.restricted && (enteredPark || reconnectedWhileParked)
    val report = reportFor(session, frame, evaluator)
    val nextSeen = seenForSession(session.phase, seen, report)
    val cards = if (report != null && session.phase == SessionPhase.ACTIVE) {
        summaryCards(report, nextSeen)
    } else {
        emptyList()
    }
    return ParkReduction(
        session = session,
        report = report,
        reread = reread,
        seen = nextSeen,
        cards = cards,
    )
}

fun reportFor(
    session: CheckSession,
    frame: ParkFrame,
    evaluator: ParkingCheckEvaluator = ParkingCheckEvaluator(),
): ParkingReport? {
    if (session.phase != SessionPhase.ACTIVE) return null
    return when (frame.link) {
        SdkLink.CONNECTED -> if (frame.snapshot.ready) evaluator.evaluate(frame.snapshot) else null
        SdkLink.NOT_CONNECTED,
        SdkLink.DISCONNECTED,
        -> evaluator.evaluate(frame.snapshot.copy(ready = true, link = frame.link))
    }
}
