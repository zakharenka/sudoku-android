package me.zakharenka.sudoku.ui.sketch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.zakharenka.sudoku.ui.theme.SudokuTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun DrawScope.sketchLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    seed: Int,
    wobble: Float = 1.2f,
    passes: Int = 2,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    val normalX = if (length == 0f) 0f else -dy / length
    val normalY = if (length == 0f) 0f else dx / length
    val segments = max(4, (length / 28f).toInt())

    repeat(passes) { pass ->
        val random = Random(seed + pass * 1_000)
        val baseOffset = if (pass == 0) 0f else (random.nextFloat() * 2f - 1f) * 0.9f
        val path = Path()

        for (step in 0..segments) {
            val t = step.toFloat() / segments
            val edge = step == 0 || step == segments
            val jitter = if (edge) 0f else (random.nextFloat() * 2f - 1f) * wobble
            val x = start.x + dx * t + normalX * (baseOffset + jitter)
            val y = start.y + dy * t + normalY * (baseOffset + jitter)

            if (step == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * if (pass == 0) 1f else 0.55f),
            style = Stroke(
                width = strokeWidth * if (pass == 0) 1f else 0.75f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

fun DrawScope.sketchRect(
    topLeft: Offset,
    size: Size,
    color: Color,
    strokeWidth: Float,
    seed: Int,
    wobble: Float = 1.2f,
    passes: Int = 2,
) {
    val topRight = Offset(topLeft.x + size.width, topLeft.y)
    val bottomRight = Offset(topLeft.x + size.width, topLeft.y + size.height)
    val bottomLeft = Offset(topLeft.x, topLeft.y + size.height)

    sketchLine(topLeft, topRight, color, strokeWidth, seed, wobble, passes)
    sketchLine(topRight, bottomRight, color, strokeWidth, seed + 1, wobble, passes)
    sketchLine(bottomRight, bottomLeft, color, strokeWidth, seed + 2, wobble, passes)
    sketchLine(bottomLeft, topLeft, color, strokeWidth, seed + 3, wobble, passes)
}

fun DrawScope.sketchFill(
    topLeft: Offset,
    size: Size,
    color: Color,
    seed: Int,
    wobble: Float = 1.4f,
) {
    if (color.alpha == 0f) {
        return
    }

    val random = Random(seed)
    val path = Path()
    val points = listOf(
        Offset(topLeft.x + randomJitter(random, wobble), topLeft.y + randomJitter(random, wobble)),
        Offset(
            topLeft.x + size.width + randomJitter(random, wobble),
            topLeft.y + randomJitter(random, wobble),
        ),
        Offset(
            topLeft.x + size.width + randomJitter(random, wobble),
            topLeft.y + size.height + randomJitter(random, wobble),
        ),
        Offset(
            topLeft.x + randomJitter(random, wobble),
            topLeft.y + size.height + randomJitter(random, wobble),
        ),
    )

    path.moveTo(points[0].x, points[0].y)
    for (index in 1 until points.size) {
        path.lineTo(points[index].x, points[index].y)
    }
    path.close()

    drawPath(path = path, color = color)
}

fun DrawScope.sketchOval(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    color: Color,
    strokeWidth: Float,
    seed: Int,
    wobble: Float = 0.7f,
) {
    val random = Random(seed)
    val path = Path()
    val steps = 28

    for (step in 0..steps) {
        val angle = 2.0 * PI * step / steps
        val edge = step == 0 || step == steps
        val jitter = if (edge) 0f else (random.nextFloat() * 2f - 1f) * wobble
        val x = center.x + cos(angle).toFloat() * (radiusX + jitter)
        val y = center.y + sin(angle).toFloat() * (radiusY + jitter)

        if (step == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

fun DrawScope.paperTexture(
    base: Color,
    speckle: Color,
    seed: Int,
) {
    drawRect(base)

    val random = Random(seed)
    repeat(900) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val radius = 0.4f + random.nextFloat() * 1.4f
        drawCircle(
            color = speckle.copy(alpha = speckle.alpha * (0.25f + random.nextFloat() * 0.75f)),
            radius = radius,
            center = Offset(x, y),
        )
    }

    repeat(18) {
        val y = random.nextFloat() * size.height
        drawLine(
            color = speckle.copy(alpha = speckle.alpha * 0.35f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.6f,
        )
    }
}

private fun randomJitter(random: Random, amount: Float): Float =
    (random.nextFloat() * 2f - 1f) * amount

@Composable
fun SketchSurface(
    modifier: Modifier = Modifier,
    fill: Color,
    border: Color?,
    strokeWidth: Dp = 1.4.dp,
    seed: Int = 0,
    shadow: Color? = null,
    straight: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val stroke = strokeWidth.toPx()
                val inset = if (border == null) 0f else stroke / 2f
                val innerSize = Size(size.width - inset * 2f, size.height - inset * 2f)

                if (straight) {
                    if (shadow != null) {
                        drawRect(
                            color = shadow,
                            topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                        )
                    }
                    drawRect(
                        color = fill,
                        topLeft = Offset(inset, inset),
                        size = innerSize,
                    )
                    if (border != null) {
                        drawRect(
                            color = border,
                            topLeft = Offset(inset, inset),
                            size = innerSize,
                            style = Stroke(width = stroke),
                        )
                    }
                } else {
                    if (shadow != null) {
                        sketchFill(
                            topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                            size = Size(
                                width = size.width - 2.dp.toPx(),
                                height = size.height - 2.dp.toPx(),
                            ),
                            color = shadow,
                            seed = seed + 100,
                            wobble = 1.8.dp.toPx(),
                        )
                    }

                    sketchFill(
                        topLeft = Offset(inset, inset),
                        size = innerSize,
                        color = fill,
                        seed = seed + 200,
                        wobble = 1.1.dp.toPx(),
                    )

                    if (border != null) {
                        sketchRect(
                            topLeft = Offset(inset, inset),
                            size = innerSize,
                            color = border,
                            strokeWidth = stroke,
                            seed = seed,
                            wobble = 1.1.dp.toPx(),
                        )
                    }
                }
            }
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun SketchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Color,
    border: Color,
    active: Boolean = false,
    seed: Int = 0,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = SudokuTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    SketchSurface(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        fill = if (active) colors.accentWash else fill,
        border = if (active) colors.accent else border,
        strokeWidth = if (active) 2.dp else 1.4.dp,
        seed = seed,
        shadow = if (pressed) null else colors.shadow,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun SketchSpinner(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sketch-spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
        ),
        label = "sketch-spinner-angle",
    )

    Canvas(modifier) {
        rotate(angle) {
            val stroke = 2.dp.toPx()
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
