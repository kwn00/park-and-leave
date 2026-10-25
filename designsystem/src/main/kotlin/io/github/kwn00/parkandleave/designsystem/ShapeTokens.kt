package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ParkLeaveCorner = RoundedCornerShape(8.dp)

fun parkShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = ParkLeaveCorner,
    medium = ParkLeaveCorner,
    large = ParkLeaveCorner,
    extraLarge = ParkLeaveCorner,
)
