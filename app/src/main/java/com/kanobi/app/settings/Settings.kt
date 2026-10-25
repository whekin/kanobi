package com.kanobi.app.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kanobi.app.measurement.Calibration
import com.kanobi.app.measurement.ContactSide

enum class AccentMode { Brand, System }

/** User preferences: persisted right away and observable from Compose. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private var accentState by mutableStateOf(
        AccentMode.entries.firstOrNull { it.name == prefs.getString("accent", null) } ?: AccentMode.Brand,
    )
    var accent: AccentMode
        get() = accentState
        set(value) {
            accentState = value
            prefs.edit { putString("accent", value.name) }
        }

    // Preserve the old screen-up zero; edge offsets start at factory zero.
    var calibration by mutableStateOf(Calibration(
        screenUp = prefs.getFloat("zero", 0f),
        leftEdge = prefs.getFloat("zero_left", 0f),
        rightEdge = prefs.getFloat("zero_right", 0f),
    ))
        private set

    fun setZero(side: ContactSide, value: Float) {
        calibration = calibration.withOffset(side, value)
        prefs.edit {
            putFloat("zero", calibration.screenUp)
            putFloat("zero_left", calibration.leftEdge)
            putFloat("zero_right", calibration.rightEdge)
        }
    }

    var guideSeen: Boolean
        get() = prefs.getBoolean("guide_seen", false)
        set(value) { prefs.edit { putBoolean("guide_seen", value) } }

    private var hapticsState by mutableStateOf(prefs.getBoolean("haptics", true))
    var haptics: Boolean
        get() = hapticsState
        set(value) {
            hapticsState = value
            prefs.edit { putBoolean("haptics", value) }
        }

    /** Drifting map, RGB split and the lock pulse; off keeps the background still to save battery. */
    private var liveState by mutableStateOf(prefs.getBoolean("live", true))
    var live: Boolean
        get() = liveState
        set(value) {
            liveState = value
            prefs.edit { putBoolean("live", value) }
        }
}
