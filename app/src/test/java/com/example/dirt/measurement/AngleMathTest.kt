package com.example.dirt.measurement

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class AngleMathTest {
    @Test fun longAxisReturnsKnownInclinationsWithEitherRoll() {
        for (degrees in listOf(-90f, -60f, -30f, 0f, 30f, 60f, 90f)) {
            val rad = Math.toRadians(degrees.toDouble())
            val along = (9.81 * sin(rad)).toFloat()
            val normal = (9.81 * cos(rad)).toFloat()
            assertEquals(degrees, AngleMath.inclination(0f, along, normal)!!, .02f)
            assertEquals(degrees, AngleMath.inclination(normal, along, 0f)!!, .02f)
        }
    }
    @Test fun rejectsFreeFallAndNonFiniteReadings() {
        assertNull(AngleMath.inclination(0f, 0f, 0f))
        assertNull(AngleMath.inclination(Float.NaN, 1f, 1f))
    }
    @Test fun stableReadingRequiresTimeAndMovingReadingNeverLocks() {
        val steady = StabilityWindow()
        assertFalse(steady.add(0L, 30f))
        for (i in 1..6) assertFalse(steady.add(i * 100L, 30f))
        assertTrue(steady.add(700L, 30.1f))
        assertFalse(steady.add(800L, 35f))
        for (i in 9..16) steady.add(i * 100L, 35f)
        assertTrue(steady.add(1700L, 35f))
        steady.clear()
        assertFalse(steady.add(1800L, 35f))
        val moving = StabilityWindow()
        for (i in 0..30) assertFalse(moving.add(i * 100L, i.toFloat()))
    }
    @Test fun progressGrowsWhileStillAndRestartsOnMovement() {
        val window = StabilityWindow()
        window.add(0L, 10f)
        assertEquals(0f, window.progress, 0f)
        for (i in 1..3) window.add(i * 100L, 10.2f)
        assertEquals(300f / 650f, window.progress, .01f)
        window.add(400L, 12f)
        assertEquals(0f, window.progress, 0f)
        for (i in 5..10) window.add(i * 100L, 12.1f)
        assertEquals(600f / 650f, window.progress, .01f)
        window.add(1100L, 12.1f)
        assertEquals(1f, window.progress, 0f)
        window.clear()
        assertEquals(0f, window.progress, 0f)
    }
}
