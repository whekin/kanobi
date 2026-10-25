package com.kanobi.app.benchmark

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kanobi.app.fx.LocalTerrainWorkMultiplier
import com.kanobi.app.measurement.Reading
import com.kanobi.app.measurement.TiltSource
import com.kanobi.app.settings.Settings
import com.kanobi.app.theme.KanobiTheme
import com.kanobi.app.theme.systemPalette
import com.kanobi.app.ui.KanobiApp
import kotlin.math.*

/** Only compiled into the separate .benchmark APK. Never a source of real measurements. */
class ReplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        val settings = Settings(applicationContext).apply {
            guideSeen = true
            haptics = false
            live = intent.getStringExtra("effects") != "off"
        }
        val source = PoseReplay()
        val palette = systemPalette(this)
        val load = sqrt(intent.getFloatExtra("gpu-load", 1f).coerceIn(1f, 4f))
        setContent {
            CompositionLocalProvider(LocalTerrainWorkMultiplier provides load) {
                KanobiTheme(palette) { KanobiApp(settings, source) }
            }
        }
    }
}

private class PoseReplay : TiltSource {
    override val available = true
    override var reading by mutableStateOf(Reading())
        private set
    private val handler = Handler(Looper.getMainLooper())
    private var started = 0L
    private var next = 0L
    private var previousAngle = 0f
    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            val time = (now - started) / 1000.0
            val pitch = .8 * sin(time * 2.0 * PI / 1.6)
            val roll = PI / 2.0 * sin(time * 2.0 * PI / 2.4)
            val angle = Math.toDegrees(pitch).toFloat()
            reading = Reading(angle, true, false, 0f, abs(angle - previousAngle) * 100f,
                (cos(pitch) * sin(roll)).toFloat(), sin(pitch).toFloat(), (cos(pitch) * cos(roll)).toFloat())
            previousAngle = angle
            next = max(next + 10L, now + 1L)
            handler.postAtTime(this, next)
        }
    }
    override fun start() {
        stop()
        started = SystemClock.uptimeMillis()
        next = started
        handler.post(tick)
    }
    override fun stop() { handler.removeCallbacks(tick); reading = reading.copy(ready = false) }
    override fun checkFreshness() = Unit
}
