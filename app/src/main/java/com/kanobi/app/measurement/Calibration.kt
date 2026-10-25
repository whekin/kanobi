package com.kanobi.app.measurement

import kotlin.math.abs

/** Contact surfaces have different mechanical offsets (notably the camera bump). */
enum class ContactSide { ScreenUp, LeftEdge, RightEdge }

data class Calibration(val screenUp: Float = 0f, val leftEdge: Float = 0f, val rightEdge: Float = 0f) {
    fun offset(side: ContactSide): Float = when (side) {
        ContactSide.ScreenUp -> screenUp
        ContactSide.LeftEdge -> leftEdge
        ContactSide.RightEdge -> rightEdge
    }

    fun withOffset(side: ContactSide, value: Float): Calibration = when (side) {
        ContactSide.ScreenUp -> copy(screenUp = value)
        ContactSide.LeftEdge -> copy(leftEdge = value)
        ContactSide.RightEdge -> copy(rightEdge = value)
    }

    fun correct(reading: Reading): Float =
        (reading.angle - offset(reading.contactSide)).coerceIn(-90f, 90f)
}

val Reading.contactSide: ContactSide
    get() = if (abs(upX) > abs(upZ)) {
        if (upX > 0f) ContactSide.LeftEdge else ContactSide.RightEdge
    } else ContactSide.ScreenUp

/** Near level with a supported contact face, rather than balanced between two faces. */
val Reading.canCalibrate: Boolean
    get() = ready && stable && abs(angle) <= 5f && when (contactSide) {
        ContactSide.ScreenUp -> upZ > .85f
        ContactSide.LeftEdge, ContactSide.RightEdge -> abs(upX) > .85f
    }
