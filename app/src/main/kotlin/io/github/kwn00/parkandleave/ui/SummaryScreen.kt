package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kwn00.parkandleave.designsystem.LocalParkLeaveColors
import io.github.kwn00.parkandleave.designsystem.ParkLeaveColors
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
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import io.github.kwn00.parkandleave.domain.headlineTone
import io.github.kwn00.parkandleave.domain.summaryGridColumns
import io.github.kwn00.parkandleave.domain.summaryLayout
import io.github.kwn00.parkandleave.domain.SummaryLayout
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
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        val width = maxWidth
        Column(Modifier.fillMaxSize()) {
            SummaryHeader(state, onOpenDebug)
            Spacer(Modifier.height(20.dp))
            when (state.session.phase) {
                SessionPhase.FROZEN -> StatusMessage(
                    text = "주행 중에는 점검을 멈춥니다",
                    color = colors.onCritical,
                    modifier = Modifier.weight(1f),
                )
                SessionPhase.IDLE -> StatusMessage(
                    text = if (state.gear.gear == Gear.UNKNOWN) {
                        "기어를 아직 읽지 못했습니다"
                    } else {
                        "기어를 P에 두면 점검을 시작합니다"
                    },
                    color = colors.onSurface,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Park & Leave",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                gearLabel(state),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
            )
            TextButton(
                onClick = onOpenDebug,
                modifier = Modifier.heightIn(min = 64.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceMuted),
            ) {
                Text("신호 디버그", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActiveSummary(state: ParkUiState, width: Dp, modifier: Modifier) {
    val colors = LocalParkLeaveColors.current
    val report = state.report
    if (state.scanning || report == null) {
        StatusMessage("점검 중", colors.onSurface, modifier)
        return
    }
    if (report.allClear && state.cards.isEmpty()) {
        Column(modifier, verticalArrangement = Arrangement.Center) {
            AllClearHero(report, Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 520.dp))
            UncheckedNote(report.uncheckedLines)
        }
        return
    }
    val layout = summaryLayout(state.cards.size)
    if (!report.allClear && layout == SummaryLayout.SPOTLIGHT && state.cards.isNotEmpty()) {
        Column(modifier) {
            IssueHeadline(state, report)
            UncheckedNote(report.uncheckedLines)
            Spacer(Modifier.height(28.dp))
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
            IssueHeadline(state, report)
            UncheckedNote(report.uncheckedLines)
            Spacer(Modifier.height(28.dp))
            CardRow(state.cards)
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState())) {
        if (report.allClear) {
            AllClearHero(report, Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 420.dp))
            UncheckedNote(report.uncheckedLines)
            Spacer(Modifier.height(28.dp))
            Text(
                "해결된 항목",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                maxLines = 1,
            )
            Spacer(Modifier.height(16.dp))
            CardGrid(state.cards, width, Modifier.fillMaxWidth())
        } else {
            IssueHeadline(state, report)
            UncheckedNote(report.uncheckedLines)
            if (state.cards.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                CardGrid(state.cards, width, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun UncheckedNote(lines: List<String>) {
    val note = uncheckedNote(lines) ?: return
    val colors = LocalParkLeaveColors.current
    Text(
        note,
        modifier = Modifier
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.info)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onInfo,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SpotlightCard(card: SummaryCard) {
    CheckCard(
        card = card,
        minHeight = 300.dp,
        modifier = Modifier.widthIn(max = 760.dp).heightIn(max = 440.dp),
        iconSize = 88.dp,
        titleSize = 48.sp,
        centered = true,
    )
}

@Composable
private fun CardRow(cards: List<SummaryCard>) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        cards.forEach { card ->
            CheckCard(
                card = card,
                minHeight = 220.dp,
                modifier = Modifier.weight(1f).heightIn(max = 360.dp),
            )
        }
    }
}

@Composable
private fun IssueHeadline(state: ParkUiState, report: ParkingReport) {
    val colors = LocalParkLeaveColors.current
    val tone = headlineTone(report.allClear, state.cards)
    Text(
        report.headline,
        style = MaterialTheme.typography.displayLarge,
        color = headlineColor(tone, colors),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    report.tomorrowLine?.let { line ->
        Spacer(Modifier.height(8.dp))
        Text(
            line,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (state.cards.any { !it.resolved } && !state.controlAvailable) {
        Spacer(Modifier.height(8.dp))
        Text(
            "직접 조치하면 카드가 갱신됩니다",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AllClearHero(report: ParkingReport, modifier: Modifier) {
    val colors = LocalParkLeaveColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.ok)
            .padding(horizontal = 40.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusGlyph(GlyphKind.CHECK, colors.onOk, Modifier.size(96.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            report.headline,
            style = MaterialTheme.typography.displayLarge,
            color = colors.onOk,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        report.clearanceLine?.let { line ->
            Spacer(Modifier.height(8.dp))
            Text(
                line,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onOk,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        report.tomorrowLine?.let { line ->
            Spacer(Modifier.height(12.dp))
            Text(
                line,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onOk,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        for (row in cards.chunked(columns)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                for (card in row) {
                    CheckCard(
                        card = card,
                        minHeight = 168.dp,
                        modifier = Modifier.weight(1f).heightIn(max = 280.dp),
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
    minHeight: Dp,
    modifier: Modifier,
    iconSize: Dp = 48.dp,
    titleSize: TextUnit = 32.sp,
    centered: Boolean = false,
) {
    val colors = LocalParkLeaveColors.current
    val (background, foreground) = cardColors(card.tone, colors)
    val titleStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = titleSize,
        lineHeight = if (centered) 56.sp else 40.sp,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .semantics { contentDescription = "${card.title}. ${card.detail}" }
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusGlyph(glyphFor(card.id), foreground, Modifier.size(iconSize))
            Spacer(Modifier.size(16.dp))
            Text(
                card.title,
                style = titleStyle,
                color = foreground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            card.detail,
            style = if (centered) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
            color = foreground,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusMessage(
    text: String,
    color: Color,
    modifier: Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.displayLarge,
            color = color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (extra != null) {
            Spacer(Modifier.height(24.dp))
            extra()
        }
    }
}

@Composable
private fun ManualCheckButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = 280.dp)
            .heightIn(min = 64.dp),
    ) {
        Text("지금 점검", style = MaterialTheme.typography.labelLarge)
    }
}

private fun cardColors(tone: CardTone, colors: ParkLeaveColors): Pair<Color, Color> = when (tone) {
    CardTone.CRITICAL -> colors.critical to colors.onCritical
    CardTone.WARN -> colors.warn to colors.onWarn
    CardTone.INFO -> colors.info to colors.onInfo
}

private fun headlineColor(tone: HeadlineTone, colors: ParkLeaveColors): Color = when (tone) {
    HeadlineTone.NEUTRAL -> colors.onSurface
    HeadlineTone.CRITICAL -> colors.onCritical
    HeadlineTone.WARN -> colors.onWarn
    HeadlineTone.OK -> colors.onOk
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
        SummaryCard(SignalId.REAR_OCCUPANCY, CardTone.CRITICAL, "뒷좌석 확인", "2열 왼쪽 점유", resolved = false),
        SummaryCard(SignalId.WINDOW, CardTone.WARN, "창문 열림", "2열 오른쪽 30%", resolved = false),
        SummaryCard(SignalId.HVAC, CardTone.WARN, "공조 켜짐", "공조가 켜져 있습니다", resolved = false),
        SummaryCard(SignalId.EXTERIOR_LIGHT, CardTone.WARN, "외부등 켜짐", "미등 켜짐", resolved = false),
        SummaryCard(SignalId.DOOR_UNLOCKED, CardTone.WARN, "도어 잠금 해제", "1열 왼쪽 잠금 해제", resolved = false),
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
        SummaryCard(SignalId.REAR_OCCUPANCY, CardTone.CRITICAL, "뒷좌석 점유", "2열 왼쪽 탑승", resolved = false),
        SummaryCard(SignalId.EXTERIOR_LIGHT, CardTone.WARN, "외부등", "미등 켜짐", resolved = false),
        SummaryCard(SignalId.HVAC, CardTone.WARN, "공조", "켜짐", resolved = false),
        SummaryCard(SignalId.WINDOW, CardTone.INFO, "창문 열림", "해결됨", resolved = true),
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
