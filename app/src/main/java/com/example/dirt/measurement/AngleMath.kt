package com.example.dirt.measurement

import kotlin.math.*

/** Inclination of the phone's physical long axis, independent of screen rotation or roll. */
object AngleMath {
    fun inclination(x: Float, y: Float, z: Float): Float? {
        val magnitude = sqrt(x * x + y * y + z * z)
        if (!magnitude.isFinite() || magnitude < 1f) return null
        return Math.toDegrees(asin((y / magnitude).coerceIn(-1f, 1f)).toDouble()).toFloat()
    }
}

/**
 * Stability is a dwell window, not a claim about the sensor's absolute accuracy:
 * the readings must stay within [spread] degrees for at least [dwellMs].
 */
class StabilityWindow(private val dwellMs: Long = 650L, private val spread: Float = 0.6f) {
    private val samples = ArrayDeque<Pair<Long, Float>>()

    /** How much of the dwell the current still run already covers, 0..1. */
    var progress = 0f
        private set

    fun clear() {
        samples.clear()
        progress = 0f
    }

    fun add(timeMs: Long, angle: Float): Boolean {
        samples.addLast(timeMs to angle)
        while (samples.size > 1 && timeMs - samples.first().first > dwellMs + 200L) samples.removeFirst()
        // Keep only the newest run of samples that fits within the allowed spread.
        var low = angle
        var high = angle
        var run = 1
        for (i in samples.size - 2 downTo 0) {
            val value = samples[i].second
            if (max(high, value) - min(low, value) > spread) break
            low = min(low, value)
            high = max(high, value)
            run++
        }
        repeat(samples.size - run) { samples.removeFirst() }
        val span = timeMs - samples.first().first
        val stable = samples.size >= 8 && span >= dwellMs
        progress = if (stable) 1f else (span.toFloat() / dwellMs).coerceIn(0f, 0.97f)
        return stable
    }
}
