package com.example.dirt.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.dirt.theme.Amber
import com.example.dirt.theme.BodyFont
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Muted
import com.example.dirt.theme.Paper
import com.example.dirt.theme.Void
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
 */
@Composable
fun AngleDial(angle: Float, tone: Tone, modifier: Modifier = Modifier, turn: () -> Float = { 0f }, edge: () -> Float = { 0f }) {
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
            Tone.Idle -> Muted
            Tone.Moving -> Amber
            else -> Lime
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
    val label = TextStyle(fontFamily = BodyFont, fontSize = 10.sp, color = Muted)
    Canvas(modifier.graphicsLayer { rotationZ = turn() }) {
        val radius = min(size.width, size.height) / 2f * DialFill
        val strength = energy + flash.value * 0.8f
        val needle = if (tone == Tone.Idle) Muted else Lime
        if (flash.value > 0f || tone == Tone.Locked) {
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Lime.copy(alpha = 0.10f * strength), Color.Transparent),
                center, radius * 1.15f), radius * 1.15f)
        }
        drawTicks(radius, shown, strength)
        for (deg in -90..90 step 30) {
            val t = Math.toRadians(deg.toDouble()).toFloat()
            val layout = measurer.measure(if (deg < 0) "−${-deg}" else "$deg", label)
            val at = center + Offset(cos(t), -sin(t)) * (radius - 24.dp.toPx())
            drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        drawLine(Muted.copy(alpha = 0.5f * (1f - edge())), center - Offset(radius, 0f), center + Offset(radius, 0f), 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())))
        // On its edge the readout sits in the middle, so the needle starts outside it.
        val clear = dialInner(radius, density) * edge()
        drawSector(radius * 0.62f + clear * 0.38f, shown, accent, strength, fill = 1f - edge())
        drawNeedle(radius * 0.94f, clear, shown, needle, strength, hub = 1f - edge())
    }
}

/** Tiny dial for journal rows: horizon, needle and the angle sector. */
@Composable
fun MiniDial(angle: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = min(size.width, size.height) / 2f * 0.8f
        drawCircle(Muted.copy(alpha = 0.25f), radius, style = Stroke(1.dp.toPx()))
        drawLine(Muted.copy(alpha = 0.5f), center - Offset(radius, 0f), center + Offset(radius, 0f), 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())))
        drawSector(radius * 0.7f, angle, Lime, 0.9f)
        val t = Math.toRadians(angle.toDouble()).toFloat()
        val dir = Offset(cos(t), -sin(t)) * radius
        drawLine(Lime, center - dir * 0.5f, center + dir, 1.8.dp.toPx(), StrokeCap.Round)
        drawCircle(Lime, 2.dp.toPx(), center)
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

/** 2° ticks, longer every 10° and 30°; the ones the needle passes light up on both ends. */
private fun DrawScope.drawTicks(radius: Float, angle: Float, strength: Float) {
    for (deg in 0 until 360 step 2) {
        val t = Math.toRadians(deg.toDouble()).toFloat()
        val d = Offset(cos(t), -sin(t))
        val length = when {
            deg % 30 == 0 -> 12.dp.toPx()
            deg % 10 == 0 -> 8.dp.toPx()
            else -> 4.dp.toPx()
        }
        val off = minOf(gap(deg.toFloat(), angle), gap(deg.toFloat(), angle + 180f))
        val near = exp(-(off / 7f) * (off / 7f))
        val color = if (near > 0.3f) Lime.copy(alpha = 0.35f + 0.6f * near * strength) else Muted.copy(alpha = 0.22f + 0.2f * near)
        drawLine(color, center + d * (radius - length), center + d * radius, (if (deg % 10 == 0) 1.4f else 1f) * density)
    }
}

private fun gap(a: Float, b: Float): Float = abs(((a - b) % 360f + 540f) % 360f - 180f)

private fun DrawScope.drawSector(radius: Float, angle: Float, color: Color, strength: Float, fill: Float = 1f) {
    val topLeft = center - Offset(radius, radius)
    val box = Size(radius * 2f, radius * 2f)
    if (fill > 0.01f) drawArc(color.copy(alpha = (0.05f + 0.07f * strength) * fill), 0f, -angle, true, topLeft, box)
    drawArc(color.copy(alpha = 0.9f), 0f, -angle, false, topLeft, box, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
}

/** The measured line through the centre; its front half, towards the phone's top, carries the glow. */
private fun DrawScope.drawNeedle(length: Float, clear: Float, angle: Float, color: Color, strength: Float, hub: Float) {
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
        drawCircle(Void, 6.dp.toPx(), center, alpha = hub)
        drawCircle(Paper, 6.dp.toPx(), center, alpha = hub, style = Stroke(1.5.dp.toPx()))
    }
}
