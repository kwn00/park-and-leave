package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

enum class ParkIconKind {
    OCCUPANT,
    TRUNK,
    HOOD,
    DOOR,
    TIRE,
    ALERT,
    WINDOW,
    UNLOCK,
    CABIN,
    READING,
    LAMP,
    HVAC,
    DROP,
    BATTERY,
    FUEL,
    CHECK,
    INFO,
}

@Composable
fun ParkIcon(
    kind: ParkIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = iconStroke()
        when (kind) {
            ParkIconKind.OCCUPANT -> drawOccupant(tint, stroke)
            ParkIconKind.TRUNK -> drawTrunk(tint, stroke)
            ParkIconKind.HOOD -> drawHood(tint, stroke)
            ParkIconKind.DOOR -> drawDoor(tint, stroke)
            ParkIconKind.TIRE -> drawTire(tint, stroke)
            ParkIconKind.ALERT -> drawAlert(tint, stroke)
            ParkIconKind.WINDOW -> drawWindow(tint, stroke)
            ParkIconKind.UNLOCK -> drawUnlock(tint, stroke)
            ParkIconKind.CABIN -> drawCabin(tint, stroke)
            ParkIconKind.READING -> drawReading(tint, stroke)
            ParkIconKind.LAMP -> drawLamp(tint, stroke)
            ParkIconKind.HVAC -> drawHvac(tint, stroke)
            ParkIconKind.DROP -> drawDrop(tint, stroke)
            ParkIconKind.BATTERY -> drawBattery(tint, stroke)
            ParkIconKind.FUEL -> drawFuel(tint, stroke)
            ParkIconKind.CHECK -> drawCheck(tint, stroke)
            ParkIconKind.INFO -> drawInfo(tint, stroke)
        }
    }
}

private fun DrawScope.iconStroke(): Stroke = Stroke(
    width = unit(1.75f),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

private fun DrawScope.unit(v: Float): Float = size.minDimension * (v / 24f)

private fun DrawScope.at(x: Float, y: Float): Offset {
    val span = size.minDimension
    val left = (size.width - span) / 2f
    val top = (size.height - span) / 2f
    return Offset(left + span * (x / 24f), top + span * (y / 24f))
}

private fun DrawScope.line(tint: Color, stroke: Stroke, x1: Float, y1: Float, x2: Float, y2: Float) {
    drawLine(tint, at(x1, y1), at(x2, y2), stroke.width, StrokeCap.Round)
}

private fun DrawScope.box(
    tint: Color,
    stroke: Stroke,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float = 1.5f,
) {
    drawRoundRect(
        color = tint,
        topLeft = at(left, top),
        size = Size(unit(width), unit(height)),
        cornerRadius = CornerRadius(unit(radius), unit(radius)),
        style = stroke,
    )
}

private fun DrawScope.drawOccupant(tint: Color, stroke: Stroke) {
    drawCircle(tint, unit(2.3f), at(12f, 7.1f), style = stroke)
    drawPath(
        Path().apply {
            moveTo(at(8f, 20.2f).x, at(8f, 20.2f).y)
            cubicTo(
                at(8.3f, 16.2f).x,
                at(8.3f, 16.2f).y,
                at(10f, 14.2f).x,
                at(10f, 14.2f).y,
                at(12f, 14.2f).x,
                at(12f, 14.2f).y,
            )
            cubicTo(
                at(14f, 14.2f).x,
                at(14f, 14.2f).y,
                at(15.7f, 16.2f).x,
                at(15.7f, 16.2f).y,
                at(16f, 20.2f).x,
                at(16f, 20.2f).y,
            )
        },
        tint,
        style = stroke,
    )
}

private fun DrawScope.drawTrunk(tint: Color, stroke: Stroke) {
    box(tint, stroke, 4f, 12f, 16f, 7f)
    line(tint, stroke, 5.5f, 12f, 8f, 6.2f)
    line(tint, stroke, 8f, 6.2f, 19.5f, 6.2f)
}

private fun DrawScope.drawHood(tint: Color, stroke: Stroke) {
    box(tint, stroke, 4f, 13.2f, 16f, 6.2f)
    line(tint, stroke, 5.2f, 13.2f, 8.4f, 6.2f)
    line(tint, stroke, 8.4f, 6.2f, 15.2f, 7.2f)
    line(tint, stroke, 15.2f, 7.2f, 19.2f, 13.2f)
}

private fun DrawScope.drawDoor(tint: Color, stroke: Stroke) {
    box(tint, stroke, 7.5f, 3f, 9f, 18f)
    box(tint, stroke, 9.2f, 5f, 5.6f, 6.2f, radius = 1f)
    line(tint, stroke, 13.4f, 14.6f, 15.4f, 14.6f)
}

private fun DrawScope.drawTire(tint: Color, stroke: Stroke) {
    drawCircle(tint, unit(7.4f), at(12f, 12f), style = stroke)
    drawCircle(tint, unit(2.3f), at(12f, 12f), style = stroke)
}

private fun DrawScope.drawAlert(tint: Color, stroke: Stroke) {
    drawPath(
        Path().apply {
            moveTo(at(12f, 3.6f).x, at(12f, 3.6f).y)
            lineTo(at(20.4f, 19.6f).x, at(20.4f, 19.6f).y)
            lineTo(at(3.6f, 19.6f).x, at(3.6f, 19.6f).y)
            close()
        },
        tint,
        style = stroke,
    )
    line(tint, stroke, 12f, 9.2f, 12f, 13.6f)
    drawCircle(tint, unit(1f), at(12f, 16.6f))
}

private fun DrawScope.drawWindow(tint: Color, stroke: Stroke) {
    box(tint, stroke, 3.5f, 5f, 17f, 14f)
    line(tint, stroke, 12f, 5f, 12f, 19f)
    line(tint, stroke, 3.5f, 12f, 20.5f, 12f)
}

private fun DrawScope.drawUnlock(tint: Color, stroke: Stroke) {
    box(tint, stroke, 6.5f, 11f, 11f, 8.4f)
    drawPath(
        Path().apply {
            moveTo(at(9.2f, 11f).x, at(9.2f, 11f).y)
            lineTo(at(9.2f, 8.2f).x, at(9.2f, 8.2f).y)
            quadraticTo(at(9.2f, 4.6f).x, at(9.2f, 4.6f).y, at(12.4f, 4.6f).x, at(12.4f, 4.6f).y)
            quadraticTo(at(15.6f, 4.6f).x, at(15.6f, 4.6f).y, at(15.6f, 8f).x, at(15.6f, 8f).y)
        },
        tint,
        style = stroke,
    )
}

private fun DrawScope.drawCabin(tint: Color, stroke: Stroke) {
    drawPath(
        Path().apply {
            moveTo(at(6f, 14.6f).x, at(6f, 14.6f).y)
            quadraticTo(at(12f, 6.4f).x, at(12f, 6.4f).y, at(18f, 14.6f).x, at(18f, 14.6f).y)
        },
        tint,
        style = stroke,
    )
    line(tint, stroke, 6f, 14.6f, 18f, 14.6f)
    line(tint, stroke, 5f, 18.2f, 19f, 18.2f)
}

private fun DrawScope.drawReading(tint: Color, stroke: Stroke) {
    box(tint, stroke, 6f, 6.4f, 12f, 4.4f, radius = 1f)
    line(tint, stroke, 12f, 10.8f, 12f, 18f)
    line(tint, stroke, 8f, 18f, 16f, 18f)
}

private fun DrawScope.drawLamp(tint: Color, stroke: Stroke) {
    box(tint, stroke, 3f, 9f, 10f, 6f, radius = 2f)
    line(tint, stroke, 14.4f, 10.2f, 20.6f, 8.2f)
    line(tint, stroke, 14.6f, 12f, 21f, 12f)
    line(tint, stroke, 14.4f, 13.8f, 20.6f, 15.8f)
}

private fun DrawScope.drawHvac(tint: Color, stroke: Stroke) {
    drawCircle(tint, unit(2f), at(12f, 12f), style = stroke)
    val degrees = floatArrayOf(-90f, 30f, 150f)
    for (deg in degrees) {
        val rad = deg * (Math.PI / 180.0)
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        line(tint, stroke, 12f + 3.4f * dx, 12f + 3.4f * dy, 12f + 8f * dx, 12f + 8f * dy)
    }
}

private fun DrawScope.drawDrop(tint: Color, stroke: Stroke) {
    drawPath(
        Path().apply {
            moveTo(at(12f, 3.6f).x, at(12f, 3.6f).y)
            cubicTo(
                at(12f, 3.6f).x,
                at(12f, 3.6f).y,
                at(18.2f, 10f).x,
                at(18.2f, 10f).y,
                at(18.2f, 14.2f).x,
                at(18.2f, 14.2f).y,
            )
            cubicTo(
                at(18.2f, 17.8f).x,
                at(18.2f, 17.8f).y,
                at(15.4f, 20.4f).x,
                at(15.4f, 20.4f).y,
                at(12f, 20.4f).x,
                at(12f, 20.4f).y,
            )
            cubicTo(
                at(8.6f, 20.4f).x,
                at(8.6f, 20.4f).y,
                at(5.8f, 17.8f).x,
                at(5.8f, 17.8f).y,
                at(5.8f, 14.2f).x,
                at(5.8f, 14.2f).y,
            )
            cubicTo(
                at(5.8f, 10f).x,
                at(5.8f, 10f).y,
                at(12f, 3.6f).x,
                at(12f, 3.6f).y,
                at(12f, 3.6f).x,
                at(12f, 3.6f).y,
            )
            close()
        },
        tint,
        style = stroke,
    )
}

private fun DrawScope.drawBattery(tint: Color, stroke: Stroke) {
    box(tint, stroke, 3f, 7f, 15f, 10f)
    drawRoundRect(
        color = tint,
        topLeft = at(18.2f, 10f),
        size = Size(unit(2.4f), unit(4f)),
        cornerRadius = CornerRadius(unit(0.6f), unit(0.6f)),
    )
    drawRoundRect(
        color = tint,
        topLeft = at(5.6f, 9.6f),
        size = Size(unit(6.4f), unit(4.8f)),
        cornerRadius = CornerRadius(unit(0.8f), unit(0.8f)),
    )
}

private fun DrawScope.drawFuel(tint: Color, stroke: Stroke) {
    box(tint, stroke, 4.5f, 3.2f, 8.4f, 12.6f)
    line(tint, stroke, 12.9f, 6.4f, 17.4f, 6.4f)
    line(tint, stroke, 17.4f, 6.4f, 17.4f, 11f)
    line(tint, stroke, 17.4f, 11f, 19.6f, 12.6f)
    line(tint, stroke, 8.6f, 15.8f, 8.6f, 20f)
    line(tint, stroke, 4f, 20f, 14.5f, 20f)
}

private fun DrawScope.drawCheck(tint: Color, stroke: Stroke) {
    line(tint, stroke, 5f, 12.6f, 10f, 17.6f)
    line(tint, stroke, 10f, 17.6f, 19.2f, 7f)
}

private fun DrawScope.drawInfo(tint: Color, stroke: Stroke) {
    drawCircle(tint, unit(8f), at(12f, 12f), style = stroke)
    line(tint, stroke, 12f, 10.8f, 12f, 16.4f)
    drawCircle(tint, unit(1f), at(12f, 8f))
}
