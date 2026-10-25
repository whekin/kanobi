package com.kanobi.app.measurement

import org.junit.Assert.*
import org.junit.Test

class CalibrationTest {
    @Test fun cameraBumpCorrectionDoesNotLeakOntoEitherEdge() {
        val calibration = Calibration(screenUp = 1f)
        assertEquals(0f, calibration.correct(Reading(angle = 1f, upZ = 1f)), .001f)
        assertEquals(0f, calibration.correct(Reading(upX = 1f, upZ = 0f)), .001f)
        assertEquals(0f, calibration.correct(Reading(upX = -1f, upZ = 0f)), .001f)
    }

    @Test fun independentOffsetsFollowTheContactSideAtTakeoffAngles() {
        val calibration = Calibration(1f, 2f, -1f)
        assertEquals(29f, calibration.correct(Reading(angle = 30f, upY = .5f, upZ = .866f)), .001f)
        assertEquals(28f, calibration.correct(Reading(angle = 30f, upX = .866f, upY = .5f, upZ = 0f)), .001f)
        assertEquals(31f, calibration.correct(Reading(angle = 30f, upX = -.866f, upY = .5f, upZ = 0f)), .001f)
        assertEquals(Calibration(1f, 0f, -1f), calibration.withOffset(ContactSide.LeftEdge, 0f))
    }

    @Test fun zeroRequiresAStableLevelSupportedContact() {
        assertTrue(Reading(ready = true, stable = true).canCalibrate)
        assertTrue(Reading(ready = true, stable = true, upX = 1f, upZ = 0f).canCalibrate)
        assertFalse(Reading(ready = true, stable = false).canCalibrate)
        assertFalse(Reading(ready = false, stable = true).canCalibrate)
        assertFalse(Reading(angle = 6f, ready = true, stable = true).canCalibrate)
        assertFalse(Reading(ready = true, stable = true, upX = .7f, upZ = .7f).canCalibrate)
        assertFalse(Reading(ready = true, stable = true, upZ = -1f).canCalibrate)
    }
}
