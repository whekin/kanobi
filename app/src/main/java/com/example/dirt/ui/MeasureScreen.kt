package com.example.dirt.ui

import android.view.Surface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.dirt.fx.Agsl
import com.example.dirt.fx.chromaticAberration
import com.example.dirt.fx.rememberShader
import com.example.dirt.measurement.Reading
import com.example.dirt.theme.Amber
import com.example.dirt.theme.DisplayFont
import com.example.dirt.theme.Ink
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Line
import com.example.dirt.theme.Muted
import com.example.dirt.theme.Panel
import com.example.dirt.theme.Paper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MeasureScreen(
    reading: () -> Reading,
    available: Boolean,
    held: Float?,
    focus: FocusPoint,
    onLock: (Float) -> Unit,
    onRelease: () -> Unit,
    onSave: () -> Unit,
) {
    val current = reading()
    val locked = held != null
    val valid = locked || current.ready
    val angle = held ?: current.angle.coerceIn(-90f, 90f)
    val stable = !locked && current.ready && current.stable
    val tone = when {
        locked -> Tone.Locked
        !valid -> Tone.Idle
        stable -> Tone.Stable
        else -> Tone.Moving
    }
    val level = rememberLevel(reading, screenTurn())
    var stage by remember { mutableStateOf(IntSize.Zero) }
    var group by remember { mutableStateOf(IntSize.Zero) }
    val inner = dialInner(min(stage.width, stage.height) / 2f * DialFill, LocalDensity.current.density)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 600.dp
        val width = constraints.maxWidth.toFloat()
        Column(
            Modifier.fillMaxSize()
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Readout and slope stay level with the horizon. Standing on its edge, the phone becomes
            // a spirit level: the readout moves into the middle of the dial, which turns with it.
            Column(
                Modifier
                    .onSizeChanged { group = it }
                    .onGloballyPositioned {
                        focus.center = it.boundsInRoot().center
                        focus.lift = (group.height + stage.height) / 2f
                        focus.clear = inner
                    }
                    .graphicsLayer {
                        val rad = Math.toRadians(level.turn.toDouble())
                        val c = abs(cos(rad)).toFloat()
                        val s = abs(sin(rad)).toFloat()
                        val w = group.width.toFloat()
                        val h = group.height.toFloat()
                        val free = min(1f, min(width / max(w * c + h * s, 1f), (h + stage.height) / max(w * s + h * c, 1f)))
                        val inside = min(1f, 1.9f * inner / max(hypot(w, h), 1f))
                        val fit = free + (inside - free) * level.edge
                        rotationZ = level.turn
                        scaleX = fit
                        scaleY = fit
                        translationY = level.edge * (h + stage.height) / 2f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Readout(angle.takeIf { valid }, tone, if (tone == Tone.Moving) current.motion else 0f, Modifier.padding(top = 4.dp))
                Detail(
                    when {
                        !available -> "нет датчика наклона"
                        valid -> "уклон ${slope(angle)}"
                        else -> ""
                    },
                )
            }
            AngleDial(
                if (valid) angle else 0f, tone,
                Modifier.fillMaxWidth().then(if (compact) Modifier.height(280.dp) else Modifier.weight(1f))
                    .onSizeChanged { stage = it },
                turn = { level.turn }, edge = { level.edge },
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                // Both side slots keep their width so the shutter stays centred.
                Box(Modifier.width(SideWidth)) {
                    androidx.compose.animation.AnimatedVisibility(locked, enter = fadeIn(), exit = fadeOut()) {
                        SideAction(Glyph.Play, "Продолжить", onRelease)
                    }
                }
                Shutter(
                    when {
                        locked -> ShutterMode.Held
                        stable -> ShutterMode.Ready
                        else -> ShutterMode.Waiting
                    },
                    if (current.ready) current.progress else 0f,
                    onClick = { if (locked) onSave() else if (stable) onLock(angle) },
                )
                Spacer(Modifier.width(SideWidth))
            }
        }
    }
}

private val SideWidth = 96.dp

/** Clockwise rotation the system already applies to the UI; zero on portrait-locked phones. */
@Composable
fun screenTurn(): Float = (LocalView.current.display?.rotation ?: Surface.ROTATION_0) * 90f

/** Share of the roll the readout follows: none lying flat, where the direction is noise. */
private fun levelWeight(reading: Reading) = if (reading.ready) smoothstep(0.25f, 0.5f, reading.inPlane) else 0f

/** 0 lying flat or upright, 1 standing on the long edge. */
fun edgeWeight(reading: Reading) = if (reading.ready) smoothstep(0.5f, 0.75f, abs(reading.upX)) else 0f

/** Readout rotation and edge blend, eased once per frame so 50 Hz sensor updates never restart it. */
@Stable
private class Level {
    var turn by mutableFloatStateOf(0f)
    var edge by mutableFloatStateOf(0f)
}

@Composable
private fun rememberLevel(reading: () -> Reading, screenTurn: Float): Level {
    val level = remember { Level() }
    val latest by rememberUpdatedState(reading)
    LaunchedEffect(screenTurn) {
        var previous = 0L
        while (true) withFrameNanos { now ->
            val dt = if (previous == 0L) 0f else ((now - previous) / 1e9f).coerceAtMost(0.05f)
            previous = now
            val current = latest()
            val goal = wrap(current.roll - screenTurn) * levelWeight(current)
            val turn = wrap(goal - level.turn)
            if (abs(turn) > 0.02f) level.turn += turn * (1f - exp(-dt / 0.12f))
            val edge = edgeWeight(current) - level.edge
            if (abs(edge) > 0.001f) level.edge += edge * (1f - exp(-dt / 0.2f))
        }
    }
    return level
}

private fun wrap(degrees: Float) = ((degrees % 360f) + 540f) % 360f - 180f

private fun smoothstep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private val ReadoutStyle = TextStyle(
    fontFamily = DisplayFont,
    fontFeatureSettings = "tnum",
    lineHeight = 0.92.em,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/** The big number. Motion splits it into RGB fringes; holding still brings it into focus. */
@Composable
private fun Readout(angle: Float?, tone: Tone, motion: Float, modifier: Modifier = Modifier) {
    val chroma = rememberShader(Agsl.CHROMA)
    val locked = tone == Tone.Locked
    val color by animateColorAsState(if (locked) Lime else Paper, tween(260), label = "readout")
    val split = (motion / 30f).coerceIn(0f, 1f)
    val pop = remember { Animatable(1f) }
    LaunchedEffect(locked) {
        if (locked) {
            pop.snapTo(1.08f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
        }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val size = with(LocalDensity.current) { min(maxWidth.value / 2.3f, 156f).dp.toSp() }
        val em = min(maxWidth.value / 2.3f, 156f)
        val signWidth = (em * 0.24f).dp
        val capHeight = (em * 0.7f).dp
        Row(
            Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .chromaticAberration(chroma) { split }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sign = when {
                angle == null || abs(angle) < 0.05f -> ""
                angle > 0f -> "+"
                else -> "−"
            }
            Text(sign, Modifier.width(signWidth), color = Muted, fontSize = size * 0.42f, textAlign = TextAlign.Center, style = ReadoutStyle)
            Text(angle?.let { degrees(abs(it)) } ?: "—", Modifier.alignByBaseline(), color = color, fontSize = size,
                style = ReadoutStyle.copy(shadow = if (locked) Shadow(Lime.copy(alpha = 0.55f), blurRadius = 56f) else null))
            // Half-size degree sign, raised from the shared baseline to the top of the digits. A layer
            // offset, unlike Modifier.offset, leaves the baseline the row aligns on untouched.
            Text("°", Modifier.alignByBaseline().graphicsLayer { translationY = -(capHeight * 0.5f).toPx() },
                color = Lime, fontSize = size * 0.5f, style = ReadoutStyle)
        }
    }
}

/** One quiet line under the number; state itself is shown by colour, the ring and the RGB split. */
@Composable
private fun Detail(text: String) {
    Text(text, color = Muted, fontSize = 13.sp, modifier = Modifier.height(20.dp))
}

private enum class ShutterMode { Waiting, Ready, Held }

/** Camera-style shutter: the tick ring fills while the reading holds still. */
@Composable
private fun Shutter(mode: ShutterMode, progress: Float, onClick: () -> Unit) {
    val fill by animateFloatAsState(if (mode == ShutterMode.Waiting) 0f else 1f, tween(240), label = "fill")
    val ring = if (mode == ShutterMode.Held) 1f else progress
    val breath = if (mode == ShutterMode.Ready) rememberInfiniteTransition(label = "breath")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    else null
    val label = if (mode == ShutterMode.Held) "Сохранить" else "Зафиксировать"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f
                val glow = (if (mode == ShutterMode.Held) 0.8f else 0.45f * fill) + 0.35f * (breath?.value ?: 0f)
                if (glow > 0.01f) drawCircle(Brush.radialGradient(listOf(Lime.copy(alpha = 0.3f * glow), Color.Transparent), center, r * 1.7f), r * 1.7f)
                val ticks = 60
                val lit = (ring * ticks).roundToInt()
                for (i in 0 until ticks) {
                    val a = i * 2f * PI.toFloat() / ticks - PI.toFloat() / 2f
                    val d = Offset(cos(a), sin(a))
                    val color = when {
                        i >= lit -> Line
                        ring >= 1f -> Lime
                        else -> Amber
                    }
                    drawLine(color, center + d * (r - 7.dp.toPx()), center + d * (r - 1.dp.toPx()),
                        (if (i % 5 == 0) 2f else 1.4f) * density, StrokeCap.Round)
                }
                val disc = r - 12.dp.toPx()
                drawCircle(lerp(Panel, Lime, fill), disc)
                if (fill < 0.99f) drawCircle(Line, disc, style = Stroke(1.dp.toPx()))
                val glyph = 30.dp.toPx()
                inset((size.width - glyph) / 2f, (size.height - glyph) / 2f) {
                    drawGlyph(if (mode == ShutterMode.Held) Glyph.Save else Glyph.Lock, lerp(Muted, Ink, fill))
                }
            }
            Box(
                Modifier.matchParentSize().clip(CircleShape)
                    .clickable(enabled = mode != ShutterMode.Waiting, role = Role.Button, onClick = onClick)
                    .semantics { contentDescription = label },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = if (mode == ShutterMode.Waiting) Muted else Paper, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SideAction(glyph: Glyph, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(SideWidth).padding(top = 16.dp).clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).background(Panel.copy(alpha = 0.85f), CircleShape).border(1.dp, Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, Paper, Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
