package me.zakharenka.sudoku.ui.sketch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.min
import kotlin.random.Random

/**
 * One point of a digit stroke in a normalized 0..1 box.
 */
internal data class DigitPoint(
    val x: Float,
    val y: Float,
)

/**
 * Center lines for the handwritten digits 1–9. Each digit is a list of strokes;
 * strokes are revealed one after another during the writing animation.
 */
internal val HandwrittenDigitStrokes: Map<Int, List<List<DigitPoint>>> = mapOf(
    1 to listOf(
        join(
            line(0.40f, 0.28f, 0.49f, 0.18f, steps = 4),
            line(0.49f, 0.18f, 0.49f, 0.80f, steps = 14),
        ),
        line(0.30f, 0.80f, 0.68f, 0.80f, steps = 8),
    ),
    2 to listOf(
        join(
            cubic(0.28f, 0.34f, 0.34f, 0.14f, 0.64f, 0.12f, 0.72f, 0.30f),
            cubic(0.72f, 0.30f, 0.74f, 0.42f, 0.64f, 0.48f, 0.56f, 0.56f),
            line(0.56f, 0.56f, 0.30f, 0.80f, steps = 10),
            line(0.30f, 0.80f, 0.74f, 0.80f, steps = 10),
        ),
    ),
    3 to listOf(
        join(
            cubic(0.28f, 0.24f, 0.34f, 0.10f, 0.66f, 0.10f, 0.72f, 0.28f),
            cubic(0.72f, 0.28f, 0.76f, 0.40f, 0.60f, 0.46f, 0.52f, 0.50f),
            cubic(0.52f, 0.50f, 0.64f, 0.52f, 0.76f, 0.60f, 0.72f, 0.74f),
            cubic(0.72f, 0.74f, 0.66f, 0.88f, 0.36f, 0.90f, 0.26f, 0.76f),
        ),
    ),
    4 to listOf(
        join(
            line(0.62f, 0.16f, 0.28f, 0.56f, steps = 12),
            line(0.28f, 0.56f, 0.78f, 0.56f, steps = 12),
        ),
        line(0.62f, 0.16f, 0.62f, 0.84f, steps = 14),
    ),
    5 to listOf(
        join(
            line(0.72f, 0.16f, 0.32f, 0.16f, steps = 12),
            line(0.32f, 0.16f, 0.30f, 0.44f, steps = 8),
            cubic(0.30f, 0.44f, 0.50f, 0.36f, 0.72f, 0.40f, 0.76f, 0.56f),
            cubic(0.76f, 0.56f, 0.78f, 0.72f, 0.60f, 0.86f, 0.40f, 0.84f),
            cubic(0.40f, 0.84f, 0.34f, 0.84f, 0.28f, 0.82f, 0.24f, 0.78f),
        ),
    ),
    6 to listOf(
        join(
            cubic(0.68f, 0.18f, 0.48f, 0.26f, 0.32f, 0.44f, 0.28f, 0.60f),
            cubic(0.28f, 0.60f, 0.24f, 0.76f, 0.36f, 0.88f, 0.52f, 0.86f),
            cubic(0.52f, 0.86f, 0.70f, 0.84f, 0.76f, 0.70f, 0.70f, 0.56f),
            cubic(0.70f, 0.56f, 0.62f, 0.42f, 0.40f, 0.42f, 0.32f, 0.56f),
        ),
    ),
    7 to listOf(
        line(0.26f, 0.18f, 0.74f, 0.18f, steps = 12),
        line(0.74f, 0.18f, 0.40f, 0.84f, steps = 14),
    ),
    8 to listOf(
        join(
            cubic(0.50f, 0.44f, 0.32f, 0.42f, 0.26f, 0.30f, 0.32f, 0.20f),
            cubic(0.32f, 0.20f, 0.38f, 0.10f, 0.62f, 0.10f, 0.68f, 0.22f),
            cubic(0.68f, 0.22f, 0.74f, 0.34f, 0.62f, 0.44f, 0.50f, 0.44f),
        ),
        join(
            cubic(0.50f, 0.44f, 0.32f, 0.44f, 0.24f, 0.58f, 0.30f, 0.70f),
            cubic(0.30f, 0.70f, 0.36f, 0.84f, 0.64f, 0.86f, 0.70f, 0.72f),
            cubic(0.70f, 0.72f, 0.76f, 0.58f, 0.64f, 0.46f, 0.50f, 0.44f),
        ),
    ),
    9 to listOf(
        join(
            cubic(0.50f, 0.46f, 0.30f, 0.44f, 0.26f, 0.28f, 0.34f, 0.18f),
            cubic(0.34f, 0.18f, 0.42f, 0.08f, 0.64f, 0.10f, 0.70f, 0.24f),
            cubic(0.70f, 0.24f, 0.76f, 0.38f, 0.64f, 0.46f, 0.50f, 0.46f),
        ),
        cubic(0.70f, 0.30f, 0.68f, 0.50f, 0.60f, 0.70f, 0.46f, 0.86f),
    ),
)

private fun line(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    steps: Int,
): List<DigitPoint> = (0..steps).map { step ->
    val t = step.toFloat() / steps
    DigitPoint(
        x = x1 + (x2 - x1) * t,
        y = y1 + (y2 - y1) * t,
    )
}

private fun cubic(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float,
    steps: Int = 16,
): List<DigitPoint> = (0..steps).map { step ->
    val t = step.toFloat() / steps
    val oneMinusT = 1f - t
    val a = oneMinusT * oneMinusT * oneMinusT
    val b = 3f * oneMinusT * oneMinusT * t
    val c = 3f * oneMinusT * t * t
    val d = t * t * t
    DigitPoint(
        x = a * x0 + b * x1 + c * x2 + d * x3,
        y = a * y0 + b * y1 + c * y2 + d * y3,
    )
}

private fun join(vararg segments: List<DigitPoint>): List<DigitPoint> =
    segments.flatMapIndexed { index, segment ->
        if (index == 0) segment else segment.drop(1)
    }

/**
 * Draws [value] as a hand-written pencil digit. The first appearance is drawn
 * instantly; later changes animate the strokes. A change to zero erases the
 * previous digit in reverse.
 */
@Composable
fun HandwrittenDigit(
    value: Int,
    color: Color,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    var displayedValue by remember { mutableIntStateOf(value) }
    val progress = remember { Animatable(if (value == 0) 0f else 1f) }

    LaunchedEffect(value) {
        if (value == displayedValue) {
            when {
                value != 0 && progress.value < 1f -> {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 280,
                            easing = LinearEasing,
                        ),
                    )
                }

                value == 0 && progress.value > 0f -> {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 150,
                            easing = LinearEasing,
                        ),
                    )
                    displayedValue = 0
                    progress.snapTo(0f)
                }
            }
            return@LaunchedEffect
        }

        if (displayedValue != 0 && progress.value > 0f) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 150,
                    easing = LinearEasing,
                ),
            )
        }

        if (value == 0) {
            displayedValue = 0
            progress.snapTo(0f)
        } else {
            displayedValue = value
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 280,
                    easing = LinearEasing,
                ),
            )
        }
    }

    if (displayedValue == 0) {
        return
    }

    val geometry = remember(displayedValue, seed) {
        buildDigitGeometry(displayedValue, seed)
    }

    Canvas(modifier) {
        if (geometry.strokes.isEmpty()) {
            return@Canvas
        }

        val side = min(size.width, size.height)
        if (side <= 0f) {
            return@Canvas
        }

        val box = side * 0.66f
        val left = (size.width - box) / 2f
        val top = (size.height - box) / 2f

        withTransform({
            translate(left, top)
            scale(box, box, pivot = Offset.Zero)
        }) {
            drawDigitGeometry(
                geometry = geometry,
                progress = progress.value,
                color = color,
            )
        }
    }
}

private class StrokeGeometry(
    val path: Path,
    val measure: PathMeasure,
    val length: Float,
)

private class DigitGeometry(
    val strokes: List<StrokeGeometry>,
    val totalLength: Float,
)

private fun buildDigitGeometry(value: Int, seed: Int): DigitGeometry {
    val strokes = HandwrittenDigitStrokes[value].orEmpty()
    val random = Random(seed * 31 + value * 1009)
    val result = mutableListOf<StrokeGeometry>()
    var totalLength = 0f

    for (source in strokes) {
        if (source.size < 2) {
            continue
        }

        val path = Path()
        source.forEachIndexed { index, point ->
            val jitterX = (random.nextFloat() * 2f - 1f) * 0.006f
            val jitterY = (random.nextFloat() * 2f - 1f) * 0.006f
            val x = point.x + jitterX
            val y = point.y + jitterY

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        val measure = PathMeasure().apply { setPath(path, false) }
        if (measure.length <= 0f) {
            continue
        }

        result += StrokeGeometry(
            path = path,
            measure = measure,
            length = measure.length,
        )
        totalLength += measure.length
    }

    return DigitGeometry(strokes = result, totalLength = totalLength)
}

private fun DrawScope.drawDigitGeometry(
    geometry: DigitGeometry,
    progress: Float,
    color: Color,
) {
    val clamped = progress.coerceIn(0f, 1f)
    if (clamped <= 0f || geometry.totalLength <= 0f) {
        return
    }

    val target = geometry.totalLength * clamped
    var consumed = 0f
    val mainStroke = Stroke(
        width = 0.085f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    val softStroke = Stroke(
        width = 0.125f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )

    for (stroke in geometry.strokes) {
        if (consumed >= target) {
            break
        }

        val drawLength = min(stroke.length, target - consumed)
        if (drawLength <= 0f) {
            break
        }

        val path = if (drawLength >= stroke.length) {
            stroke.path
        } else {
            Path().also { partial ->
                stroke.measure.getSegment(0f, drawLength, partial, true)
            }
        }

        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.22f),
            style = softStroke,
        )
        drawPath(
            path = path,
            color = color,
            style = mainStroke,
        )

        consumed += stroke.length
    }
}
