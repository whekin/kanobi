package com.kanobi.app.measurement

/** Measurement source; the benchmark variant can replay identical poses without moving hardware. */
interface TiltSource {
    val available: Boolean
    val reading: Reading
    fun start()
    fun stop()
    fun checkFreshness()
}
