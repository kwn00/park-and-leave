package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kwn00.parkandleave.designsystem.ParkIcon
import io.github.kwn00.parkandleave.designsystem.ParkIconKind
import io.github.kwn00.parkandleave.designsystem.ParkLeaveColors
import io.github.kwn00.parkandleave.designsystem.ParkLeaveDimens
import io.github.kwn00.parkandleave.designsystem.ParkLeaveShapes
import io.github.kwn00.parkandleave.designsystem.ParkLeaveSpace
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import io.github.kwn00.parkandleave.designsystem.SummaryDensity
import io.github.kwn00.parkandleave.designsystem.keepPhrasesTogether
import io.github.kwn00.parkandleave.designsystem.keepWordsTogether
import io.github.kwn00.parkandleave.designsystem.lineBox
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
import io.github.kwn00.parkandleave.domain.uncheckedNote

@Composable
fun SummaryScreen(
    state: ParkUiState,
    onOpenDebug: () -> Unit,
    onManualCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ParkLeaveTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = ParkLeaveSpace.paneHorizontal),
    ) {
        val width = maxWidth
        val density = if (state.session.phase == SessionPhase.ACTIVE) {
            summaryDensity(state.cards.size, width)
        } else {
            SummaryDensity.Regular
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = density.paneTop),
        ) {
            SummaryHeader(state, onOpenDebug)
            Spacer(Modifier.height(density.headerGap))
            when (state.session.phase) {
                SessionPhase.FROZEN -> StatusMessage(
                    text = "주행 중에는 점검을 멈춥니다",
                    icon = ParkIconKind.STEERING,
                    modifier = Modifier.weight(1f),
                )
                SessionPhase.IDLE -> StatusMessage(
                    text = if (state.gear.gear == Gear.UNKNOWN) {
                        "기어를 아직 읽지 못했습니다"
                    } else {
                        "기어를 P에 두면 점검을 시작합니다"
                    },
                    icon = ParkIconKind.PARK,
                    modifier = Modifier.weight(1f),
                    extra = if (state.showManualCheck) {
                        { ManualCheckButton(onManualCheck) }
                    } else {
                        null
                    },
                )
                SessionPhase.ACTIVE -> ActiveSummary(state, width, density)
            }
        }
    }
}

/** Rows come from the card count at a fixed four-column cell; more rows, denser band. */
internal fun summaryDensity(cardCount: Int, contentWidth: Dp): SummaryDensity {
    if (cardCount == 0) return SummaryDensity.Regular
    val columns = summaryGridColumns(contentWidth.value.toInt(), cardCount).coerceAtLeast(1)
    return SummaryDensity.forRows((cardCount + columns - 1) / columns)
}

@Composable
private fun SummaryHeader(state: ParkUiState, onOpenDebug: () -> Unit) {
    val colors = ParkLeaveTheme.colors
    val type = ParkLeaveTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ParkLeaveDimens.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Park & Leave",
            modifier = Modifier.weight(1f),
            style = type.brand,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GearPill(state)
        Spacer(Modifier.width(ParkLeaveSpace.x1))
        TextButton(
            onClick = onOpenDebug,
            modifier = Modifier.heightIn(min = ParkLeaveDimens.touchTarget),
            shape = ParkLeaveShapes.pill,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceMuted),
        ) {
            Text("신호 디버그", style = type.headerAction, maxLines = 1)
        }
    }
}

@Composable
private fun GearPill(state: ParkUiState) {
    val colors = ParkLeaveTheme.colors
    val parked = state.gear.gear == Gear.PARK
    val raw = state.gear.rawValue?.toString() ?: "-"
    val label = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.onSurfaceSubtle)) { append(raw) }
        append(" ")
        append(state.gear.gear.name)
    }
    Row(
        modifier = Modifier
            .height(ParkLeaveDimens.gearPillHeight)
            .clip(ParkLeaveShapes.pill)
            .border(ParkLeaveDimens.hairline, colors.hairline, ParkLeaveShapes.pill)
            .semantics { contentDescription = gearLabel(state) }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(ParkLeaveShapes.pill)
                .background(if (parked) colors.accent else colors.onSurfaceSubtle),
        )
        Spacer(Modifier.width(ParkLeaveSpace.x1Half))
        Text(label, style = ParkLeaveTheme.type.gear, color = colors.onSurface, maxLines = 1)
    }
}

/**
 * The summary never scrolls. Content keeps its natural height, top-aligned, and
 * [SummaryDensity] is chosen so that height stays inside the pane for up to fourteen cards.
 */
@Composable
private fun ColumnScope.ActiveSummary(state: ParkUiState, width: Dp, density: SummaryDensity) {
    val report = state.report
    if (state.scanning || report == null) {
        StatusMessage("점검 중", icon = null, modifier = Modifier.weight(1f))
        return
    }
    if (report.allClear && state.cards.isEmpty()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = ParkLeaveDimens.headerHeight),
            verticalArrangement = Arrangement.Center,
        ) {
            AllClearHero(report, compact = false)
            ConfirmedPanel(state, density, top = ParkLeaveSpace.x6)
        }
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = density.paneBottom),
    ) {
        if (report.allClear) {
            AllClearHero(report, compact = true)
            Spacer(Modifier.height(density.sectionGap))
            SectionLabel("해결된 항목")
            Spacer(Modifier.height(ParkLeaveSpace.x1Half))
        } else {
            IssueHeadline(state, report, density)
            if (state.cards.isNotEmpty()) Spacer(Modifier.height(density.headlineGap))
        }
        CardGrid(state.cards, width, density)
        ConfirmedPanel(state, density, top = density.panelGap)
    }
}

@Composable
private fun IssueHeadline(state: ParkUiState, report: ParkingReport, density: SummaryDensity) {
    val colors = ParkLeaveTheme.colors
    val type = ParkLeaveTheme.type
    val tone = headlineTone(report.allClear, state.cards)
    val context = buildList {
        report.tomorrowLine?.let { add(it to colors.onSurface) }
        if (state.cards.any { !it.resolved } && !state.controlAvailable) {
            add("직접 조치하면 카드가 갱신됩니다" to colors.onSurfaceMuted)
        }
    }
    // Context sits beside the count, on its baseline, so the count costs one line of height.
    Row(Modifier.fillMaxWidth()) {
        BoxedText(
            emphasizeCount(report.headline, headlineAccent(tone, colors)),
            style = if (density.compactHeadline) type.headlineCompact else type.headline,
            color = colors.onSurface,
            modifier = Modifier.alignBy(LastBaseline),
        )
        if (context.isNotEmpty()) {
            Spacer(Modifier.width(ParkLeaveSpace.x6))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alignBy(LastBaseline),
                verticalArrangement = Arrangement.spacedBy(
                    if (density.compactHeadline) 2.dp else ParkLeaveSpace.half,
                ),
            ) {
                context.forEach { (line, color) ->
                    BoxedText(line.keepWordsTogether(), style = type.subline, color = color)
                }
            }
        }
    }
}

@Composable
private fun BoxedText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) = BoxedText(AnnotatedString(text), style, color, modifier, maxLines)

/**
 * Card text keeps the font's measured height. Hangul draws at about 1.45 em even when
 * [TextStyle.lineHeight] is smaller, and [lineBox] would push those glyphs into the card clip.
 */
@Composable
private fun CardLine(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int = 1,
) {
    Text(
        text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Summary text measured at its token line height; see [lineBox]. */
@Composable
private fun BoxedText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text,
        modifier = modifier.lineBox(style.lineHeight),
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The count in "확인 3건" carries the severity hue; the rest of the headline stays ink. */
private fun emphasizeCount(headline: String, accent: Color): AnnotatedString {
    val digits = Regex("\\d+").find(headline) ?: return AnnotatedString(headline)
    return buildAnnotatedString {
        append(headline.substring(0, digits.range.first))
        withStyle(SpanStyle(color = accent)) { append(digits.value) }
        append(headline.substring(digits.range.last + 1))
    }
}

@Composable
private fun AllClearHero(report: ParkingReport, compact: Boolean) {
    val colors = ParkLeaveTheme.colors
    val type = ParkLeaveTheme.type
    val badge = if (compact) ParkLeaveDimens.heroBadgeCompact else ParkLeaveDimens.heroBadge
    val badgeIcon = if (compact) ParkLeaveDimens.heroBadgeIconCompact else ParkLeaveDimens.heroBadgeIcon
    // The compact hero sits above resolved cards, so its text keeps to its line boxes too.
    val box = if (compact) Modifier.lineBox(type.heroTitle.lineHeight) else Modifier
    val subBox = if (compact) Modifier.lineBox(type.heroSubline.lineHeight) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 0.dp else ParkLeaveSpace.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(badge)
                .clip(ParkLeaveShapes.pill)
                .background(colors.okTint),
            contentAlignment = Alignment.Center,
        ) {
            ParkIcon(ParkIconKind.CHECK, colors.ok, Modifier.size(badgeIcon))
        }
        Spacer(Modifier.width(if (compact) ParkLeaveSpace.x5 else ParkLeaveSpace.x6))
        Column {
            Text(
                report.headline,
                modifier = box,
                style = type.heroTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            report.clearanceLine?.let { line ->
                Spacer(Modifier.height(ParkLeaveSpace.x2))
                Text(
                    line.keepWordsTogether(),
                    modifier = subBox,
                    style = type.heroSubline,
                    color = colors.ok,
                    maxLines = 1,
                )
            }
            report.tomorrowLine?.let { line ->
                Spacer(Modifier.height(ParkLeaveSpace.x2))
                Text(
                    line.keepWordsTogether(),
                    style = type.subline,
                    color = colors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun CardGrid(cards: List<SummaryCard>, width: Dp, density: SummaryDensity) {
    if (cards.isEmpty()) return
    val columns = summaryGridColumns(width.value.toInt(), cards.size).coerceAtLeast(1)
    val cardWidth = cardWidth(width, columns, density.gutter)
    Column(verticalArrangement = Arrangement.spacedBy(density.gutter)) {
        for (row in cards.chunked(columns)) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(density.gutter),
            ) {
                for (card in row) {
                    CheckCard(card, density, Modifier.width(cardWidth).fillMaxHeight())
                }
            }
        }
    }
}

private data class CardLook(
    val icon: ParkIconKind,
    val iconTint: Color,
    val tileColor: Color,
    val edge: Color,
    val edgeWidth: Dp,
    val title: Color,
    val detail: Color,
)

private fun cardLook(card: SummaryCard, colors: ParkLeaveColors): CardLook {
    val icon = iconFor(card.id)
    if (card.resolved) {
        return CardLook(
            icon = icon,
            iconTint = colors.ok,
            tileColor = colors.okTint,
            edge = colors.hairline,
            edgeWidth = ParkLeaveDimens.hairline,
            title = colors.onSurfaceMuted,
            detail = colors.ok,
        )
    }
    return when (card.tone) {
        CardTone.CRITICAL -> CardLook(
            icon = icon,
            iconTint = colors.critical,
            tileColor = colors.criticalTint,
            edge = colors.criticalEdge,
            edgeWidth = ParkLeaveDimens.criticalEdge,
            title = colors.onSurface,
            detail = colors.onSurfaceMuted,
        )
        CardTone.WARN -> CardLook(
            icon = icon,
            iconTint = colors.warn,
            tileColor = colors.warnTint,
            edge = colors.hairline,
            edgeWidth = ParkLeaveDimens.hairline,
            title = colors.onSurface,
            detail = colors.onSurfaceMuted,
        )
        CardTone.INFO -> CardLook(
            icon = icon,
            iconTint = colors.accent,
            tileColor = colors.accentTint,
            edge = colors.hairline,
            edgeWidth = ParkLeaveDimens.hairline,
            title = colors.onSurface,
            detail = colors.onSurfaceMuted,
        )
    }
}

@Composable
internal fun CheckCard(card: SummaryCard, density: SummaryDensity, modifier: Modifier) {
    val colors = ParkLeaveTheme.colors
    val type = ParkLeaveTheme.type
    val look = cardLook(card, colors)
    val spoken = if (card.guidance.isNotBlank()) {
        "${card.title}. ${card.detail}. ${card.guidance}"
    } else {
        "${card.title}. ${card.detail}"
    }
    val titleStyle = when {
        density.denseTitle -> type.cardTitleDense
        density.compactTitle -> type.cardTitleCompact
        else -> type.cardTitle
    }
    Column(
        modifier = modifier
            .clip(ParkLeaveShapes.card)
            .background(colors.surface)
            .border(look.edgeWidth, look.edge, ParkLeaveShapes.card)
            .semantics { contentDescription = spoken }
            .padding(horizontal = ParkLeaveSpace.cardPadding, vertical = density.cardPadding),
    ) {
        if (density.iconBesideTitle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(look.icon, look.iconTint, look.tileColor, density)
                Spacer(Modifier.width(density.tileGap))
                CardLine(card.title.keepWordsTogether(), titleStyle, look.title)
            }
        } else {
            IconTile(look.icon, look.iconTint, look.tileColor, density)
            Spacer(Modifier.height(density.tileGap))
            CardLine(card.title.keepWordsTogether(), titleStyle, look.title, maxLines = 2)
        }
        Spacer(Modifier.height(density.titleGap))
        CardLine(
            card.detail.keepPhrasesTogether(),
            type.cardBody.copy(lineHeight = density.bodyLineHeight),
            look.detail,
            maxLines = 2,
        )
        if (card.guidance.isNotBlank()) {
            Spacer(Modifier.height(density.ruleGap))
            Spacer(Modifier.weight(1f))
            ActionHint(card.guidance, density)
        }
    }
}

@Composable
private fun IconTile(icon: ParkIconKind, tint: Color, container: Color, density: SummaryDensity) {
    Box(
        modifier = Modifier
            .size(density.iconTile)
            .clip(ParkLeaveShapes.tile)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        ParkIcon(icon, tint, Modifier.size(density.icon))
    }
}

/** Width of one grid cell for [contentWidth] split into [columns]. */
internal fun cardWidth(contentWidth: Dp, columns: Int, gutter: Dp): Dp =
    ((contentWidth - gutter * (columns - 1)) / columns).coerceAtLeast(0.dp)

/** Text measure inside a card, after its padding. */
internal fun cardTextWidth(contentWidth: Dp, columns: Int, density: SummaryDensity): Dp =
    (cardWidth(contentWidth, columns, density.gutter) - ParkLeaveSpace.cardPadding * 2).coerceAtLeast(0.dp)

/** Title measure, which loses the tile and its gap when the tile sits beside it. */
internal fun cardTitleWidth(contentWidth: Dp, columns: Int, density: SummaryDensity): Dp {
    val text = cardTextWidth(contentWidth, columns, density)
    return if (density.iconBesideTitle) (text - density.iconTile - density.tileGap).coerceAtLeast(0.dp) else text
}

/**
 * What the driver should do. It is not tappable, so it reads as guidance rather than a link:
 * ink instead of accent, regular weight, no arrow, set off from the state line by a hairline.
 */
@Composable
private fun ActionHint(text: String, density: SummaryDensity) {
    val colors = ParkLeaveTheme.colors
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ParkLeaveDimens.hairline)
                .background(colors.hairline),
        )
        Spacer(Modifier.height(density.hintGap))
        CardLine(
            text.keepWordsTogether(),
            ParkLeaveTheme.type.cardHint.copy(lineHeight = density.hintLineHeight),
            colors.onSurface,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    BoxedText(text, style = ParkLeaveTheme.type.sectionLabel, color = ParkLeaveTheme.colors.onSurfaceMuted)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmedPanel(state: ParkUiState, density: SummaryDensity, top: Dp) {
    val colors = ParkLeaveTheme.colors
    val note = uncheckedNote(state.report?.uncheckedLines.orEmpty())
    if (state.confirmed.isEmpty() && note == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = top)
            .clip(ParkLeaveShapes.panel)
            .background(colors.surface)
            .border(ParkLeaveDimens.hairline, colors.hairline, ParkLeaveShapes.panel)
            .padding(density.panelPadding),
    ) {
        if (state.confirmed.isNotEmpty()) {
            if (!density.labelBesideChips) {
                SectionLabel("정상으로 확인된 항목")
                Spacer(Modifier.height(ParkLeaveSpace.x1Half))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkLeaveSpace.x1Half),
                verticalArrangement = Arrangement.spacedBy(ParkLeaveSpace.x1Half),
            ) {
                if (density.labelBesideChips) {
                    Box(
                        Modifier
                            .height(density.chipHeight)
                            .padding(end = ParkLeaveSpace.x1),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        SectionLabel("정상으로 확인된 항목")
                    }
                }
                state.confirmed.forEach { label -> ConfirmedChip(label, density.chipHeight) }
            }
        }
        if (note != null) {
            if (state.confirmed.isNotEmpty()) {
                Spacer(Modifier.height(density.panelRuleGap))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ParkLeaveDimens.hairline)
                        .background(colors.hairline),
                )
                Spacer(Modifier.height(density.panelRuleGap))
            }
            UncheckedNote(note)
        }
    }
}

@Composable
private fun ConfirmedChip(label: String, height: Dp) {
    val colors = ParkLeaveTheme.colors
    Row(
        modifier = Modifier
            .height(height)
            .clip(ParkLeaveShapes.pill)
            .background(colors.surfaceMuted)
            .semantics { contentDescription = "$label 정상" }
            .padding(start = 20.dp, end = ParkLeaveSpace.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParkIcon(ParkIconKind.CHECK, colors.ok, Modifier.size(ParkLeaveDimens.chipIcon))
        Spacer(Modifier.width(ParkLeaveSpace.x1Half))
        Text(label, style = ParkLeaveTheme.type.chip, color = colors.onSurface, maxLines = 1)
    }
}

@Composable
private fun UncheckedNote(note: String) {
    val colors = ParkLeaveTheme.colors
    val type = ParkLeaveTheme.type
    val split = note.indexOf(':')
    val text = buildAnnotatedString {
        if (split < 0) {
            append(note)
        } else {
            withStyle(type.noteEmphasis.toSpanStyle().copy(color = colors.onSurfaceMuted)) {
                append(note.substring(0, split + 1))
            }
            append(note.substring(split + 1))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ParkIcon(ParkIconKind.INFO, colors.onSurfaceSubtle, Modifier.size(ParkLeaveDimens.noteIcon))
        Spacer(Modifier.width(ParkLeaveSpace.x2))
        BoxedText(text, style = type.note, color = colors.onSurfaceSubtle)
    }
}

@Composable
private fun StatusMessage(
    text: String,
    icon: ParkIconKind?,
    modifier: Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = ParkLeaveTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ParkLeaveDimens.headerHeight),
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(ParkLeaveDimens.statusTile)
                    .clip(ParkLeaveShapes.tileLarge)
                    .background(colors.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                ParkIcon(icon, colors.accent, Modifier.size(ParkLeaveDimens.statusIcon))
            }
            Spacer(Modifier.height(ParkLeaveSpace.x5))
        }
        Text(
            text.keepWordsTogether(),
            style = ParkLeaveTheme.type.status,
            color = colors.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (extra != null) {
            Spacer(Modifier.height(ParkLeaveSpace.x5))
            extra()
        }
    }
}

@Composable
private fun ManualCheckButton(onClick: () -> Unit) {
    val colors = ParkLeaveTheme.colors
    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(min = ParkLeaveDimens.buttonMinWidth)
            .heightIn(min = ParkLeaveDimens.buttonHeight),
        shape = ParkLeaveShapes.pill,
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
    ) {
        Text("지금 점검", style = ParkLeaveTheme.type.button)
    }
}

private fun headlineAccent(tone: HeadlineTone, colors: ParkLeaveColors): Color = when (tone) {
    HeadlineTone.NEUTRAL -> colors.onSurface
    HeadlineTone.CRITICAL -> colors.critical
    HeadlineTone.WARN -> colors.warn
    HeadlineTone.OK -> colors.ok
}

private fun gearLabel(state: ParkUiState): String {
    val raw = state.gear.rawValue?.toString() ?: "-"
    return "$raw ${state.gear.gear.name}"
}

@Preview(name = "확인 건", widthDp = 1682, heightDp = 1200)
@Composable
private fun IssuesWidePreview() {
    ParkLeaveTheme(darkTheme = false) {
        SummaryScreen(previewIssues(), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "모두 정상", widthDp = 1682, heightDp = 1200)
@Composable
private fun AllClearPreview() {
    ParkLeaveTheme(darkTheme = false) {
        SummaryScreen(previewAllClear(resolved = false), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "해결 후", widthDp = 1682, heightDp = 1200)
@Composable
private fun ResolvedPreview() {
    ParkLeaveTheme(darkTheme = false) {
        SummaryScreen(previewAllClear(resolved = true), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "야간 확인", widthDp = 1682, heightDp = 1200)
@Composable
private fun IssuesNightPreview() {
    ParkLeaveTheme(darkTheme = true) {
        SummaryScreen(previewIssues(), onOpenDebug = {}, onManualCheck = {})
    }
}

@Preview(name = "주행 중", widthDp = 1682, heightDp = 1200)
@Composable
private fun DrivingPreview() {
    ParkLeaveTheme(darkTheme = true) {
        SummaryScreen(previewDriving(), onOpenDebug = {}, onManualCheck = {})
    }
}

internal fun previewCardCount(
    count: Int,
    uncheckedLines: List<String> = emptyList(),
): ParkUiState {
    val hood = SummaryCard(
        SignalId.HOOD,
        CardTone.CRITICAL,
        "보닛 열림",
        "보닛이 열려 있습니다",
        resolved = false,
        guidance = "보닛을 닫아 주세요",
    )
    val rear = SummaryCard(
        SignalId.REAR_OCCUPANCY,
        CardTone.CRITICAL,
        "뒷좌석 확인",
        "2열 전체 점유",
        resolved = false,
        guidance = "뒷좌석을 확인하세요",
    )
    val more = listOf(
        rear,
        SummaryCard(
            SignalId.WINDOW,
            CardTone.WARN,
            "창문 열림",
            "2열 오른쪽 30%",
            resolved = false,
            guidance = "해당 창문을 닫아 주세요",
        ),
        SummaryCard(
            SignalId.HVAC,
            CardTone.WARN,
            "공조 켜짐",
            "공조가 켜져 있습니다",
            resolved = false,
            guidance = "공조를 꺼 주세요",
        ),
        SummaryCard(
            SignalId.EXTERIOR_LIGHT,
            CardTone.WARN,
            "외부등 켜짐",
            "미등 켜짐",
            resolved = false,
            guidance = "전조등·미등을 꺼 주세요",
        ),
        SummaryCard(
            SignalId.READING_LIGHT,
            CardTone.WARN,
            "독서등 켜짐",
            "1열·2열 양쪽",
            resolved = false,
            guidance = "독서등을 꺼 주세요",
        ),
    )
    val cards = when (count) {
        1 -> listOf(hood)
        2 -> listOf(rear, hood)
        else -> more.take(count)
    }
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
        cards = cards,
        confirmed = previewConfirmed(cards),
    )
}

private fun previewConfirmed(cards: List<SummaryCard>): List<String> {
    val blocked = cards.map { it.id }.toSet()
    val fine = listOf(
        SignalId.WINDOW to "창문",
        SignalId.DOOR_OPEN to "도어",
        SignalId.DOOR_UNLOCKED to "도어 잠금",
        SignalId.HOOD to "보닛",
        SignalId.TRUNK to "트렁크",
        SignalId.CABIN_LIGHT to "실내등",
        SignalId.READING_LIGHT to "독서등",
        SignalId.EXTERIOR_LIGHT to "외부등",
        SignalId.REAR_OCCUPANCY to "뒷좌석",
        SignalId.HVAC to "공조",
        SignalId.TIRE to "타이어",
        SignalId.LOW_SOC to "배터리",
        SignalId.LOW_FUEL to "연료",
    )
    return fine.filter { it.first !in blocked }.map { it.second }
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
        confirmed = previewConfirmed(cards),
    )
}

internal fun previewAllClear(
    resolved: Boolean,
    uncheckedLines: List<String> = emptyList(),
): ParkUiState {
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
            uncheckedLines = uncheckedLines,
        ),
        cards = cards,
        confirmed = previewConfirmed(cards),
    )
}

internal fun previewDriving(): ParkUiState = previewState(
    report = null,
    cards = emptyList(),
    confirmed = emptyList(),
).copy(
    gear = GearReading(rawValue = 8, gear = Gear.DRIVE, error = null),
    session = CheckSession(phase = SessionPhase.FROZEN, motionAllowed = false),
)

internal fun previewState(
    report: ParkingReport?,
    cards: List<SummaryCard>,
    confirmed: List<String> = previewConfirmed(cards),
): ParkUiState {
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
        confirmed = confirmed,
    )
}
