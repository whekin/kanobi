package com.kanobi.app.fx

import android.graphics.RuntimeShader
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
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
        Log.w("Kanobi", "AGSL shader rejected", e)
        null
    }
}

/** Used only by the separate benchmark to simulate a higher pixel-shading workload. */
internal val LocalTerrainWorkMultiplier = staticCompositionLocalOf { 1f }

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
 * Full-screen living topographic map. It follows tilt and horizon changes at the display rate and
 * drifts at about 30 fps when the phone is still, from its own frame loop, so sensor updates never
 * recompose it. With [live] off it stays still. The shader renders at 0.66 scale (adaptively 0.5/0.4 under sustained load) into a cached
 * layer: other animations on screen only composite that texture instead of running the shader again.
 */
@Composable
fun AmbientTerrain(ambience: () -> Ambience, accent: Color, base: Color, live: Boolean, modifier: Modifier = Modifier) {
    val latest by rememberUpdatedState(ambience)
    val scene = remember { Scene() }
    val frame = remember { mutableIntStateOf(0) }
    val quality = remember { TerrainQuality() }
    var terrainScale by remember { mutableFloatStateOf(quality.scale) }
    val renderScale = terrainScale * LocalTerrainWorkMultiplier.current
    val display = LocalView.current.display
    val shader = rememberShader(Agsl.TERRAIN)
    val resolver = LocalContext.current.contentResolver
    val systemMotion = remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val motion = live && systemMotion
    LaunchedEffect(motion) {
        var previous = 0L
        var drawn = 0L
        var refreshCheckedAt = 0L
        var refreshRate = display?.refreshRate ?: 60f
        while (true) withFrameNanos { now ->
            // Adaptive displays can switch between 60/90/120 Hz without recomposition.
            // Poll infrequently so a legitimate lower display rate does not lower texture quality.
            if (now - refreshCheckedAt >= 500_000_000L) {
                refreshRate = display?.refreshRate ?: 60f
                refreshCheckedAt = now
            }
            terrainScale = quality.frame(now, refreshRate)
            val busy = scene.advance(latest(), if (previous == 0L) 0f else (now - previous) / 1e9f, motion)
            previous = now
            if (busy || drawn == 0L || (motion && now - drawn >= 32_000_000L)) {
                drawn = now
                frame.intValue++
            }
        }
    }
    Spacer(modifier.renderedAt(renderScale).drawBehind {
        frame.intValue
        scene.size = size / renderScale
        if (shader == null) {
            drawRect(base)
            return@drawBehind
        }
        val focus = scene.focusOr(Offset(scene.size.width / 2f, scene.size.height * 0.3f)) * renderScale
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uDensity", density * renderScale)
        shader.setFloatUniform("uTime", scene.time)
        shader.setFloatUniform("uTilt", scene.tilt)
        shader.setFloatUniform("uFocus", focus.x, focus.y)
        shader.setFloatUniform("uGlow", scene.glow[0], scene.glow[1], scene.glow[2])
        shader.setFloatUniform("uPulse", scene.pulseAge)
        shader.setFloatUniform("uAccent", accent.red, accent.green, accent.blue)
        shader.setFloatUniform("uBase", base.red, base.green, base.blue)
        shader.setFloatUniform("uHorizon", scene.horizon)
        shader.setFloatUniform("uUp", scene.up.x, scene.up.y)
        shader.setFloatUniform("uClear", scene.clear / density)
        drawRect(ShaderBrush(shader))
    })
}

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

    /** Steps towards [target]; returns true while anything still visibly moves. */
    fun advance(target: Ambience, dt: Float, motion: Boolean): Boolean {
        this.motion = motion
        val step = dt.coerceIn(0f, 0.1f)
        if (motion) time += step
        // The horizon has to keep up with the phone; light and glow can take their time.
        val slow = if (motion) 1f - exp(-step / 0.35f) else 1f
        val fast = if (motion) 1f - exp(-step / 0.1f) else 1f
        var busy = false
        fun ease(current: Float, goal: Float, k: Float, epsilon: Float): Float {
            if (abs(goal - current) > epsilon) busy = true
            return current + (goal - current) * k
        }
        tilt = ease(tilt, target.tilt, slow, 0.3f)
        horizon = ease(horizon, target.horizon, fast, 0.002f)
        clear = ease(clear, target.clear, fast, 0.5f)
        for (i in 0..2) glow[i] = ease(glow[i], target.glow.channel(i) * target.energy, slow, 0.002f)
        val roll = Math.toRadians(target.roll.toDouble())
        val aimUp = Offset(sin(roll).toFloat(), -cos(roll).toFloat())
        if (horizon > 0.002f && (aimUp - up).getDistance() > 0.003f) busy = true
        up = lerp(up, aimUp, fast).let { it / max(it.getDistance(), 1e-3f) }
        val aim = when {
            target.focus.isSpecified -> target.focus
            size.isEmpty() -> Offset.Unspecified
            else -> Offset(size.width / 2f, size.height * 0.3f)
        }
        focus = when {
            !aim.isSpecified -> focus
            focus.isSpecified -> {
                if ((aim - focus).getDistance() > 0.5f) busy = true
                lerp(focus, aim, fast)
            }
            else -> aim
        }
        if (target.pulse != pulse) {
            pulse = target.pulse
            pulseAt = time
        }
        return busy || pulseAge in 0f..1.6f
    }

    fun focusOr(default: Offset) = focus.takeIf { it.isSpecified } ?: default
}

private fun Color.channel(i: Int) = when (i) {
    0 -> red
    1 -> green
    else -> blue
}
