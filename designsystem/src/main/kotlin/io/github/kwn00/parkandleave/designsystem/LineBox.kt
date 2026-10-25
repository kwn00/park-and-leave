package io.github.kwn00.parkandleave.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

/**
 * Lays a text block out at exactly [lineHeight] per line.
 *
 * Hangul is drawn by the CJK fallback font, and the text layout pads the first line's top
 * and the last line's bottom out to that font's 1.45 em extent so no glyph can clip. That
 * padding is empty space above and below the glyphs, about 14 dp on a 48 sp title. This
 * trims it evenly from both ends so spacing tokens measure the gap between line boxes.
 * Glyphs still draw in full; only the reported bounds shrink, and the draw extends past
 * the reported box. Do not use this inside a clipped card: Hangul fills that extension,
 * and the clip cuts the bottoms off. A block already no taller than its line boxes,
 * such as Latin-only text, is left as it is.
 */
fun Modifier.lineBox(lineHeight: TextUnit): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val box = lineHeight.toPx()
    val lines = (placeable.height / box).roundToInt().coerceAtLeast(1)
    val height = minOf(placeable.height, (lines * box).roundToInt())
        .coerceIn(constraints.minHeight, constraints.maxHeight)
    val top = (placeable.height - height) / 2
    layout(placeable.width, height) { placeable.place(0, -top) }
}
