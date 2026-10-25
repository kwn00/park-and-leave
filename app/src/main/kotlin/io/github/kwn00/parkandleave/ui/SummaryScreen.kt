package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.github.kwn00.parkandleave.designsystem.LocalParkLeaveColors
import io.github.kwn00.parkandleave.designsystem.LocalParkLeaveType
import io.github.kwn00.parkandleave.designsystem.ParkIcon
import io.github.kwn00.parkandleave.designsystem.ParkIconKind
import io.github.kwn00.parkandleave.designsystem.ParkLeaveColors
import io.github.kwn00.parkandleave.designsystem.ParkLeaveCorner
import io.github.kwn00.parkandleave.designsystem.ParkLeaveDimen
import io.github.kwn00.parkandleave.designsystem.ParkLeaveSpace
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import io.github.kwn00.parkandleave.domain.CardTone
import io.github.kwn00.parkandleave.domain.CheckSession
import io.github.kwn00.parkandleave.domain.Gear
import io.github.kwn00.parkandleave.domain.GearReading
import io.github.kwn00.parkandleave.domain.HeadlineTone
import io.github.kwn00.parkandleave.domain.ParkingReport
import io.github.kwn00.parkandleave.domain.SessionPhase
import io.github.kwn00.parkandleave.domain.SignalId
import io.github.kwn00.parkandleave.domain.SummaryCard
import io.github.kwn00.parkandleave.domain.SummaryLayout
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.headlineTone
import io.github.kwn00.parkandleave.domain.summaryGridColumns
import io.github.kwn00.parkandleave.domain.summaryLayout
import io.github.kwn00.parkandleave.domain.uncheckedNote

@Composable
fun SummaryScreen(
    state: ParkUiState,
    onOpenDebug: () -> Unit,
    onManualCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalParkLeaveColors.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = ParkLeaveSpace.s4, vertical = ParkLeaveSpace.s3),
    ) {
        val width = maxWidth
        Column(Modifier.fillMaxSize()) {
            SummaryHeader(state, onOpenDebug)
            Spacer(Modifier.height(ParkLeaveSpace.s3))
            when (state.session.phase) {
                SessionPhase.FROZEN -> StatusMessage(
                    text = "주행 중에는 점검을 멈춥니다",
                    modifier = Modifier.weight(1f),
                    alert = true,
                )
                SessionPhase.IDLE -> StatusMessage(
                    text = if (state.gear.gear == Gear.UNKNOWN) {
                        "기어를 아직 읽지 못했습니다"
                    } else {
                        "기어를 P에 두면 점검을 시작합니다"
                    },
                    modifier = Modifier.weight(1f),
                    extra = if (state.showManualCheck) {
                        { ManualCheckButton(onManualCheck) }
                    } else {
                        null
                    },
                )
                SessionPhase.ACTIVE -> ActiveSummary(
                    state = state,
                    width = width,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryHeader(state: ParkUiState, onOpenDebug: () -> Unit) {
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    val report = state.report
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Park & Leave",
                style = type.brand,
                color = colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.session.phase == SessionPhase.ACTIVE && report != null && !state.scanning) {
                Spacer(Modifier.height(ParkLeaveSpace.s2))
                StatusLine(state, report)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ParkLeaveSpace.s2),
        ) {
            Text(
                gearLabel(state),
                style = type.meta,
                color = colors.onSurfaceMuted,
                maxLines = 1,
            )
            TextButton(
                onClick = onOpenDebug,
                modifier = Modifier.heightIn(min = ParkLeaveDimen.touch),
                shape = ParkLeaveCorner,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurface),
            ) {
                Text("신호 디버그", style = type.button)
            }
        }
    }
}

@Composable
private fun StatusLine(state: ParkUiState, report: ParkingReport) {
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    val tone = headlineTone(report.allClear, state.cards)
    val mark = statusMark(tone, colors)
    val showManual = !report.allClear &&
        state.cards.any { !it.resolved } &&
        !state.controlAvailable
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(ParkLeaveSpace.s3)
                    .height(ParkLeaveDimen.statusMark),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .width(ParkLeaveDimen.accentBar)
                        .height(ParkLeaveDimen.statusMark)
                        .background(mark),
                )
            }
            Text(
                report.headline,
                modifier = Modifier.weight(1f),
                style = type.status,
                color = statusColor(tone, colors),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (report.tomorrowLine != null || showManual) {
            Column(
                modifier = Modifier.padding(start = ParkLeaveSpace.s3, top = ParkLeaveSpace.s1),
                verticalArrangement = Arrangement.spacedBy(ParkLeaveSpace.s1),
            ) {
                report.tomorrowLine?.let { line ->
                    Text(
                        line,
                        style = type.support,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showManual) {
                    Text(
                        "직접 조치하면 카드가 갱신됩니다",
                        style = type.support,
                        color = colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSummary(state: ParkUiState, width: Dp, modifier: Modifier) {
    val type = LocalParkLeaveType.current
    val colors = LocalParkLeaveColors.current
    val report = state.report
    if (state.scanning || report == null) {
        StatusMessage("점검 중", modifier)
        return
    }
    if (report.allClear && state.cards.isEmpty()) {
        Column(modifier, verticalArrangement = Arrangement.Center) {
            ClearanceLine(report.clearanceLine)
            uncheckedNote(report.uncheckedLines)?.let { note ->
                Spacer(Modifier.height(ParkLeaveSpace.s2))
                UncheckedLine(note)
            }
        }
        return
    }
    val layout = summaryLayout(state.cards.size)
    if (!report.allClear && layout == SummaryLayout.SPOTLIGHT && state.cards.isNotEmpty()) {
        Column(modifier) {
            uncheckedNote(report.uncheckedLines)?.let { note ->
                UncheckedLine(note)
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SpotlightCard(state.cards.first())
            }
        }
        return
    }
    if (!report.allClear && layout == SummaryLayout.ROW && state.cards.isNotEmpty()) {
        Column(modifier) {
            uncheckedNote(report.uncheckedLines)?.let { note ->
                UncheckedLine(note)
                Spacer(Modifier.height(ParkLeaveSpace.s3))
            }
            CardRow(state.cards)
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState())) {
        if (report.allClear) {
            ClearanceLine(report.clearanceLine)
            uncheckedNote(report.uncheckedLines)?.let { note ->
                Spacer(Modifier.height(ParkLeaveSpace.s2))
                UncheckedLine(note)
            }
            Spacer(Modifier.height(ParkLeaveSpace.s4))
            Text(
                "해결된 항목",
                style = type.section,
                color = colors.onSurfaceMuted,
                maxLines = 1,
            )
            Spacer(Modifier.height(ParkLeaveSpace.s2))
            CardGrid(state.cards, width, Modifier.fillMaxWidth())
        } else {
            val note = uncheckedNote(report.uncheckedLines)
            if (note != null) UncheckedLine(note)
            if (state.cards.isNotEmpty()) {
                if (note != null) Spacer(Modifier.height(ParkLeaveSpace.s3))
                CardGrid(state.cards, width, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun UncheckedLine(note: String) {
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParkIcon(
            ParkIconKind.INFO,
            colors.accent,
            Modifier.size(ParkLeaveDimen.iconSmall),
        )
        Spacer(Modifier.width(ParkLeaveSpace.s2))
        Text(
            note,
            modifier = Modifier.weight(1f),
            style = type.support,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClearanceLine(line: String?) {
    if (line == null) return
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        ParkIcon(
            ParkIconKind.CHECK,
            colors.accent,
            Modifier.size(ParkLeaveDimen.icon),
        )
        Spacer(Modifier.width(ParkLeaveSpace.s2))
        Text(
            line,
            style = type.clearance,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SpotlightCard(card: SummaryCard) {
    CheckCard(
        card = card,
        emphasis = true,
        modifier = Modifier.widthIn(max = ParkLeaveDimen.spotlightMaxWidth),
    )
}

@Composable
private fun CardRow(cards: List<SummaryCard>) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(max = ParkLeaveDimen.rowMax),
        horizontalArrangement = Arrangement.spacedBy(ParkLeaveSpace.s2),
        verticalAlignment = Alignment.Top,
    ) {
        cards.forEach { card ->
            CheckCard(
                card = card,
                modifier = Modifier.weight(1f).heightIn(max = ParkLeaveDimen.rowMax),
            )
        }
    }
}

@Composable
private fun CardGrid(
    cards: List<SummaryCard>,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    val columns = summaryGridColumns(width.value.toInt(), cards.size).coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ParkLeaveSpace.s2)) {
        for (row in cards.chunked(columns)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(max = ParkLeaveDimen.cardMax),
                horizontalArrangement = Arrangement.spacedBy(ParkLeaveSpace.s2),
                verticalAlignment = Alignment.Top,
            ) {
                for (card in row) {
                    CheckCard(
                        card = card,
                        modifier = Modifier.weight(1f).heightIn(max = ParkLeaveDimen.cardMax),
                    )
                }
                var blanks = columns - row.size
                while (blanks > 0) {
                    Spacer(Modifier.weight(1f))
                    blanks -= 1
                }
            }
        }
    }
}

@Composable
private fun CheckCard(
    card: SummaryCard,
    modifier: Modifier,
    emphasis: Boolean = false,
) {
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    val accent = cardAccent(card.tone, colors)
    val minHeight = if (emphasis) ParkLeaveDimen.spotlightMin else ParkLeaveDimen.cardMin
    val maxHeight = if (emphasis) ParkLeaveDimen.spotlightMaxHeight else ParkLeaveDimen.cardMax
    val description = buildString {
        append(card.title)
        append(". ")
        append(card.detail)
        if (card.hint.isNotBlank()) {
            append(". ")
            append(card.hint)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .border(ParkLeaveDimen.border, colors.outline, ParkLeaveCorner)
            .clip(ParkLeaveCorner)
            .background(colors.surface)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(ParkLeaveDimen.accentBar)
                    .fillMaxHeight()
                    .background(accent),
            )
        }
        Column(
            Modifier.padding(
                start = ParkLeaveSpace.s3,
                end = ParkLeaveSpace.s3,
                top = if (emphasis) ParkLeaveSpace.s4 else ParkLeaveSpace.s3,
                bottom = if (emphasis) ParkLeaveSpace.s4 else ParkLeaveSpace.s3,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                ParkIcon(
                    glyphFor(card.id),
                    accent,
                    Modifier.size(if (emphasis) ParkLeaveDimen.iconEmphasis else ParkLeaveDimen.icon),
                )
                Spacer(Modifier.size(ParkLeaveSpace.s2))
                Column(Modifier.weight(1f)) {
                    Text(
                        card.title,
                        style = if (emphasis) type.cardTitleEmphasis else type.cardTitle,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(ParkLeaveSpace.s1))
                    Text(
                        card.detail,
                        style = if (emphasis) type.cardBodyEmphasis else type.cardBody,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (card.hint.isNotBlank()) {
                        Spacer(Modifier.height(ParkLeaveSpace.s1))
                        Text(
                            card.hint,
                            style = type.cardHint,
                            color = colors.onSurfaceMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    text: String,
    modifier: Modifier,
    alert: Boolean = false,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (alert) {
                ParkIcon(
                    ParkIconKind.ALERT,
                    colors.critical,
                    Modifier.size(ParkLeaveDimen.iconEmphasis),
                )
                Spacer(Modifier.width(ParkLeaveSpace.s2))
            }
            Text(
                text,
                modifier = Modifier.weight(1f),
                style = type.message,
                color = colors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (extra != null) {
            Spacer(Modifier.height(ParkLeaveSpace.s3))
            extra()
        }
    }
}

@Composable
private fun ManualCheckButton(onClick: () -> Unit) {
    val colors = LocalParkLeaveColors.current
    val type = LocalParkLeaveType.current
    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = ParkLeaveDimen.actionMinWidth)
            .heightIn(min = ParkLeaveDimen.touch),
        shape = ParkLeaveCorner,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.buttonContainer,
            contentColor = colors.buttonContent,
        ),
        border = BorderStroke(ParkLeaveDimen.border, colors.buttonBorder),
    ) {
        Text("지금 점검", style = type.button)
    }
}

private fun cardAccent(tone: CardTone, colors: ParkLeaveColors): Color = when (tone) {
    CardTone.CRITICAL -> colors.critical
    CardTone.WARN -> colors.warn
    CardTone.INFO -> colors.onSurfaceMuted
}

private fun statusColor(tone: HeadlineTone, colors: ParkLeaveColors): Color = when (tone) {
    HeadlineTone.NEUTRAL -> colors.onSurface
    HeadlineTone.CRITICAL -> colors.critical
    HeadlineTone.WARN -> colors.warn
    HeadlineTone.OK -> colors.onSurface
}

private fun statusMark(tone: HeadlineTone, colors: ParkLeaveColors): Color = when (tone) {
    HeadlineTone.NEUTRAL -> colors.accent
    HeadlineTone.CRITICAL -> colors.critical
    HeadlineTone.WARN -> colors.warn
    HeadlineTone.OK -> colors.accent
}

private fun gearLabel(state: ParkUiState): String {
    val raw = state.gear.rawValue?.toString() ?: "-"
    return "$raw ${state.gear.gear.name}"
}

@Preview(name = "확인 건", widthDp = 1682, heightDp = 900)
@Composable
private fun IssuesWidePreview() {
    ParkLeaveTheme(darkTheme = false) {
        SummaryScreen(previewIssues(), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "모두 정상", widthDp = 1682, heightDp = 900)
@Composable
private fun AllClearPreview() {
    ParkLeaveTheme(darkTheme = false) {
        SummaryScreen(previewAllClear(resolved = false), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "해결 후", widthDp = 1682, heightDp = 900)
@Composable
private fun ResolvedPreview() {
    ParkLeaveTheme(darkTheme = false) {
        SummaryScreen(previewAllClear(resolved = true), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "야간 확인", widthDp = 1682, heightDp = 900)
@Composable
private fun IssuesNightPreview() {
    ParkLeaveTheme(darkTheme = true) {
        SummaryScreen(previewIssues(), onOpenDebug = {}, onManualCheck = {})
    }
}

internal fun previewCardCount(
    count: Int,
    uncheckedLines: List<String> = emptyList(),
): ParkUiState {
    val prototypes = listOf(
        SummaryCard(
            SignalId.REAR_OCCUPANCY,
            CardTone.CRITICAL,
            "뒷좌석 확인",
            "2열 왼쪽 점유",
            resolved = false,
            hint = "뒷좌석을 확인하세요",
        ),
        SummaryCard(
            SignalId.WINDOW,
            CardTone.WARN,
            "창문 열림",
            "2열 오른쪽 30%",
            resolved = false,
            hint = "해당 창문을 닫아 주세요",
        ),
        SummaryCard(
            SignalId.HVAC,
            CardTone.WARN,
            "공조 켜짐",
            "공조가 켜져 있습니다",
            resolved = false,
            hint = "공조를 꺼 주세요",
        ),
        SummaryCard(
            SignalId.EXTERIOR_LIGHT,
            CardTone.WARN,
            "외부등 켜짐",
            "미등 켜짐",
            resolved = false,
            hint = "전조등·미등을 꺼 주세요",
        ),
        SummaryCard(
            SignalId.DOOR_UNLOCKED,
            CardTone.WARN,
            "도어 잠금 해제",
            "1열 왼쪽 잠금 해제",
            resolved = false,
            hint = "도어 잠금 버튼을 눌러 주세요",
        ),
    )
    return previewState(
        report = ParkingReport(
            items = emptyList(),
            headline = "확인 ${count}건",
            allClear = false,
            tomorrowLine = null,
            clearanceLine = null,
            speechLine = null,
            uncheckedLines = uncheckedLines,
        ),
        cards = prototypes.take(count),
    )
}

internal fun previewIssues(): ParkUiState {
    val cards = listOf(
        SummaryCard(
            SignalId.REAR_OCCUPANCY,
            CardTone.CRITICAL,
            "뒷좌석 점유",
            "2열 왼쪽 탑승",
            resolved = false,
            hint = "뒷좌석을 확인하세요",
        ),
        SummaryCard(
            SignalId.EXTERIOR_LIGHT,
            CardTone.WARN,
            "외부등",
            "미등 켜짐",
            resolved = false,
            hint = "전조등·미등을 꺼 주세요",
        ),
        SummaryCard(
            SignalId.HVAC,
            CardTone.WARN,
            "공조",
            "켜짐",
            resolved = false,
            hint = "공조를 꺼 주세요",
        ),
        SummaryCard(
            SignalId.WINDOW,
            CardTone.INFO,
            "창문 열림",
            "해결됨",
            resolved = true,
        ),
    )
    return previewState(
        report = ParkingReport(
            items = emptyList(),
            headline = "확인 3건",
            allClear = false,
            tomorrowLine = null,
            clearanceLine = null,
            speechLine = null,
        ),
        cards = cards,
    )
}

internal fun previewAllClear(resolved: Boolean): ParkUiState {
    val cards = if (resolved) {
        listOf(
            SummaryCard(SignalId.WINDOW, CardTone.INFO, "창문 열림", "해결됨", resolved = true),
            SummaryCard(SignalId.READING_LIGHT, CardTone.INFO, "독서등 켜짐", "해결됨", resolved = true),
        )
    } else {
        emptyList()
    }
    return previewState(
        report = ParkingReport(
            items = emptyList(),
            headline = "모두 정상",
            allClear = true,
            tomorrowLine = null,
            clearanceLine = "내려도 됩니다",
            speechLine = null,
        ),
        cards = cards,
    )
}

private fun previewState(report: ParkingReport, cards: List<SummaryCard>): ParkUiState {
    val gear = GearReading(rawValue = 4, gear = Gear.PARK, error = null)
    return ParkUiState(
        origin = DataOrigin.FAKE,
        screen = AppScreen.SUMMARY,
        gear = gear,
        snapshot = VehicleSnapshot(ready = true),
        session = CheckSession(phase = SessionPhase.ACTIVE, motionAllowed = true),
        report = report,
        scanning = false,
        showManualCheck = false,
        debugRows = emptyList(),
        liveStatus = "",
        vehicleStatus = "",
        speechMode = "",
        controlAvailable = false,
        placeLabel = "",
        cards = cards,
    )
}
