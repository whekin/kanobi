package com.kanobi.app.measurement

import kotlin.math.exp
import kotlin.math.sqrt

/** Only used for the raw accelerometer fallback. Fused gravity needs no second low-pass. */
internal class GravityFilter {
    val vector = FloatArray(3)
    private var initialized = false

    fun clear() { initialized = false }

    fun update(values: FloatArray, dtSeconds: Float): FloatArray {
        if (!initialized) {
            values.copyInto(vector, endIndex = 3)
            initialized = true
            return vector
        }
        val difference = sqrt((0..2).sumOf { i ->
            val d = (values[i] - vector[i]).toDouble()
            d * d
        }).toFloat()
        // Fast for a real repositioning; quiet for sub-degree hand tremor at rest.
        val moving = ((difference - .08f) / .35f).coerceIn(0f, 1f)
        val tau = .12f + (.025f - .12f) * moving
        val blend = 1f - exp(-dtSeconds.coerceAtLeast(0f) / tau)
        for (i in 0..2) vector[i] += (values[i] - vector[i]) * blend
        return vector
    }
}
