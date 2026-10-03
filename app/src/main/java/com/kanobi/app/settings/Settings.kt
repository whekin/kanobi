package com.kanobi.app.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

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

    /** Offset subtracted from every reading, set by zeroing on a level surface screen up. */
    private var zeroState by mutableFloatStateOf(prefs.getFloat("zero", 0f))
    var zero: Float
        get() = zeroState
        set(value) {
            zeroState = value
            prefs.edit { putFloat("zero", value) }
        }

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
