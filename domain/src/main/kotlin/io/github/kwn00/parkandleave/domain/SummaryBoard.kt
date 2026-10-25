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
)

const val RESOLVED_DETAIL: String = "해결됨"

fun Severity.toCardTone(): CardTone = when (this) {
    Severity.WARNING -> CardTone.CRITICAL
    Severity.CAUTION -> CardTone.WARN
}

/**
 * How many equal columns the summary grid should use for this window.
 * Fewer cards than the fit still span the width, one card per column.
 */
fun summaryGridColumns(widthDp: Int, cardCount: Int): Int {
    if (cardCount <= 1) return 1
    val fit = when {
        widthDp >= 1500 -> 4
        widthDp >= 1080 -> 3
        widthDp >= 720 -> 2
        else -> 1
    }
    return minOf(fit, cardCount)
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
