package io.github.kwn00.parkandleave.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

enum class ParkIconKind {
    OCCUPANT,
    TRUNK,
    HOOD,
    DOOR,
    TIRE,
    ALERT,
    WINDOW,
    UNLOCK,
    CABIN_LIGHT,
    READING_LIGHT,
    EXTERIOR_LIGHT,
    FAN,
    DROP,
    BATTERY,
    FUEL,
    CHECK,
    ARROW,
    INFO,
    PARK,
    STEERING,
}

/** Stroke on the 24-unit icon grid. Every glyph uses it, so weight stays even at any size. */
private const val GRID = 24f
private const val STROKE_UNITS = 2f

/** Outlined line icons drawn on a 24-unit grid, one stroke weight, round caps and joins. */
@Composable
fun ParkIcon(kind: ParkIconKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val pen = IconPen(this, tint)
        when (kind) {
            ParkIconKind.OCCUPANT -> pen.occupant()
            ParkIconKind.TRUNK -> pen.trunk()
            ParkIconKind.HOOD -> pen.hood()
            ParkIconKind.DOOR -> pen.door()
            ParkIconKind.TIRE -> pen.tire()
            ParkIconKind.ALERT -> pen.alert()
            ParkIconKind.WINDOW -> pen.window()
            ParkIconKind.UNLOCK -> pen.unlock()
            ParkIconKind.CABIN_LIGHT -> pen.cabinLight()
            ParkIconKind.READING_LIGHT -> pen.readingLight()
            ParkIconKind.EXTERIOR_LIGHT -> pen.exteriorLight()
            ParkIconKind.FAN -> pen.fan()
            ParkIconKind.DROP -> pen.drop()
            ParkIconKind.BATTERY -> pen.battery()
            ParkIconKind.FUEL -> pen.fuel()
            ParkIconKind.CHECK -> pen.check()
            ParkIconKind.ARROW -> pen.arrow()
            ParkIconKind.INFO -> pen.info()
            ParkIconKind.PARK -> pen.park()
            ParkIconKind.STEERING -> pen.steering()
        }
    }
}

private class IconPen(private val scope: DrawScope, private val tint: Color) {
    private val unit = scope.size.minDimension / GRID
    private val originX = (scope.size.width - unit * GRID) / 2f
    private val originY = (scope.size.height - unit * GRID) / 2f
    private val stroke = Stroke(width = unit * STROKE_UNITS, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun at(x: Float, y: Float) = Offset(originX + x * unit, originY + y * unit)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        scope.drawLine(tint, at(x1, y1), at(x2, y2), stroke.width, StrokeCap.Round)
    }

    fun circle(cx: Float, cy: Float, r: Float) {
        scope.drawCircle(tint, r * unit, at(cx, cy), style = stroke)
    }

    fun dot(cx: Float, cy: Float, r: Float = 1.2f) {
        scope.drawCircle(tint, r * unit, at(cx, cy))
    }

    fun box(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        scope.drawRoundRect(
            color = tint,
            topLeft = at(left, top),
            size = Size((right - left) * unit, (bottom - top) * unit),
            cornerRadius = CornerRadius(radius * unit),
            style = stroke,
        )
    }

    fun path(build: Pen.() -> Unit) {
        val path = Path()
        Pen(path).build()
        scope.drawPath(path, tint, style = stroke)
    }

    fun rotated(degrees: Float, block: () -> Unit) {
        scope.rotate(degrees, pivot = at(12f, 12f)) { block() }
    }

    inner class Pen(private val path: Path) {
        fun move(x: Float, y: Float) = at(x, y).let { path.moveTo(it.x, it.y) }
        fun line(x: Float, y: Float) = at(x, y).let { path.lineTo(it.x, it.y) }
        fun quad(cx: Float, cy: Float, x: Float, y: Float) {
            val c = at(cx, cy)
            val e = at(x, y)
            path.quadraticTo(c.x, c.y, e.x, e.y)
        }
        fun cubic(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
            val a = at(c1x, c1y)
            val b = at(c2x, c2y)
            val e = at(x, y)
            path.cubicTo(a.x, a.y, b.x, b.y, e.x, e.y)
        }
        fun arc(cx: Float, cy: Float, r: Float, startDegrees: Float, sweepDegrees: Float) {
            path.arcTo(Rect(at(cx, cy), r * unit), startDegrees, sweepDegrees, forceMoveTo = false)
        }
        fun close() = path.close()
    }

    /** Side profile facing left, wheels breaking the sill line. */
    private fun carBody() {
        path {
            move(5.25f, 16.5f)
            line(4.5f, 16.5f)
            quad(2.5f, 16.5f, 2.5f, 14.5f)
            line(2.5f, 13.5f)
            quad(2.5f, 11.5f, 4.5f, 11.5f)
            line(19.5f, 11.5f)
            quad(21.5f, 11.5f, 21.5f, 13.5f)
            line(21.5f, 14.5f)
            quad(21.5f, 16.5f, 19.5f, 16.5f)
            line(18.75f, 16.5f)
        }
        line(8.75f, 16.5f, 15.25f, 16.5f)
        circle(7f, 16.5f, 1.75f)
        circle(17f, 16.5f, 1.75f)
    }

    fun occupant() {
        circle(12f, 7.5f, 3.5f)
        path {
            move(5f, 20.5f)
            line(5f, 19.5f)
            arc(10.5f, 19.5f, 5.5f, 180f, 90f)
            line(13.5f, 14f)
            arc(13.5f, 19.5f, 5.5f, 270f, 90f)
            line(19f, 20.5f)
        }
    }

    fun hood() {
        carBody()
        path {
            move(8.75f, 11.5f)
            line(10.75f, 7.25f)
            line(16f, 7.25f)
            line(18.5f, 11.5f)
        }
        line(8.25f, 11.5f, 3f, 7.5f)
    }

    fun trunk() {
        carBody()
        path {
            move(5.5f, 11.5f)
            line(8f, 7.25f)
            line(14.25f, 7.25f)
        }
        line(14.25f, 7.25f, 21f, 5f)
    }

    fun door() {
        path {
            move(6f, 20.5f)
            line(6f, 9f)
            line(11f, 3.5f)
            line(18f, 3.5f)
            line(18f, 20.5f)
            close()
        }
        path {
            move(8.75f, 10f)
            line(11.75f, 6.5f)
            line(15.25f, 6.5f)
            line(15.25f, 10f)
            close()
        }
        line(13f, 14f, 15.25f, 14f)
    }

    /** Side window with the glass lowered: open frame above, glass edge and travel arrow below. */
    fun window() {
        path {
            move(3.5f, 13f)
            line(3.5f, 11f)
            quad(3.5f, 10f, 4.25f, 9.25f)
            line(8.75f, 4.75f)
            quad(9.5f, 4f, 10.5f, 4f)
            line(19f, 4f)
            quad(20.5f, 4f, 20.5f, 5.5f)
            line(20.5f, 13f)
        }
        box(3.5f, 13f, 20.5f, 20.5f, 1.5f)
        line(12f, 15.25f, 12f, 18.25f)
        path {
            move(10.25f, 16.75f)
            line(12f, 18.5f)
            line(13.75f, 16.75f)
        }
    }

    fun unlock() {
        box(5f, 11f, 19f, 20.5f, 2.5f)
        path {
            move(8f, 11f)
            line(8f, 7.5f)
            arc(12f, 7.5f, 4f, 180f, 150f)
        }
        line(12f, 14.75f, 12f, 16.75f)
    }

    fun cabinLight() {
        line(4f, 6.5f, 20f, 6.5f)
        path {
            move(8f, 6.5f)
            arc(12f, 6.5f, 4f, 180f, -180f)
        }
        line(12f, 14f, 12f, 17.5f)
        line(7.25f, 12.75f, 5f, 15.25f)
        line(16.75f, 12.75f, 19f, 15.25f)
    }

    fun readingLight() {
        path {
            move(9.5f, 3.5f)
            line(14.5f, 3.5f)
            line(16.75f, 9f)
            line(7.25f, 9f)
            close()
        }
        line(8.5f, 12.5f, 6.75f, 16.5f)
        line(12f, 12.5f, 12f, 16.5f)
        line(15.5f, 12.5f, 17.25f, 16.5f)
        line(4.5f, 20.5f, 19.5f, 20.5f)
    }

    fun exteriorLight() {
        path {
            move(11f, 5.5f)
            cubic(6f, 5.5f, 3.5f, 8.5f, 3.5f, 12f)
            cubic(3.5f, 15.5f, 6f, 18.5f, 11f, 18.5f)
            close()
        }
        line(14.5f, 7.5f, 20.5f, 9f)
        line(14.5f, 11.5f, 20.5f, 13f)
        line(14.5f, 15.5f, 20.5f, 17f)
    }

    fun fan() {
        circle(12f, 12f, 1.75f)
        for (turn in 0 until 3) {
            rotated(turn * 120f) {
                path {
                    move(12f, 9.25f)
                    cubic(9.25f, 7.75f, 9.25f, 3f, 12.75f, 3f)
                    cubic(16f, 3f, 16.25f, 7f, 13.5f, 9.75f)
                }
            }
        }
    }

    fun drop() {
        path {
            move(12f, 3.5f)
            cubic(12f, 3.5f, 5.5f, 10f, 5.5f, 14.5f)
            cubic(5.5f, 18.1f, 8.4f, 21f, 12f, 21f)
            cubic(15.6f, 21f, 18.5f, 18.1f, 18.5f, 14.5f)
            cubic(18.5f, 10f, 12f, 3.5f, 12f, 3.5f)
            close()
        }
    }

    fun battery() {
        box(2.5f, 7f, 19f, 17f, 2.5f)
        line(21.5f, 10.5f, 21.5f, 13.5f)
        line(6.5f, 10.5f, 6.5f, 13.5f)
    }

    fun fuel() {
        path {
            move(4.5f, 20f)
            line(4.5f, 5.5f)
            quad(4.5f, 4f, 6f, 4f)
            line(11.5f, 4f)
            quad(13f, 4f, 13f, 5.5f)
            line(13f, 20f)
        }
        line(3f, 20f, 14.5f, 20f)
        line(4.5f, 10f, 13f, 10f)
        path {
            move(13f, 8.5f)
            line(15f, 8.5f)
            quad(17f, 8.5f, 17f, 10.5f)
            line(17f, 16.5f)
            quad(17f, 18f, 18.5f, 18f)
            quad(20f, 18f, 20f, 16.5f)
            line(20f, 8.5f)
            line(17.5f, 6f)
        }
    }

    fun tire() {
        path {
            move(7.5f, 19.5f)
            cubic(5f, 17.8f, 3.5f, 15.1f, 3.5f, 12f)
            cubic(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f)
            cubic(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f)
            cubic(20.5f, 15.1f, 19f, 17.8f, 16.5f, 19.5f)
        }
        line(12f, 8f, 12f, 13f)
        dot(12f, 16.25f)
    }

    fun alert() {
        path {
            move(12f, 4f)
            line(21f, 19.5f)
            line(3f, 19.5f)
            close()
        }
        line(12f, 10f, 12f, 14f)
        dot(12f, 16.75f)
    }

    fun check() {
        path {
            move(5f, 12.5f)
            line(9.5f, 17f)
            line(19f, 7.5f)
        }
    }

    fun arrow() {
        line(5f, 12f, 19f, 12f)
        path {
            move(13f, 6f)
            line(19f, 12f)
            line(13f, 18f)
        }
    }

    fun info() {
        circle(12f, 12f, 9f)
        line(12f, 11f, 12f, 16.5f)
        dot(12f, 7.75f)
    }

    fun park() {
        box(3.5f, 3.5f, 20.5f, 20.5f, 4.5f)
        path {
            move(9.5f, 17f)
            line(9.5f, 7f)
            line(13f, 7f)
            cubic(14.9f, 7f, 16f, 8.3f, 16f, 10f)
            cubic(16f, 11.7f, 14.9f, 13f, 13f, 13f)
            line(9.5f, 13f)
        }
    }

    fun steering() {
        circle(12f, 12f, 8.5f)
        circle(12f, 12f, 2.25f)
        line(3.5f, 12f, 9.75f, 12f)
        line(14.25f, 12f, 20.5f, 12f)
        line(12f, 14.25f, 12f, 20.5f)
    }
}
