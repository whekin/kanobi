package com.kanobi.app.ui

import android.view.Surface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kanobi.app.R
import com.kanobi.app.measurement.Reading
import com.kanobi.app.measurement.Calibration
import com.kanobi.app.theme.DisplayFont
import com.kanobi.app.theme.KanobiTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun MeasureScreen(
    reading: () -> Reading,
    available: Boolean,
    calibration: Calibration,
    held: Float?,
    live: Boolean,
    focus: FocusPoint,
    onLock: (Float) -> Unit,
    onRelease: () -> Unit,
    onSave: () -> Unit,
) {
    val current = reading()
    val locked = held != null
    val valid = locked || current.ready
    val angle = held ?: calibration.correct(current)
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
    val window = LocalWindowInfo.current
    val density = LocalDensity.current.density
    val inner = dialInner(min(stage.width, stage.height) / 2f * DialFill, density)
    // Phones under ~700 dp tall scroll instead of squeezing the dial.
    val compact = window.containerDpSize.height < 700.dp
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
                    val free = min(1f, min(window.containerSize.width / max(w * c + h * s, 1f), (h + stage.height) / max(w * s + h * c, 1f)))
                    val inside = min(1f, 1.9f * inner / max(hypot(w, h), 1f))
                    val fit = free + (inside - free) * level.edge
                    rotationZ = level.turn
                    scaleX = fit
                    scaleY = fit
                    translationY = level.edge * (h + stage.height) / 2f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Readout(angle.takeIf { valid }, tone, if (tone == Tone.Moving && live) current.motion else 0f, Modifier.padding(top = 4.dp))
            Text(
                when {
                    !available -> stringResource(R.string.no_sensor)
                    valid -> stringResource(R.string.slope_value, slopeText(angle))
                    else -> ""
                },
                color = KanobiTheme.colors.muted, fontSize = 13.sp, modifier = Modifier.height(20.dp),
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
                    SideAction(Glyph.Play, stringResource(R.string.resume), onRelease)
                }
            }
            Shutter(
                when {
                    locked -> ShutterMode.Held
                    stable -> ShutterMode.Ready
                    else -> ShutterMode.Waiting
                },
                progress = { reading().let { if (it.ready) it.progress else 0f } },
                onClick = { if (locked) onSave() else if (stable) onLock(angle) },
            )
            Spacer(Modifier.width(SideWidth))
        }
    }
}

private val SideWidth = 96.dp

/** Clockwise rotation the system already applies to the UI; zero on portrait-locked phones. */
@Composable
fun screenTurn(): Float = (LocalView.current.display?.rotation ?: Surface.ROTATION_0) * 90f

/** Share of the roll the readout follows: none lying flat, where the direction is noise. */
private fun levelWeight(reading: Reading) = if (reading.ready) smoothstep(0.25f, 0.5f, reading.inPlane) else 0f

/**
 * 0 lying flat or upright, 1 standing on the long edge: the screen within ~35° of vertical and
 * the long side turned at least 15° off it, i.e. readings of 75° and below on the edge.
 */
fun edgeWeight(reading: Reading): Float {
    if (!reading.ready) return 0f
    val sideways = Math.toDegrees(atan2(abs(reading.upX), abs(reading.upY)).toDouble()).toFloat()
    return smoothstep(0.7f, 0.85f, reading.inPlane) * smoothstep(8f, 15f, sideways)
}

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
            if (abs(turn) > 0.02f) level.turn = InstrumentResponse.follow(level.turn, level.turn + turn, dt, InstrumentResponse.TurnTimeConstant)
            val edge = edgeWeight(current) - level.edge
            if (abs(edge) > 0.001f) level.edge = InstrumentResponse.follow(level.edge, level.edge + edge, dt, InstrumentResponse.EdgeTimeConstant)
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

/** Split glyph colours directly in the canvas, without an offscreen text RuntimeShader. */
@Composable
private fun Readout(angle: Float?, tone: Tone, motion: Float, modifier: Modifier = Modifier) {
    val colors = KanobiTheme.colors
    val locked = tone == Tone.Locked
    val shift = (motion / 30f).coerceIn(0f, 1f) * 5.dp.value * LocalDensity.current.density
    val color by animateColorAsState(if (locked) colors.accent else colors.text, tween(260), label = "readout")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(locked) {
        if (locked) {
            pop.snapTo(1.08f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
        }
    }
    val em = min(LocalWindowInfo.current.containerDpSize.width.value / 2.3f, 156f)
    val size = with(LocalDensity.current) { em.dp.toSp() }
    Row(
        modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val sign = when {
            angle == null || abs(angle) < 0.05f -> ""
            angle > 0f -> "+"
            else -> "−"
        }
        ChromaticText(sign, Modifier.width((em * 0.24f).dp), colors.muted, ReadoutStyle.copy(fontSize = size * 0.42f, textAlign = TextAlign.Center), shift)
        ChromaticText(angle?.let { degrees(abs(it)) } ?: "—", Modifier.alignByBaseline(), color,
            ReadoutStyle.copy(fontSize = size, shadow = if (locked) Shadow(colors.accent.copy(alpha = 0.55f), blurRadius = 56f) else null), shift)
        // Half-size degree sign, raised from the shared baseline to the top of the digits. A layer
        // offset, unlike Modifier.offset, leaves the baseline the row aligns on untouched.
        ChromaticText("°", Modifier.alignByBaseline().graphicsLayer { translationY = -(em * 0.35f).dp.toPx() },
            colors.accent, ReadoutStyle.copy(fontSize = size * 0.5f), shift)
    }
}

/** Three atlas-backed glyph draws preserve the RGB fringes without RenderEffect or saveLayer.
 * The green pass covers the background; additive red and blue give the original colour where
 * the glyphs overlap. Layout stays the same as an ordinary accessible Text. */
@Composable
private fun ChromaticText(text: String, modifier: Modifier, color: Color, style: TextStyle, shift: Float) {
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(text, modifier.drawWithContent {
        val result = layout.value
        if (shift < .5f || result == null) {
            drawContent()
        } else {
            drawText(result, color = Color(0f, color.green, 0f, color.alpha))
            drawText(result, color = Color(color.red, 0f, 0f, color.alpha), topLeft = Offset(-shift, 0f), blendMode = BlendMode.Plus)
            drawText(result, color = Color(0f, 0f, color.blue, color.alpha), topLeft = Offset(shift, 0f), blendMode = BlendMode.Plus)
        }
    }, color = color, style = style, onTextLayout = { layout.value = it })
}

private enum class ShutterMode { Waiting, Ready, Held }

/**
 * Camera-style shutter. While the reading holds still a comet of ticks sweeps round the ring,
 * dim at its tail and bright at its head, shifting from the warning colour to the accent as it
 * closes; any movement lets it fade back instead of snapping off.
 */
@Composable
private fun Shutter(mode: ShutterMode, progress: () -> Float, onClick: () -> Unit) {
    val colors = KanobiTheme.colors
    val fill by animateFloatAsState(if (mode == ShutterMode.Waiting) 0f else 1f, tween(100, easing = FastOutSlowInEasing), label = "fill")
    val ring = rememberSweep { if (mode == ShutterMode.Held) 1f else progress() }
    val breath = if (mode == ShutterMode.Ready) rememberInfiniteTransition(label = "breath")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    else null
    val label = stringResource(if (mode == ShutterMode.Held) R.string.save else R.string.lock)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f
                val swept = ring.value
                val done = ((swept - 0.85f) / 0.15f).coerceIn(0f, 1f)
                val glow = (if (mode == ShutterMode.Held) 0.8f else 0.45f * fill) + 0.3f * (breath?.value ?: 0f)
                if (glow > 0.01f) {
                    drawCircle(Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.3f * glow), Color.Transparent), center, r * 1.7f), r * 1.7f)
                }
                val ticks = 60
                val head = swept * ticks
                val tint = lerp(colors.warn, colors.accent, done)
                for (i in 0 until ticks) {
                    val a = i * 2f * PI.toFloat() / ticks - PI.toFloat() / 2f
                    val d = Offset(cos(a), sin(a))
                    val lit = (head - i).coerceIn(0f, 1f)
                    // Tail fades, head is brightest; once closed the whole ring evens out.
                    val comet = if (head > 0f) 0.25f + 0.75f * ((i + 1f) / head).coerceAtMost(1f).pow(1.6f) else 0f
                    val alpha = lit * (comet + (1f - comet) * done)
                    val from = center + d * (r - 7.dp.toPx())
                    val to = center + d * (r - 1.dp.toPx())
                    val width = (if (i % 5 == 0) 2f else 1.4f) * density
                    drawLine(colors.line.copy(alpha = 0.7f), from, to, width, StrokeCap.Round)
                    if (alpha > 0.01f) drawLine(tint.copy(alpha = alpha), from, to, width, StrokeCap.Round)
                }
                val disc = r - 12.dp.toPx()
                drawCircle(lerp(colors.panel, colors.accent, fill), disc)
                if (fill < 0.99f) drawCircle(colors.line, disc, style = Stroke(1.dp.toPx()))
                val glyph = 30.dp.toPx()
                inset((size.width - glyph) / 2f, (size.height - glyph) / 2f) {
                    drawGlyph(if (mode == ShutterMode.Held) Glyph.Save else Glyph.Lock, lerp(colors.muted, colors.onAccent, fill))
                }
            }
            Box(
                Modifier.matchParentSize().clip(CircleShape)
                    .clickable(enabled = mode != ShutterMode.Waiting, role = Role.Button, onClick = onClick)
                    .semantics { contentDescription = label },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = if (mode == ShutterMode.Waiting) colors.muted else colors.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Eases a 0..1 sweep per frame: catches up quickly as it grows, falls back gently when reset. */
@Composable
private fun rememberSweep(target: () -> Float): androidx.compose.runtime.State<Float> {
    val value = remember { mutableFloatStateOf(0f) }
    val latest by rememberUpdatedState(target)
    LaunchedEffect(Unit) {
        var previous = 0L
        while (true) withFrameNanos { now ->
            val dt = if (previous == 0L) 0f else ((now - previous) / 1e9f).coerceAtMost(0.05f)
            previous = now
            val goal = latest()
            val delta = goal - value.floatValue
            if (abs(delta) > 0.001f) value.floatValue += delta * (1f - exp(-dt / if (delta > 0f) 0.06f else 0.22f))
        }
    }
    return value
}

@Composable
private fun SideAction(glyph: Glyph, label: String, onClick: () -> Unit) {
    val colors = KanobiTheme.colors
    Column(
        Modifier.width(SideWidth).padding(top = 16.dp).clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).background(colors.panel.copy(alpha = 0.85f), CircleShape).border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, colors.text, Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
