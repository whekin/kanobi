package com.example.dirt.fx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Void
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Compiles an AGSL shader once; null if this Android's SkSL rejects it, leaving the plain look. */
@Composable
fun rememberShader(source: String): RuntimeShader? = remember(source) {
    try {
        RuntimeShader(source)
    } catch (e: IllegalArgumentException) {
        Log.w("DIRT", "AGSL shader rejected", e)
        null
    }
}

/**
 * What the terrain behind the app reacts to. Read every frame, never during composition.
 * [horizon] fades in the real horizon (0..1); [roll] is the clockwise screen rotation that is level;
 * the horizon line leaves a gap of [clear] px around the focus for the readout.
 */
class Ambience(
    val tilt: Float,
    val glow: Color,
    val energy: Float,
    val focus: Offset,
    val pulse: Int,
    val horizon: Float = 0f,
    val roll: Float = 0f,
    val clear: Float = 0f,
)

/**
 * Full-screen living topographic map. It redraws at about 30 fps from its own frame loop, so
 * sensor updates never recompose it. The shader renders at [TerrainScale] into a cached layer:
 * other animations on screen only composite that texture instead of running the shader again.
 */
@Composable
fun AmbientTerrain(ambience: () -> Ambience, modifier: Modifier = Modifier) {
    val latest by rememberUpdatedState(ambience)
    val scene = remember { Scene() }
    val frame = remember { mutableIntStateOf(0) }
    val shader = rememberShader(Agsl.TERRAIN)
    val resolver = LocalContext.current.contentResolver
    val motion = remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    LaunchedEffect(motion) {
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                scene.advance(latest(), if (previous == 0L) 0f else (now - previous) / 1e9f, motion)
                previous = now
            }
            frame.intValue++
            delay(24)
        }
    }
    Spacer(modifier.renderedAt(TerrainScale).drawBehind {
        frame.intValue
        scene.size = size / TerrainScale
        if (shader == null) {
            drawRect(Void)
            return@drawBehind
        }
        val focus = scene.focusOr(Offset(scene.size.width / 2f, scene.size.height * 0.3f)) * TerrainScale
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uDensity", density * TerrainScale)
        shader.setFloatUniform("uTime", scene.time)
        shader.setFloatUniform("uTilt", scene.tilt)
        shader.setFloatUniform("uFocus", focus.x, focus.y)
        shader.setFloatUniform("uGlow", scene.glow[0], scene.glow[1], scene.glow[2])
        shader.setFloatUniform("uPulse", scene.pulseAge)
        shader.setFloatUniform("uAccent", Lime.red, Lime.green, Lime.blue)
        shader.setFloatUniform("uHorizon", scene.horizon)
        shader.setFloatUniform("uUp", scene.up.x, scene.up.y)
        shader.setFloatUniform("uClear", scene.clear / density)
        drawRect(ShaderBrush(shader))
    })
}

/** Share of the screen resolution the terrain is shaded at; the soft map upscales cleanly. */
private const val TerrainScale = 0.66f

/** Lays out at full size but draws into an offscreen layer [scale] times smaller, stretched back. */
private fun Modifier.renderedAt(scale: Float) = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(
        Constraints.fixed((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1)),
    )
    layout(width, height) {
        placeable.placeWithLayer(0, 0) {
            scaleX = 1f / scale
            scaleY = 1f / scale
            transformOrigin = TransformOrigin(0f, 0f)
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }
}

/** Eased state of the terrain in screen pixels, stepped by the frame loop. */
private class Scene {
    var size = Size.Zero
    var time = 0f
    var tilt = 0f
    val glow = FloatArray(3)
    var focus = Offset.Unspecified
    var horizon = 0f
    var clear = 0f
    var up = Offset(0f, -1f)
    private var motion = true
    private var pulse = 0
    private var pulseAt = -100f

    /** Seconds since the last lock pulse, or -1 while there is nothing to show. */
    val pulseAge get() = if (motion) time - pulseAt else -1f

    fun advance(target: Ambience, dt: Float, motion: Boolean) {
        this.motion = motion
        val step = dt.coerceIn(0f, 0.1f)
        if (motion) time += step
        val k = if (motion) 1f - exp(-step / 0.35f) else 1f
        tilt += (target.tilt - tilt) * k
        horizon += (target.horizon - horizon) * k
        clear += (target.clear - clear) * k
        val roll = Math.toRadians(target.roll.toDouble())
        up = lerp(up, Offset(sin(roll).toFloat(), -cos(roll).toFloat()), k).let { it / max(it.getDistance(), 1e-3f) }
        glow[0] += (target.glow.red * target.energy - glow[0]) * k
        glow[1] += (target.glow.green * target.energy - glow[1]) * k
        glow[2] += (target.glow.blue * target.energy - glow[2]) * k
        val aim = when {
            target.focus.isSpecified -> target.focus
            size.isEmpty() -> Offset.Unspecified
            else -> Offset(size.width / 2f, size.height * 0.3f)
        }
        focus = when {
            !aim.isSpecified -> focus
            focus.isSpecified -> lerp(focus, aim, k)
            else -> aim
        }
        if (target.pulse != pulse) {
            pulse = target.pulse
            pulseAt = time
        }
    }

    fun focusOr(default: Offset) = focus.takeIf { it.isSpecified } ?: default
}

/**
 * Splits red and blue apart by up to [maxShift] while [amount] (0..1) is above zero.
 * [amount] is read inside the layer, so changing it never recomposes the content.
 */
fun Modifier.chromaticAberration(shader: RuntimeShader?, maxShift: Dp = 5.dp, amount: () -> Float): Modifier =
    if (shader == null) this else graphicsLayer {
        val shift = amount().coerceIn(0f, 1f) * maxShift.toPx()
        renderEffect = if (shift > 0.5f) {
            shader.setFloatUniform("uShift", shift, 0f)
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        } else null
    }
