package com.kanobi.app.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kanobi.app.R
import com.kanobi.app.fx.Ambience
import com.kanobi.app.fx.AmbientTerrain
import com.kanobi.app.measurement.Calibration
import com.kanobi.app.measurement.Measurement
import com.kanobi.app.measurement.MeasurementStore
import com.kanobi.app.measurement.Reading
import com.kanobi.app.measurement.TiltSource
import com.kanobi.app.measurement.TiltSensor
import com.kanobi.app.settings.Settings
import com.kanobi.app.theme.DisplayFont
import com.kanobi.app.theme.KanobiTheme
import com.kanobi.app.theme.Palette
import com.kanobi.app.theme.systemPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Where the readout sits on screen, so the terrain glow, the lock pulse and the horizon centre
 * on it. [lift] is how far it moves down when the phone stands on its edge, and [clear] the radius
 * kept free around it there.
 */
class FocusPoint {
    var center: Offset = Offset.Unspecified
    var lift = 0f
    var clear = 0f
}

private enum class Sheet { Save, Settings, Zero, Guide }

@Composable
fun KanobiApp(settings: Settings, sensorSource: TiltSource? = null) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = KanobiTheme.colors
    val sensor = remember(sensorSource) { sensorSource ?: TiltSensor(context.applicationContext) }
    val store = remember { MeasurementStore(context.applicationContext) }
    val systemAccent = remember { systemPalette(context).accent }
    var journal by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(if (settings.guideSeen) null else Sheet.Guide) }
    var held by rememberSaveable { mutableStateOf<Float?>(null) }
    var measurements by remember { mutableStateOf(store.load()) }
    var pendingDelete by remember { mutableStateOf<Measurement?>(null) }
    var pulses by remember { mutableIntStateOf(0) }
    val focus = remember { FocusPoint() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { text -> scope.launch { snackbar.showSnackbar(text) } }
    val savedText = stringResource(R.string.saved)
    val zeroDone = stringResource(R.string.zero_done)
    val zeroRestored = stringResource(R.string.zero_restored)

    SensorWhile(sensor, active = !journal || sheet == Sheet.Zero)
    BackHandler(enabled = journal) { journal = false }

    Box(Modifier.fillMaxSize().background(colors.background).then(if (journal) Modifier else Modifier.keepScreenOn())) {
        val turn = screenTurn()
        AmbientTerrain(
            { ambience(journal, sensor.reading, settings.calibration, held != null, focus, turn, pulses, colors) },
            colors.accent, colors.background, settings.live, Modifier.fillMaxSize(),
        )
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            TopBar(journal, measurements.size, onJournal = { journal = !journal }, onSettings = { sheet = Sheet.Settings })
            AnimatedContent(
                journal, Modifier.weight(1f), label = "page",
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 60)) + scaleIn(tween(240, delayMillis = 60), initialScale = 0.97f))
                        .togetherWith(fadeOut(tween(120)))
                },
            ) { showJournal ->
                if (showJournal) {
                    JournalScreen(measurements, onDelete = { pendingDelete = it }, onMeasure = { journal = false })
                } else {
                    MeasureScreen(
                        reading = { sensor.reading }, available = sensor.available, calibration = settings.calibration, held = held,
                        live = settings.live, focus = focus,
                        onLock = { angle ->
                            held = angle
                            if (settings.live) pulses++
                            if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        },
                        onRelease = { held = null },
                        onSave = { sheet = Sheet.Save },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)) {
            AppSnackbar(it)
        }
    }

    val angle = held
    when (sheet) {
        Sheet.Save -> if (angle != null) SaveSheet(
            angle, defaultName = stringResource(R.string.default_name, measurements.size + 1),
            onSave = { name ->
                measurements = store.save(angle, name)
                held = null
                sheet = null
                notify(savedText)
            },
            onDismiss = { sheet = null },
        )
        Sheet.Settings -> SettingsSheet(settings, systemAccent, onCalibrate = { sheet = Sheet.Zero },
            onGuide = { sheet = Sheet.Guide }, onDismiss = { sheet = null })
        Sheet.Guide -> GuideSheet(onDismiss = { settings.guideSeen = true; sheet = null })
        Sheet.Zero -> ZeroSheet(
            reading = { sensor.reading }, available = sensor.available, calibration = settings.calibration,
            onSet = { side, value ->
                settings.setZero(side, value)
                held = null
                sheet = null
                notify(zeroDone)
            },
            onReset = { side ->
                settings.setZero(side, 0f)
                held = null
                sheet = null
                notify(zeroRestored)
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
    pendingDelete?.let { item ->
        DeleteDialog(
            item,
            onConfirm = {
                measurements = store.delete(item.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Wordmark, the journal toggle with the number of saved measurements, and settings. */
@Composable
private fun TopBar(journal: Boolean, count: Int, onJournal: () -> Unit, onSettings: () -> Unit) {
    val colors = KanobiTheme.colors
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GlyphIcon(Glyph.Logo, colors.accent, Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text("KANOBI", fontFamily = DisplayFont, fontSize = 26.sp, letterSpacing = 2.sp, color = colors.text)
        Spacer(Modifier.weight(1f))
        val journalLabel = if (journal) stringResource(R.string.close_journal)
            else pluralStringResource(R.plurals.journal_entries, count, count)
        PillButton(journalLabel, onJournal) {
            if (journal) {
                GlyphIcon(Glyph.Close, colors.text, Modifier.size(16.dp))
            } else {
                GlyphIcon(Glyph.Book, colors.text, Modifier.size(18.dp))
                if (count > 0) Text(count.toString(), fontFamily = DisplayFont, fontSize = 18.sp, color = colors.accent,
                    modifier = Modifier.padding(start = 8.dp))
            }
        }
        PillButton(stringResource(R.string.settings), onSettings) { GlyphIcon(Glyph.Settings, colors.text, Modifier.size(18.dp)) }
    }
}

/** Runs the sensor only while the measure page is visible and the activity is resumed. */
@Composable
private fun SensorWhile(sensor: TiltSource, active: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(sensor, lifecycle, active) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (active) sensor.start()
                Lifecycle.Event.ON_PAUSE -> sensor.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            sensor.stop()
        }
    }
    LaunchedEffect(sensor, active) {
        while (active) {
            delay(500)
            sensor.checkFreshness()
        }
    }
}

private fun ambience(
    journal: Boolean, reading: Reading, calibration: Calibration, held: Boolean, focus: FocusPoint, turn: Float, pulses: Int, colors: Palette,
): Ambience {
    if (journal) return Ambience(0f, colors.accent, 0.18f, Offset.Unspecified, pulses)
    val edge = edgeWeight(reading)
    return Ambience(
        tilt = if (reading.ready) calibration.correct(reading) else 0f,
        glow = if (held || reading.stable) colors.accent else colors.warn,
        energy = when {
            held -> 1f
            !reading.ready -> 0f
            reading.stable -> 0.7f
            else -> 0.35f
        },
        focus = if (focus.center.isSpecified) focus.center + Offset(0f, focus.lift * edge) else Offset.Unspecified,
        pulse = pulses,
        horizon = edge,
        roll = reading.roll - turn,
        clear = focus.clear * edge,
    )
}
