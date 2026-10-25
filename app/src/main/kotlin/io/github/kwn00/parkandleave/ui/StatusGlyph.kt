package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.kwn00.parkandleave.domain.SignalId

enum class GlyphKind {
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
}

fun glyphFor(id: SignalId): GlyphKind = when (id) {
    SignalId.REAR_OCCUPANCY -> GlyphKind.OCCUPANT
    SignalId.TRUNK -> GlyphKind.TRUNK
    SignalId.HOOD -> GlyphKind.HOOD
    SignalId.DOOR_OPEN -> GlyphKind.DOOR
    SignalId.TIRE -> GlyphKind.TIRE
    SignalId.TELLTALE -> GlyphKind.ALERT
    SignalId.WINDOW -> GlyphKind.WINDOW
    SignalId.DOOR_UNLOCKED -> GlyphKind.UNLOCK
    SignalId.CABIN_LIGHT -> GlyphKind.CABIN
    SignalId.READING_LIGHT -> GlyphKind.READING
    SignalId.EXTERIOR_LIGHT -> GlyphKind.LAMP
    SignalId.HVAC -> GlyphKind.HVAC
    SignalId.WASHER -> GlyphKind.DROP
    SignalId.LOW_SOC -> GlyphKind.BATTERY
    SignalId.LOW_FUEL -> GlyphKind.FUEL
}

@Composable
fun StatusGlyph(kind: GlyphKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        when (kind) {
            GlyphKind.OCCUPANT -> drawOccupant(tint, stroke)
            GlyphKind.TRUNK -> drawTrunk(tint, stroke)
            GlyphKind.HOOD -> drawHood(tint, stroke)
            GlyphKind.DOOR -> drawDoor(tint, stroke)
            GlyphKind.TIRE -> drawTire(tint, stroke)
            GlyphKind.ALERT -> drawAlert(tint, stroke)
            GlyphKind.WINDOW -> drawWindow(tint, stroke)
            GlyphKind.UNLOCK -> drawUnlock(tint, stroke)
            GlyphKind.CABIN -> drawCabin(tint, stroke)
            GlyphKind.READING -> drawReading(tint, stroke)
            GlyphKind.LAMP -> drawLamp(tint, stroke)
            GlyphKind.HVAC -> drawHvac(tint, stroke)
            GlyphKind.DROP -> drawDrop(tint)
            GlyphKind.BATTERY -> drawBattery(tint, stroke)
            GlyphKind.FUEL -> drawFuel(tint, stroke)
            GlyphKind.CHECK -> drawCheck(tint, stroke)
        }
    }
}

private fun DrawScope.drawOccupant(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.28f), style = stroke)
    drawPath(
        Path().apply {
            moveTo(w * 0.22f, h * 0.84f)
            quadraticTo(w * 0.28f, h * 0.52f, w * 0.5f, h * 0.52f)
            quadraticTo(w * 0.72f, h * 0.52f, w * 0.78f, h * 0.84f)
        },
        tint,
        style = stroke,
    )
}

private fun DrawScope.drawTrunk(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        tint,
        topLeft = Offset(w * 0.16f, h * 0.42f),
        size = Size(w * 0.68f, h * 0.36f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = stroke,
    )
    drawLine(tint, Offset(w * 0.2f, h * 0.42f), Offset(w * 0.32f, h * 0.22f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.32f, h * 0.22f), Offset(w * 0.78f, h * 0.22f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawHood(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        tint,
        topLeft = Offset(w * 0.16f, h * 0.5f),
        size = Size(w * 0.68f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = stroke,
    )
    drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.32f, h * 0.2f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.32f, h * 0.2f), Offset(w * 0.74f, h * 0.28f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.74f, h * 0.28f), Offset(w * 0.8f, h * 0.5f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawDoor(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        tint,
        topLeft = Offset(w * 0.28f, h * 0.14f),
        size = Size(w * 0.36f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = stroke,
    )
    drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.54f, h * 0.5f))
}

private fun DrawScope.drawTire(tint: Color, stroke: Stroke) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(tint, radius = size.minDimension * 0.36f, center = c, style = stroke)
    drawCircle(tint, radius = size.minDimension * 0.14f, center = c, style = stroke)
}

private fun DrawScope.drawAlert(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawPath(
        Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.86f, h * 0.82f)
            lineTo(w * 0.14f, h * 0.82f)
            close()
        },
        tint,
        style = stroke,
    )
    drawLine(tint, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.58f), stroke.width, StrokeCap.Round)
    drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.7f))
}

private fun DrawScope.drawWindow(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    val left = w * 0.14f
    val top = h * 0.2f
    drawRoundRect(
        tint,
        topLeft = Offset(left, top),
        size = Size(w * 0.72f, h * 0.6f),
        cornerRadius = CornerRadius(w * 0.05f),
        style = stroke,
    )
    drawLine(tint, Offset(w * 0.5f, top), Offset(w * 0.5f, top + h * 0.6f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(left, h * 0.5f), Offset(left + w * 0.72f, h * 0.5f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawUnlock(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        tint,
        topLeft = Offset(w * 0.24f, h * 0.44f),
        size = Size(w * 0.52f, h * 0.38f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = stroke,
    )
    drawPath(
        Path().apply {
            moveTo(w * 0.36f, h * 0.44f)
            lineTo(w * 0.36f, h * 0.32f)
            quadraticTo(w * 0.36f, h * 0.16f, w * 0.54f, h * 0.16f)
            quadraticTo(w * 0.7f, h * 0.16f, w * 0.7f, h * 0.3f)
        },
        tint,
        style = stroke,
    )
}

private fun DrawScope.drawCabin(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.42f), style = stroke)
    drawLine(tint, Offset(w * 0.5f, h * 0.16f), Offset(w * 0.5f, h * 0.24f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.3f, h * 0.34f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.78f, h * 0.28f), Offset(w * 0.7f, h * 0.34f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.32f, h * 0.78f), Offset(w * 0.68f, h * 0.78f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawReading(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawLine(tint, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.62f, h * 0.28f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.42f, h * 0.28f), Offset(w * 0.42f, h * 0.62f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.28f, h * 0.78f), Offset(w * 0.72f, h * 0.78f), stroke.width, StrokeCap.Round)
    drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.68f, h * 0.46f))
}

private fun DrawScope.drawLamp(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawPath(
        Path().apply {
            moveTo(w * 0.18f, h * 0.32f)
            lineTo(w * 0.48f, h * 0.32f)
            lineTo(w * 0.78f, h * 0.5f)
            lineTo(w * 0.48f, h * 0.68f)
            lineTo(w * 0.18f, h * 0.68f)
            close()
        },
        tint,
        style = stroke,
    )
    drawLine(tint, Offset(w * 0.82f, h * 0.4f), Offset(w * 0.9f, h * 0.34f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.82f, h * 0.6f), Offset(w * 0.9f, h * 0.66f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawHvac(tint: Color, stroke: Stroke) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(tint, radius = size.minDimension * 0.1f, center = c, style = stroke)
    val w = size.width
    val h = size.height
    drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.36f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.22f, h * 0.68f), Offset(w * 0.38f, h * 0.56f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.78f, h * 0.68f), Offset(w * 0.62f, h * 0.56f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawDrop(tint: Color) {
    val w = size.width
    val h = size.height
    drawPath(
        Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            quadraticTo(w * 0.86f, h * 0.48f, w * 0.5f, h * 0.86f)
            quadraticTo(w * 0.14f, h * 0.48f, w * 0.5f, h * 0.12f)
            close()
        },
        tint,
    )
}

private fun DrawScope.drawBattery(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        tint,
        topLeft = Offset(w * 0.16f, h * 0.28f),
        size = Size(w * 0.6f, h * 0.46f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = stroke,
    )
    drawRect(tint, topLeft = Offset(w * 0.76f, h * 0.4f), size = Size(w * 0.08f, h * 0.2f))
    drawRect(tint, topLeft = Offset(w * 0.26f, h * 0.4f), size = Size(w * 0.22f, h * 0.22f))
}

private fun DrawScope.drawFuel(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        tint,
        topLeft = Offset(w * 0.22f, h * 0.22f),
        size = Size(w * 0.34f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = stroke,
    )
    drawLine(tint, Offset(w * 0.56f, h * 0.34f), Offset(w * 0.74f, h * 0.34f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.74f, h * 0.34f), Offset(w * 0.74f, h * 0.58f), stroke.width, StrokeCap.Round)
    drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.66f), style = stroke)
}

private fun DrawScope.drawCheck(tint: Color, stroke: Stroke) {
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = size.minDimension * 0.38f, center = Offset(w / 2f, h / 2f), style = stroke)
    drawLine(tint, Offset(w * 0.3f, h * 0.52f), Offset(w * 0.44f, h * 0.66f), stroke.width, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.44f, h * 0.66f), Offset(w * 0.72f, h * 0.36f), stroke.width, StrokeCap.Round)
}
