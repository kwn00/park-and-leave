package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import com.android.resources.Density
import io.github.kwn00.parkandleave.designsystem.ParkLeaveSpace
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import io.github.kwn00.parkandleave.designsystem.SummaryDensity
import io.github.kwn00.parkandleave.designsystem.keepPhrasesTogether
import io.github.kwn00.parkandleave.designsystem.keepWordsTogether
import io.github.kwn00.parkandleave.domain.SummaryCard
import io.github.kwn00.parkandleave.domain.VehicleSnapshot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Every card string the evaluator can produce, laid out in a four-column cell of the
 * 1682dp app pane with the layoutlib fonts. Hints stay on one line, titles on one (also
 * at the compact size beside the tile), and bodies on one line for a single place and
 * at most two otherwise. The regular band has the widest gutter, so its cell is the narrowest.
 */
class CardTextFitTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = PANE_WIDTH,
            screenHeight = 3200,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
        ),
        renderingMode = RenderingMode.SHRINK,
        showSystemUi = false,
    )

    @Test
    fun onePlaceEach() = check("card-fit-one-place", listOf(ParkedScenarios.everyItemOnePlace), maxBodyLines = 1)

    @Test
    fun manyPlaces() = check("card-fit-many-places", ParkedScenarios.everyItemManyPlaces, maxBodyLines = 2)

    private fun check(name: String, snapshots: List<VehicleSnapshot>, maxBodyLines: Int) {
        val cards = snapshots
            .flatMap { ParkedScenarios.state(it).cards }
            .distinctBy { Triple(it.title, it.detail, it.guidance) }
        val measured = linkedMapOf<SummaryCard, CardLines>()
        val content = PANE_WIDTH.dp - ParkLeaveSpace.paneHorizontal * 2
        paparazzi.snapshot(name = name) {
            ParkLeaveTheme(darkTheme = false) {
                val measurer = rememberTextMeasurer()
                val type = ParkLeaveTheme.type
                val px = LocalDensity.current
                val width = with(px) { cardTextWidth(content, COLUMNS, SummaryDensity.Regular).toPx().toInt() }
                val besideTile = with(px) { cardTitleWidth(content, COLUMNS, SummaryDensity.Dense).toPx().toInt() }
                cards.forEach { card ->
                    val title = AnnotatedString(card.title.keepWordsTogether())
                    measured[card] = CardLines(
                        width = width,
                        titleWidth = besideTile,
                        title = measurer.lines(title, type.cardTitle, width),
                        compactTitle = measurer.lines(title, type.cardTitleCompact, besideTile),
                        body = measurer.lines(AnnotatedString(card.detail.keepPhrasesTogether()), type.cardBody, width),
                        hint = measurer.lines(AnnotatedString(card.guidance.keepWordsTogether()), type.cardHint, width),
                    )
                }
                Box(
                    Modifier
                        .background(ParkLeaveTheme.colors.background)
                        .padding(ParkLeaveSpace.paneHorizontal),
                ) {
                    CardGrid(cards, content, SummaryDensity.Regular)
                }
            }
        }

        assertTrue("no cards were built", cards.size >= 5)
        val problems = measured.flatMap { (card, lines) ->
            buildList {
                if (lines.title.count != 1) add("title '${card.title}' ${lines.title.describe(lines.width)}")
                if (lines.compactTitle.count != 1) {
                    add("title beside tile '${card.title}' ${lines.compactTitle.describe(lines.titleWidth)}")
                }
                if (lines.body.count > maxBodyLines) add("body '${card.detail}' ${lines.body.describe(lines.width)}")
                if (lines.hint.count != 1) add("hint '${card.guidance}' ${lines.hint.describe(lines.width)}")
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private data class Measured(val count: Int, val naturalWidth: Int) {
        fun describe(width: Int) = "takes $count lines (needs ${naturalWidth}px, cell ${width}px)"
    }

    private data class CardLines(
        val width: Int,
        val titleWidth: Int,
        val title: Measured,
        val compactTitle: Measured,
        val body: Measured,
        val hint: Measured,
    )

    private fun TextMeasurer.lines(text: AnnotatedString, style: TextStyle, width: Int): Measured {
        val wrapped = measure(text, style, constraints = Constraints(maxWidth = width))
        val natural = measure(text, style, softWrap = false)
        return Measured(wrapped.lineCount, natural.size.width)
    }

    private companion object {
        const val PANE_WIDTH = 1682
        const val COLUMNS = 4
    }
}
