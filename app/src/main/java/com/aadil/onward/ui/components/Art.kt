package com.aadil.onward.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aadil.onward.ui.theme.Onward
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The app mark: an evergreen tile with a check. */
@Composable
fun BrandMark(size: Dp = 30.dp) {
    val c = Onward.colors
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 32f
        drawRoundRect(c.accent, cornerRadius = CornerRadius(9 * s))
        val tick = Path().apply {
            moveTo(9.5f * s, 16.5f * s); lineTo(14f * s, 21f * s); lineTo(22.5f * s, 11.5f * s)
        }
        drawPath(tick, c.accentInk, style = Stroke(2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Soft coloured glows behind the content, top left and top right. */
@Composable
fun Backdrop(modifier: Modifier = Modifier) {
    val c = Onward.colors
    Canvas(modifier.fillMaxSize()) {
        val d = density
        drawRect(
            Brush.radialGradient(
                listOf(c.glow1, c.glow1.copy(alpha = 0f)),
                center = Offset(100 * d, -0 * d),
                radius = 340 * d,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(c.glow2, c.glow2.copy(alpha = 0f)),
                center = Offset(size.width + 20 * d, 10 * d),
                radius = 300 * d,
            ),
        )
    }
}

/** A sun rising over the horizon, drawn once with a gentle entrance. */
@Composable
fun SunriseArt(modifier: Modifier = Modifier) {
    val c = Onward.colors
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(900, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f))) }
    Canvas(modifier.size(150.dp, 100.dp)) {
        val s = size.width / 160f
        scale(s, pivot = Offset.Zero) {
            fun line(a: Offset, b: Offset, color: Color) =
                drawLine(color, a, b, strokeWidth = 3f, cap = StrokeCap.Round)
            line(Offset(16f, 79f), Offset(144f, 79f), c.lineStrong)
            line(Offset(36f, 92f), Offset(80f, 92f), c.line)
            line(Offset(92f, 92f), Offset(114f, 92f), c.line)
            line(Offset(56f, 103f), Offset(104f, 103f), c.line)
            clipRect(0f, 0f, 160f, 77.5f) {
                translate(top = (1 - t.value) * 34f) {
                    val disc = Rect(Offset(80f, 78f), 36f)
                    drawArc(c.accentSoft, 180f, 180f, true, disc.topLeft, disc.size)
                    drawArc(c.accent, 180f, 180f, false, disc.topLeft, disc.size,
                        style = Stroke(3f, cap = StrokeCap.Round))
                }
            }
            val rays = c.accent.copy(alpha = 0.55f * t.value * t.value)
            listOf(
                Offset(80f, 26f) to Offset(80f, 14f),
                Offset(52f, 36f) to Offset(44f, 28f),
                Offset(108f, 36f) to Offset(116f, 28f),
                Offset(36f, 60f) to Offset(25f, 56f),
                Offset(124f, 60f) to Offset(135f, 56f),
            ).forEach { (a, b) -> line(a, b, rays) }
        }
    }
}

/** A ring that fills as things get done, with whatever sits in its middle. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    stroke: Dp = 7.dp,
    color: Color = Onward.colors.accent,
    track: Color = Onward.colors.lineStrong,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            if (p > 0f) {
                drawArc(color, -90f, 360f * p, false, Offset(inset, inset), arcSize,
                    style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        center()
    }
}

/**
 * The round checkbox. Its rim takes the priority colour; checking fills it
 * with [fill] and draws the tick in.
 */
@Composable
fun CheckCircle(
    checked: Boolean,
    rim: Color,
    modifier: Modifier = Modifier,
    fill: Color = Onward.colors.accent,
    tickColor: Color = Onward.colors.accentInk,
    size: Dp = 24.dp,
) {
    val t by animateFloatAsState(if (checked) 1f else 0f, tween(320, easing = FastOutSlowInEasing), label = "check")
    val pop by animateFloatAsState(
        if (checked) 1f else 0.0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pop",
    )
    Canvas(modifier.size(size).scale(1f + 0.12f * (pop - t).coerceAtLeast(0f) + 0.0f)) {
        val r = this.size.minDimension / 2
        val w = 2.dp.toPx()
        drawCircle(rim.copy(alpha = rim.alpha * (1 - t)), radius = r - w / 2, style = Stroke(w))
        drawCircle(fill, radius = r * t)
        if (t > 0f) {
            val s = this.size.width / 24f
            val tick = Path().apply {
                moveTo(7f * s, 12.4f * s); lineTo(10.4f * s, 15.8f * s); lineTo(17f * s, 8.8f * s)
            }
            val measure = PathMeasure().apply { setPath(tick, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * ((t - 0.3f) / 0.7f).coerceIn(0f, 1f), partial, true)
            drawPath(partial, tickColor, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private class Piece(
    val angle: Float, val speed: Float, val spin: Float, val color: Color,
    val w: Float, val h: Float, val drift: Float,
)

/**
 * A burst of confetti from [origin] each time [trigger] goes up. Draws
 * nothing in between, so it can sit over the whole screen.
 */
@Composable
fun Confetti(trigger: Int, origin: Offset, colors: List<Color>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    var pieces by remember { mutableStateOf(emptyList<Piece>()) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val rnd = Random(trigger * 7919)
        pieces = List(80) {
            Piece(
                angle = (-90f + rnd.nextFloat() * 150f - 75f) * (Math.PI / 180f).toFloat(),
                speed = 380f + rnd.nextFloat() * 620f,
                spin = rnd.nextFloat() * 900f - 450f,
                color = colors[rnd.nextInt(colors.size)],
                w = 5f + rnd.nextFloat() * 4f,
                h = 8f + rnd.nextFloat() * 6f,
                drift = rnd.nextFloat() * 60f - 30f,
            )
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1900, easing = LinearEasing))
    }
    if (progress.value >= 1f || pieces.isEmpty()) return
    Canvas(modifier.fillMaxSize()) {
        val secs = progress.value * 1.9f
        val d = density
        val g = 900f
        val fade = (1f - ((progress.value - 0.7f) / 0.3f).coerceIn(0f, 1f))
        for (p in pieces) {
            val x = origin.x + (cos(p.angle) * p.speed * secs + p.drift * secs) * d * 0.6f
            val y = origin.y + (sin(p.angle) * p.speed * secs + 0.5f * g * secs * secs) * d * 0.6f
            rotate(p.spin * secs, pivot = Offset(x, y)) {
                drawRect(
                    p.color.copy(alpha = fade),
                    topLeft = Offset(x - p.w * d / 2, y - p.h * d / 2),
                    size = Size(p.w * d, p.h * d * (0.4f + 0.6f * kotlin.math.abs(cos(secs * 6 + p.spin)))),
                )
            }
        }
    }
}
