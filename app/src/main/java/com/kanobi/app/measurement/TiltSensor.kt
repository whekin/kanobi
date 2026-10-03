package com.kanobi.app.measurement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sqrt

data class Reading(
    val angle: Float = 0f,
    val ready: Boolean = false,
    val stable: Boolean = false,
    /** Share of the stability dwell already covered, 0..1. */
    val progress: Float = 0f,
    /** Smoothed angular speed in degrees per second. */
    val motion: Float = 0f,
    /** "Up" projected on the screen plane as a share of gravity: x towards the right edge, y towards the top. */
    val upX: Float = 0f,
    val upY: Float = 0f,
) {
    /** 0 lying flat, 1 with the screen vertical; below ~0.3 the roll direction is mostly noise. */
    val inPlane get() = hypot(upX, upY)

    /** Clockwise screen rotation, in degrees, that keeps content level with the horizon. */
    val roll get() = Math.toDegrees(atan2(upX, upY).toDouble()).toFloat()
}

class TiltSensor(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val available get() = sensor != null
    var reading by mutableStateOf(Reading())
        private set
    private val filtered = FloatArray(3)
    private var initialized = false
    private var lastEvent = 0L
    private var lastTimestamp = 0L
    private var motion = 0f
    private val window = StabilityWindow()
    private var reliable = true

    fun start() {
        stop()
        initialized = false
        reliable = true
        sensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        manager.unregisterListener(this)
        invalidate()
    }

    fun checkFreshness() {
        if (SystemClock.elapsedRealtime() - lastEvent > 1500L) invalidate()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val dt = if (initialized) (event.timestamp - lastTimestamp) / 1_000_000_000f else 0f
        lastTimestamp = event.timestamp
        val alpha = if (sensor?.type == Sensor.TYPE_ACCELEROMETER) exp(-dt / 0.22f) else 0f
        for (i in 0..2) filtered[i] = if (!initialized) event.values[i]
            else alpha * filtered[i] + (1f - alpha) * event.values[i]
        initialized = true
        lastEvent = SystemClock.elapsedRealtime()
        val angle = AngleMath.inclination(filtered[0], filtered[1], filtered[2])
        if (angle == null || !reliable) {
            invalidate()
            return
        }
        motion = if (reading.ready && dt > 0f) motion + (abs(angle - reading.angle) / dt - motion) * (1f - exp(-dt / 0.12f)) else 0f
        val stable = window.add(lastEvent, angle)
        val magnitude = sqrt(filtered[0] * filtered[0] + filtered[1] * filtered[1] + filtered[2] * filtered[2])
        reading = Reading(angle, true, stable, window.progress, motion, filtered[0] / magnitude, filtered[1] / magnitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        reliable = accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
        if (!reliable) invalidate()
    }

    private fun invalidate() {
        window.clear()
        motion = 0f
        reading = reading.copy(ready = false, stable = false, progress = 0f, motion = 0f)
    }
}
