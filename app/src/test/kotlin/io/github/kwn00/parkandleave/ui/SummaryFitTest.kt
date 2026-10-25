package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

/**
 * The summary has no scroll container, so its natural height must fit the 1682 x 1200 dp
 * app pane for every card count up to fourteen, with the tallest content each count allows.
 * The snapshot is a contact sheet of all fourteen panes, each cut at the pane height.
 */
class SummaryFitTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = PANE_WIDTH,
            screenHeight = PANE_HEIGHT,
            xdpi = 160,
            ydpi = 160,
            orientation = ScreenOrientation.LANDSCAPE,
            density = Density.MEDIUM,
        ),
        showSystemUi = false,
    )

    @Test
    fun issuesFitThePane() = assertFits("summary-fit-issues", ParkedScenarios::tallest)

    @Test
    fun resolvedCardsFitThePane() = assertFits("summary-fit-resolved", ParkedScenarios::resolved)

    private fun assertFits(name: String, state: (Int) -> ParkUiState) {
        val states = (1..MAX_CARDS).associateWith(state)
        states.forEach { (count, ui) -> assertEquals("cards for $count", count, ui.cards.size) }
        val heights = sortedMapOf<Int, Int>()
        paparazzi.snapshot(name = name) {
            ParkLeaveTheme(darkTheme = false) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF7A7F87))
                        .padding(GAP.dp),
                    verticalArrangement = Arrangement.spacedBy(GAP.dp),
                ) {
                    states.entries.chunked(COLUMNS).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
                            row.forEach { (count, ui) -> Thumbnail(ui) { heights[count] = it } }
                        }
                    }
                }
            }
        }
        assertEquals("every pane measured", MAX_CARDS, heights.size)
        val over = heights.filterValues { it > PANE_HEIGHT }
        assertTrue("taller than the ${PANE_HEIGHT}dp pane: $over (all: $heights)", over.isEmpty())
    }

    /** Lays the screen out at the real pane width with no height cap, then shows it scaled down. */
    @Composable
    private fun Thumbnail(state: ParkUiState, onMeasured: (Int) -> Unit) {
        Layout(
            content = { SummaryScreen(state, onOpenDebug = {}, onManualCheck = {}) },
            modifier = Modifier.clipToBounds(),
        ) { measurables, _ ->
            val width = PANE_WIDTH.dp.roundToPx()
            val screen = measurables.single().measure(Constraints(minWidth = width, maxWidth = width))
            onMeasured(screen.height.toDp().value.roundToInt())
            layout((width * SCALE).roundToInt(), (PANE_HEIGHT.dp.toPx() * SCALE).roundToInt()) {
                screen.placeWithLayer(0, 0) {
                    scaleX = SCALE
                    scaleY = SCALE
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }

    private companion object {
        const val PANE_WIDTH = 1682
        const val PANE_HEIGHT = 1200
        const val MAX_CARDS = 14
        const val COLUMNS = 4
        const val GAP = 6

        /**
         * Thumbnails of the real pane, small enough that four rows and the gaps stay inside
         * the snapshot frame. 0.245 clipped the last row.
         */
        const val SCALE = 0.24f
    }
}
