package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import io.github.kwn00.parkandleave.designsystem.ParkLeaveDimens
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import io.github.kwn00.parkandleave.designsystem.ParkLeaveType
import io.github.kwn00.parkandleave.designsystem.SummaryDensity
import io.github.kwn00.parkandleave.designsystem.keepPhrasesTogether
import io.github.kwn00.parkandleave.designsystem.keepWordsTogether
import io.github.kwn00.parkandleave.domain.SummaryCard
import io.github.kwn00.parkandleave.domain.summaryGridColumns
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.max

/**
 * Hangul draws at about 1.45 em, taller than the line-height token. A card clips its children,
 * so each card's measured text stack has to fit in the card's inner height (inside the padding)
 * for every count from 1 to 14. The pane-height test does not catch glyphs cut off by the card.
 */
class CardInnerFitTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 8,
            screenHeight = 8,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
        ),
        showSystemUi = false,
    )

    @Test
    fun textFitsInsideEveryCard() {
        val problems = mutableListOf<String>()
        paparazzi.snapshot(name = "card-inner-fit") {
            ParkLeaveTheme(darkTheme = false) {
                val measurer = rememberTextMeasurer()
                val type = ParkLeaveTheme.type
                val content = (PANE_WIDTH - 64).dp
                Box {
                    for (count in 1..MAX_CARDS) {
                        val state = ParkedScenarios.tallest(count)
                        val density = summaryDensity(count, content)
                        val columns = summaryGridColumns(content.value.toInt(), count).coerceAtLeast(1)
                        val cardWidth = cardWidth(content, columns, density.gutter)
                        state.cards.forEach { card ->
                            val contentHeight = textStackHeight(measurer, type, density, card, content, columns)
                            Layout(
                                content = { CheckCard(card, density, Modifier.width(cardWidth)) },
                            ) { measurables, _ ->
                                val widthPx = cardWidth.roundToPx()
                                val placeable = measurables.single().measure(
                                    Constraints(
                                        minWidth = widthPx,
                                        maxWidth = widthPx,
                                        maxHeight = Constraints.Infinity,
                                    ),
                                )
                                val pad = density.cardPadding.roundToPx()
                                val inner = placeable.height - pad * 2
                                if (contentHeight > inner) {
                                    problems += "$count '${card.title}' text ${contentHeight}px > inner ${inner}px"
                                }
                                layout(1, 1) { placeable.place(0, 0) }
                            }
                        }
                    }
                    Box(Modifier.size(1.dp))
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Composable
    private fun textStackHeight(
        measurer: TextMeasurer,
        type: ParkLeaveType,
        density: SummaryDensity,
        card: SummaryCard,
        content: Dp,
        columns: Int,
    ): Int {
        val titleStyle = when {
            density.denseTitle -> type.cardTitleDense
            density.compactTitle -> type.cardTitleCompact
            else -> type.cardTitle
        }
        val px = LocalDensity.current
        val titleWidth = with(px) { cardTitleWidth(content, columns, density).roundToPx() }
        val textWidth = with(px) { cardTextWidth(content, columns, density).roundToPx() }
        val title = measurer.line(card.title.keepWordsTogether(), titleStyle, titleWidth)
        val body = measurer.line(card.detail.keepPhrasesTogether(), type.cardBody.copy(lineHeight = density.bodyLineHeight), textWidth)
        val hint = if (card.guidance.isBlank()) {
            0
        } else {
            measurer.line(card.guidance.keepWordsTogether(), type.cardHint.copy(lineHeight = density.hintLineHeight), textWidth)
        }
        val icon = with(px) { density.iconTile.roundToPx() }
        val header = if (density.iconBesideTitle) {
            max(icon, title)
        } else {
            icon + with(px) { density.tileGap.roundToPx() } + title
        }
        var height = header + with(px) { density.titleGap.roundToPx() } + body
        if (hint > 0) {
            height += with(px) { density.ruleGap.roundToPx() } +
                with(px) { ParkLeaveDimens.hairline.roundToPx() } +
                with(px) { density.hintGap.roundToPx() } +
                hint
        }
        return height
    }

    private fun androidx.compose.ui.text.TextMeasurer.line(text: String, style: TextStyle, width: Int): Int =
        measure(AnnotatedString(text), style, constraints = Constraints(maxWidth = width)).size.height

    private companion object {
        const val PANE_WIDTH = 1682
        const val MAX_CARDS = 14
    }
}
