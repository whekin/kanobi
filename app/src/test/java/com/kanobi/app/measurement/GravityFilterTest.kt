package com.kanobi.app.measurement

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class GravityFilterTest {
    @Test fun rawAccelerometerRespondsTo30DegreeTiltWithin100ms() {
        val filter = GravityFilter()
        filter.update(floatArrayOf(0f, 0f, 9.81f), .01f)
        var angle = 0f
        repeat(10) {
            val v = filter.update(floatArrayOf(0f, 4.905f, 8.496f), .01f)
            angle = AngleMath.inclination(v[0], v[1], v[2])!!
        }
        println("Fallback angle after 100 ms: $angle° of 30°")
        assertTrue(angle >= 27f)
    }

    @Test fun suppressesSmallStationaryNoiseAndResetsOnRestart() {
        val filter = GravityFilter()
        filter.update(floatArrayOf(0f, 0f, 9.81f), .01f)
        repeat(100) { i ->
            val v = filter.update(floatArrayOf(0f, if (i % 2 == 0) .015f else -.015f, 9.81f), .01f)
            assertTrue(abs(AngleMath.inclination(v[0], v[1], v[2])!!) < .03f)
        }
        filter.clear()
        val v = filter.update(floatArrayOf(0f, 4.905f, 8.496f), .01f)
        assertEquals(30f, AngleMath.inclination(v[0], v[1], v[2])!!, .01f)
    }
}
