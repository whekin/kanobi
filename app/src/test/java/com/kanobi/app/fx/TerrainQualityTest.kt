package com.kanobi.app.fx

import org.junit.Assert.assertEquals
import org.junit.Test

class TerrainQualityTest {
    @Test fun smooth60And120HzKeepFullTextureQuality() {
        for (rate in listOf(60, 120)) {
            val quality = TerrainQuality()
            repeat(rate * 10) { quality.frame(1L + it * (1_000_000_000L / rate), rate.toFloat()) }
            assertEquals(.66f, quality.scale, 0f)
        }
    }

    @Test fun sustainedMissedFramesReduceBackgroundWorkAndStopAtFloor() {
        val quality = TerrainQuality()
        repeat(100) { quality.frame(1L + it * 33_333_333L, 120f) }
        assertEquals(.5f, quality.scale, 0f)
        repeat(300) { quality.frame(3_333_333_301L + it * 33_333_333L, 120f) }
        assertEquals(.4f, quality.scale, 0f)
    }

    @Test fun resumeAndRefreshRateChangesDoNotCountAsMissedFrames() {
        val quality = TerrainQuality()
        repeat(120) { quality.frame(1L + it * 8_333_333L, 120f) }
        repeat(360) { quality.frame(10_000_000_001L + it * 16_666_666L, 60f) }
        assertEquals(.66f, quality.scale, 0f)
    }
}
