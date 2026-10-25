package com.kanobi.app.fx

import kotlin.math.abs

/** Reduces only the soft background texture if sustained missed display frames leave no headroom.
 * Never scales instrument text/lines or lowers their update rate. No oscillating quality ramps. */
internal class TerrainQuality {
    private val levels = floatArrayOf(.66f, .5f, .4f)
    private var level = 0
    val scale get() = levels[level]
    private var last = 0L
    private var windowStart = 0L
    private var warmUntil = 0L
    private var period = 0.0
    private var frames = 0
    private var late = 0

    fun frame(now: Long, refreshRate: Float): Float {
        val expected = 1e9 / refreshRate.coerceIn(10f, 240f)
        val dt = now - last
        if (last == 0L || dt > 250_000_000L || abs(expected - period) > expected * .15) {
            last = now
            period = expected
            warmUntil = now + 1_000_000_000L
            resetWindow(now)
            return scale
        }
        last = now
        if (now < warmUntil) {
            resetWindow(now)
            return scale
        }
        frames++
        if (dt > expected * 1.5) late++
        if (now - windowStart >= 2_000_000_000L) {
            if (frames >= 12 && late.toFloat() / frames > .2f && level < levels.lastIndex) level++
            resetWindow(now)
        }
        return scale
    }

    private fun resetWindow(now: Long) {
        windowStart = now
        frames = 0
        late = 0
    }
}
