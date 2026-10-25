package com.kanobi.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentResponseTest {
    @Test fun instrumentCatchesUpWithin100msAt60And120Hz() {
        for (hz in listOf(60, 120)) {
            for (tau in listOf(InstrumentResponse.DialTimeConstant, InstrumentResponse.TurnTimeConstant)) {
                var shown = 0f
                repeat(hz / 10) { shown = InstrumentResponse.follow(shown, 30f, 1f / hz, tau) }
                println("$hz Hz, tau=$tau: after 100 ms $shown° of 30°")
                assertTrue("Instrument still trails a 30° move at $hz Hz: $shown°", shown >= 27f)
            }
        }
    }

    @Test fun followingNeverOvershootsForEitherDirection() {
        for (target in listOf(-30f, 30f)) {
            var shown = 0f
            repeat(120) {
                shown = InstrumentResponse.follow(shown, target, 1f / 120, InstrumentResponse.DialTimeConstant)
                assertTrue(shown in minOf(0f, target)..maxOf(0f, target))
            }
            assertTrue(kotlin.math.abs(shown - target) < .01f)
        }
    }
}
