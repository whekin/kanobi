package com.kanobi.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanobi.app.theme.BodyFont
import com.kanobi.app.theme.KanobiTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class Tone { Idle, Moving, Stable, Locked }

/** Share of the stage's shorter side the dial ring takes; the readout uses it to fit inside. */
const val DialFill = 0.86f

/** Ring radius left inside the ticks and labels, for a dial of [radius]. */
fun dialInner(radius: Float, density: Float) = radius - 30f * density

/**
 * Abstract inclinometer: a tick ring, the horizon and a needle along the measured line. Rotated by
 * [turn] it stays level with the real horizon, so on its edge the needle runs along the phone.
 * The ring and labels are cached; each frame only redraws the ticks near the needle and the needle.
 */
@Composable
fun AngleDial(angle: Float, tone: Tone, modifier: Modifier = Modifier, turn: () -> Float = { 0f }, edge: () -> Float = { 0f }) {
    val colors = KanobiTheme.colors
    val shown by rememberEased(angle, 0.12f)
    val energy by animateFloatAsState(
        when (tone) {
            Tone.Idle -> 0.2f
            Tone.Moving -> 0.55f
            Tone.Stable -> 0.8f
            Tone.Locked -> 1f
        }, tween(400), label = "energy",
    )
    val accent by animateColorAsState(
        when (tone) {
            Tone.Idle -> colors.muted
            Tone.Moving -> colors.warn
            else -> colors.accent
        }, tween(300), label = "accent",
    )
    val flash = remember { Animatable(0f) }
    LaunchedEffect(tone) {
        if (tone == Tone.Locked) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(1100, easing = LinearOutSlowInEasing))
        }
    }
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontFamily = BodyFont, fontSize = 10.sp, color = colors.muted)
    Spacer(modifier.graphicsLayer { rotationZ = turn() }.drawWithCache {
        val radius = min(size.width, size.height) / 2f * DialFill
        val middle = Offset(size.width / 2f, size.height / 2f)
        val ring = ticks(middle, radius, every = 2, skip = 10, length = 4.dp.toPx())
        val tens = ticks(middle, radius, every = 10, skip = 30, length = 8.dp.toPx())
        val thirties = ticks(middle, radius, every = 30, skip = 0, length = 12.dp.toPx())
        val labels = (-90..90 step 30).map { deg ->
            val layout = measurer.measure(if (deg < 0) "−${-deg}" else "$deg", label)
            val t = Math.toRadians(deg.toDouble()).toFloat()
            val at = middle + Offset(cos(t), -sin(t)) * (radius - 24.dp.toPx())
            layout to at - Offset(layout.size.width / 2f, layout.size.height / 2f)
        }
        val dashes = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
        onDrawBehind {
            val a = shown
            val strength = energy + flash.value * 0.8f
            val side = edge()
            if (flash.value > 0f || tone == Tone.Locked) {
                drawCircle(Brush.radialGradient(listOf(Color.Transparent, colors.accent.copy(alpha = 0.10f * strength), Color.Transparent),
                    center, radius * 1.15f), radius * 1.15f)
            }
            drawPath(ring, colors.muted.copy(alpha = 0.22f), style = Stroke(1f * density))
            drawPath(tens, colors.muted.copy(alpha = 0.32f), style = Stroke(1.4f * density))
            drawPath(thirties, colors.muted.copy(alpha = 0.32f), style = Stroke(1.4f * density))
            drawNeedleTicks(radius, a, strength, colors.accent)
            for ((layout, at) in labels) drawText(layout, topLeft = at)
            drawLine(colors.muted.copy(alpha = 0.5f * (1f - side)), center - Offset(radius, 0f), center + Offset(radius, 0f),
                1.dp.toPx(), pathEffect = dashes)
            // On its edge the readout sits in the middle, so the needle starts outside it.
            val clear = dialInner(radius, density) * side
            drawSector(radius * 0.62f + clear * 0.38f, a, accent, strength, fill = 1f - side)
            drawNeedle(radius * 0.94f, clear, a, if (tone == Tone.Idle) colors.muted else colors.accent, strength,
                hub = 1f - side, hubFill = colors.background, hubLine = colors.text)
        }
    })
}

/** Tiny dial for journal rows: horizon, needle and the angle sector. */
@Composable
fun MiniDial(angle: Float, modifier: Modifier = Modifier) {
    val colors = KanobiTheme.colors
    Canvas(modifier) {
        val radius = min(size.width, size.height) / 2f * 0.8f
        drawCircle(colors.muted.copy(alpha = 0.25f), radius, style = Stroke(1.dp.toPx()))
        drawLine(colors.muted.copy(alpha = 0.5f), center - Offset(radius, 0f), center + Offset(radius, 0f), 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())))
        drawSector(radius * 0.7f, angle, colors.accent, 0.9f)
        val t = Math.toRadians(angle.toDouble()).toFloat()
        val dir = Offset(cos(t), -sin(t)) * radius
        drawLine(colors.accent, center - dir * 0.5f, center + dir, 1.8.dp.toPx(), StrokeCap.Round)
        drawCircle(colors.accent, 2.dp.toPx(), center)
    }
}

/** Follows a value that changes at sensor rate, easing every frame instead of restarting an animation. */
@Composable
fun rememberEased(target: Float, timeConstant: Float): State<Float> {
    val value = remember { mutableFloatStateOf(target) }
    val latest by rememberUpdatedState(target)
    LaunchedEffect(timeConstant) {
        var previous = 0L
        while (true) withFrameNanos { now ->
            val dt = if (previous == 0L) 0f else ((now - previous) / 1e9f).coerceAtMost(0.05f)
            previous = now
            val delta = latest - value.floatValue
            if (abs(delta) > 0.01f) value.floatValue += delta * (1f - exp(-dt / timeConstant))
        }
    }
    return value
}

/** Ticks every [every]° round the ring, leaving out multiples of [skip], [length] long towards the centre. */
private fun ticks(center: Offset, radius: Float, every: Int, skip: Int, length: Float) = Path().apply {
    for (deg in 0 until 360 step every) {
        if (skip > 0 && deg % skip == 0) continue
        val t = Math.toRadians(deg.toDouble()).toFloat()
        val d = Offset(cos(t), -sin(t))
        val from = center + d * (radius - length)
        val to = center + d * radius
        moveTo(from.x, from.y)
        lineTo(to.x, to.y)
    }
}

/** The ticks the needle passes light up, on both of its ends. */
private fun DrawScope.drawNeedleTicks(radius: Float, angle: Float, strength: Float, color: Color) {
    for (end in listOf(angle, angle + 180f)) {
        val first = (Math.floorDiv(end.toInt() - 20, 2)) * 2
        for (deg in first..first + 42 step 2) {
            val off = abs(deg - end)
            val near = exp(-(off / 7f) * (off / 7f))
            if (near < 0.3f) continue
            val norm = ((deg % 360) + 360) % 360
            val length = when {
                norm % 30 == 0 -> 12.dp.toPx()
                norm % 10 == 0 -> 8.dp.toPx()
                else -> 4.dp.toPx()
            }
            val t = Math.toRadians(deg.toDouble()).toFloat()
            val d = Offset(cos(t), -sin(t))
            drawLine(color.copy(alpha = (0.35f + 0.6f * near * strength).coerceAtMost(1f)), center + d * (radius - length), center + d * radius,
                (if (norm % 10 == 0) 1.4f else 1f) * density)
        }
    }
}

private fun DrawScope.drawSector(radius: Float, angle: Float, color: Color, strength: Float, fill: Float = 1f) {
    val topLeft = center - Offset(radius, radius)
    val box = Size(radius * 2f, radius * 2f)
    if (fill > 0.01f) drawArc(color.copy(alpha = (0.05f + 0.07f * strength) * fill), 0f, -angle, true, topLeft, box)
    drawArc(color.copy(alpha = 0.9f), 0f, -angle, false, topLeft, box, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
}

/** The measured line through the centre; its front half, towards the phone's top, carries the glow. */
private fun DrawScope.drawNeedle(length: Float, clear: Float, angle: Float, color: Color, strength: Float, hub: Float, hubFill: Color, hubLine: Color) {
    val t = Math.toRadians(angle.toDouble()).toFloat()
    val dir = Offset(cos(t), -sin(t))
    val tip = center + dir * length
    val root = center + dir * clear
    drawLine(color.copy(alpha = 0.45f), center - dir * length, center - dir * clear, 1.5.dp.toPx(), StrokeCap.Round)
    for ((width, alpha) in listOf(18.dp to 0.05f, 10.dp to 0.09f, 5.dp to 0.18f)) {
        drawLine(color.copy(alpha = (alpha * strength).coerceIn(0f, 1f)), root, tip, width.toPx(), StrokeCap.Round)
    }
    drawLine(color, root, tip, 2.5.dp.toPx(), StrokeCap.Round)
    drawCircle(color, 4.dp.toPx(), tip)
    if (hub > 0.01f) {
        drawCircle(hubFill, 6.dp.toPx(), center, alpha = hub)
        drawCircle(hubLine, 6.dp.toPx(), center, alpha = hub, style = Stroke(1.5.dp.toPx()))
    }
}
