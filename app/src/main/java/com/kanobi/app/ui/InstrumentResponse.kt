package com.kanobi.app.ui

import kotlin.math.exp

/** Measured instrument response, independent of the frame rate. */
internal object InstrumentResponse {
    const val DialTimeConstant = 0.025f
    const val TurnTimeConstant = 0.025f
    const val EdgeTimeConstant = 0.06f

    fun follow(current: Float, target: Float, dtSeconds: Float, timeConstant: Float): Float =
        current + (target - current) * (1f - exp(-dtSeconds / timeConstant))
}
