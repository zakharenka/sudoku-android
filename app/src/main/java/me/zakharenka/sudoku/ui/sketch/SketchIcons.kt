package me.zakharenka.sudoku.ui.sketch

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class SketchIcon {
    UNDO,
    ERASE,
    NOTES,
    HINT,
    PAUSE,
    PLAY,
    REFRESH,
    TROPHY,
    INFO,
    SETTINGS,
    CLOCK,
    CLOSE,
}

@Composable
fun SketchIcon(
    icon: SketchIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.8.dp,
) {
    Canvas(modifier) {
        val side = min(size.width, size.height)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val stroke = strokeWidth.toPx()

        fun point(x: Float, y: Float): Offset =
            Offset(centerX + x * side, centerY + y * side)

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, seed: Int, width: Float = stroke) {
            sketchLine(point(x1, y1), point(x2, y2), tint, width, seed, wobble = 0.8f)
        }

        fun circle(radius: Float, seed: Int, width: Float = stroke) {
            sketchOval(
                center = point(0f, 0f),
                radiusX = radius * side,
                radiusY = radius * side,
                color = tint,
                strokeWidth = width,
                seed = seed,
                wobble = 0.5f,
            )
        }

        when (icon) {
            SketchIcon.UNDO -> {
                val path = Path().apply {
                    val start = point(-0.25f, 0.08f)
                    moveTo(start.x, start.y)
                    val c1 = point(-0.18f, -0.28f)
                    val c2 = point(0.22f, -0.28f)
                    val end = point(0.25f, 0.08f)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
                    val c3 = point(0.27f, 0.28f)
                    val c4 = point(0.08f, 0.32f)
                    val tail = point(-0.05f, 0.26f)
                    cubicTo(c3.x, c3.y, c4.x, c4.y, tail.x, tail.y)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                line(-0.25f, 0.08f, -0.1f, -0.04f, 11)
                line(-0.25f, 0.08f, -0.12f, 0.18f, 12)
            }

            SketchIcon.ERASE -> {
                val path = Path().apply {
                    val first = point(-0.28f, 0.1f)
                    moveTo(first.x, first.y)
                    listOf(
                        point(-0.05f, -0.24f),
                        point(0.28f, -0.02f),
                        point(0.05f, 0.3f),
                    ).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                line(-0.13f, 0.03f, 0.12f, 0.2f, 13)
            }

            SketchIcon.NOTES -> {
                line(-0.22f, 0.24f, 0.22f, -0.22f, 14, stroke * 1.5f)
                line(-0.3f, 0.3f, -0.22f, 0.24f, 15)
                line(-0.3f, 0.3f, -0.14f, 0.3f, 16)
                line(-0.14f, 0.3f, -0.22f, 0.24f, 17)
            }

            SketchIcon.HINT -> {
                sketchOval(
                    center = point(0f, -0.08f),
                    radiusX = 0.16f * side,
                    radiusY = 0.19f * side,
                    color = tint,
                    strokeWidth = stroke,
                    seed = 18,
                    wobble = 0.6f,
                )
                line(-0.09f, 0.16f, 0.09f, 0.16f, 19)
                line(-0.06f, 0.23f, 0.06f, 0.23f, 20)
                line(-0.03f, 0.3f, 0.03f, 0.3f, 21)
            }

            SketchIcon.PAUSE -> {
                line(-0.12f, -0.24f, -0.12f, 0.24f, 22, stroke * 1.8f)
                line(0.12f, -0.24f, 0.12f, 0.24f, 23, stroke * 1.8f)
            }

            SketchIcon.PLAY -> {
                val path = Path().apply {
                    val first = point(-0.14f, -0.24f)
                    moveTo(first.x, first.y)
                    val second = point(-0.14f, 0.24f)
                    lineTo(second.x, second.y)
                    val third = point(0.24f, 0f)
                    lineTo(third.x, third.y)
                    close()
                }
                drawPath(path = path, color = tint)
            }

            SketchIcon.REFRESH -> {
                val radius = 0.24f * side
                val startAngle = -55f
                drawArc(
                    color = tint,
                    startAngle = startAngle,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                val angle = startAngle * PI / 180.0
                val tip = Offset(
                    centerX + cos(angle).toFloat() * radius,
                    centerY + sin(angle).toFloat() * radius,
                )
                sketchLine(tip, tip + Offset(-0.12f * side, -0.03f * side), tint, stroke, 24, 0.5f)
                sketchLine(tip, tip + Offset(-0.03f * side, 0.12f * side), tint, stroke, 25, 0.5f)
            }

            SketchIcon.TROPHY -> {
                val path = Path().apply {
                    val start = point(-0.18f, -0.2f)
                    moveTo(start.x, start.y)
                    lineTo(point(-0.18f, 0f).x, point(-0.18f, 0f).y)
                    val c1 = point(-0.1f, 0.22f)
                    val c2 = point(0.1f, 0.22f)
                    val end = point(0.18f, 0f)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
                    lineTo(point(0.18f, -0.2f).x, point(0.18f, -0.2f).y)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                line(-0.18f, -0.12f, -0.3f, -0.08f, 26)
                line(-0.3f, -0.08f, -0.18f, 0.02f, 27)
                line(0.18f, -0.12f, 0.3f, -0.08f, 28)
                line(0.3f, -0.08f, 0.18f, 0.02f, 29)
                line(0f, 0.22f, 0f, 0.32f, 30)
                line(-0.12f, 0.34f, 0.12f, 0.34f, 31)
            }

            SketchIcon.INFO -> {
                circle(0.28f, 32)
                drawCircle(tint, radius = 0.03f * side, center = point(0f, -0.12f))
                line(0f, -0.02f, 0f, 0.16f, 33, stroke * 1.2f)
            }

            SketchIcon.SETTINGS -> {
                circle(0.22f, 34)
                circle(0.09f, 35)
                repeat(8) { index ->
                    val angle = index * PI / 4.0
                    val innerX = cos(angle).toFloat() * 0.22f
                    val innerY = sin(angle).toFloat() * 0.22f
                    val outerX = cos(angle).toFloat() * 0.32f
                    val outerY = sin(angle).toFloat() * 0.32f
                    line(innerX, innerY, outerX, outerY, 36 + index, stroke * 1.3f)
                }
            }

            SketchIcon.CLOCK -> {
                circle(0.28f, 44)
                line(0f, 0f, 0f, -0.16f, 45, stroke * 1.2f)
                line(0f, 0f, 0.12f, 0.06f, 46, stroke * 1.2f)
            }

            SketchIcon.CLOSE -> {
                line(-0.2f, -0.2f, 0.2f, 0.2f, 47)
                line(-0.2f, 0.2f, 0.2f, -0.2f, 48)
            }
        }
    }
}
