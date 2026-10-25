package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Spacing on an 8 dp grid. Half steps exist only for optical text offsets and tight stacks. */
object ParkLeaveSpace {
    val half = 4.dp
    val x1 = 8.dp
    val x1Half = 12.dp
    val x2 = 16.dp
    val x3 = 24.dp
    val x4 = 32.dp
    val x5 = 40.dp
    val x6 = 48.dp
    val x8 = 64.dp
    val x10 = 80.dp

    val paneHorizontal = x4
    val paneTop = x3
    val paneBottom = x3
    val gutter = x2
    /** Leaves a four-column cell 352 dp of text, enough for every action hint on one line. */
    val cardPadding = 20.dp
    val panelPadding = x3
}

object ParkLeaveShapes {
    val card: Shape = RoundedCornerShape(28.dp)
    val panel: Shape = RoundedCornerShape(28.dp)
    val tile: Shape = RoundedCornerShape(20.dp)
    val tileLarge: Shape = RoundedCornerShape(36.dp)
    val pill: Shape = CircleShape
}

object ParkLeaveDimens {
    val hairline = 1.dp
    val criticalEdge = 2.dp
    val touchTarget = 64.dp

    val iconTile = 64.dp
    val icon = 40.dp
    val chipHeight = 56.dp
    val chipIcon = 26.dp
    val noteIcon = 26.dp

    val headerHeight = 64.dp
    val gearPillHeight = 48.dp
    val cardMinHeight = 288.dp
    val heroBadge = 224.dp
    val heroBadgeIcon = 120.dp
    val statusTile = 144.dp
    val statusIcon = 80.dp
    val buttonHeight = 80.dp
    val buttonMinWidth = 320.dp
}
