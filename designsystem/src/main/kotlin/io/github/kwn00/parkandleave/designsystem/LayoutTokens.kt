package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Spacing on an 8 dp grid. Half steps exist only for optical text offsets. */
object ParkLeaveSpace {
    val half = 4.dp
    val x1 = 8.dp
    val x2 = 16.dp
    val x3 = 24.dp
    val x4 = 32.dp
    val x5 = 40.dp
    val x6 = 48.dp
    val x8 = 64.dp
    val x10 = 80.dp

    val paneHorizontal = x5
    val paneTop = x3
    val paneBottom = x5
    val gutter = x3
    val cardPadding = x4
    val panelPadding = x4
}

object ParkLeaveShapes {
    val card: Shape = RoundedCornerShape(28.dp)
    val panel: Shape = RoundedCornerShape(28.dp)
    val tile: Shape = RoundedCornerShape(24.dp)
    val tileLarge: Shape = RoundedCornerShape(36.dp)
    val pill: Shape = CircleShape
}

object ParkLeaveDimens {
    val hairline = 1.dp
    val criticalEdge = 2.dp
    val touchTarget = 64.dp

    val iconTile = 80.dp
    val icon = 48.dp
    val hintIcon = 28.dp
    val chipHeight = 64.dp
    val chipIcon = 28.dp
    val noteIcon = 28.dp

    val headerHeight = 64.dp
    val gearPillHeight = 48.dp
    val cardMinHeight = 368.dp
    val heroBadge = 224.dp
    val heroBadgeIcon = 120.dp
    val statusTile = 144.dp
    val statusIcon = 80.dp
    val buttonHeight = 80.dp
    val buttonMinWidth = 320.dp
}
