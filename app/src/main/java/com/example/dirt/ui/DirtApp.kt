package com.example.dirt.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dirt.fx.Ambience
import com.example.dirt.fx.AmbientTerrain
import com.example.dirt.measurement.Measurement
import com.example.dirt.measurement.MeasurementStore
import com.example.dirt.measurement.Reading
import com.example.dirt.measurement.TiltSensor
import com.example.dirt.theme.Amber
import com.example.dirt.theme.DisplayFont
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Line
import com.example.dirt.theme.Panel
import com.example.dirt.theme.Paper
import com.example.dirt.theme.Void
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

@Composable
fun DirtApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val sensor = remember { TiltSensor(context.applicationContext) }
    val store = remember { MeasurementStore(context.applicationContext) }
    var journal by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var held by rememberSaveable { mutableStateOf<Float?>(null) }
    var measurements by remember { mutableStateOf(store.load()) }
    var pendingDelete by remember { mutableStateOf<Measurement?>(null) }
    var pulses by remember { mutableIntStateOf(0) }
    val focus = remember { FocusPoint() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SensorWhile(sensor, active = !journal)
    BackHandler(enabled = journal) { journal = false }

    Box(Modifier.fillMaxSize().background(Void).then(if (journal) Modifier else Modifier.keepScreenOn())) {
        val turn = screenTurn()
        AmbientTerrain({ ambience(journal, sensor.reading, held != null, focus, turn, pulses) }, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            TopBar(journal, measurements.size, onToggle = { journal = !journal })
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
                        reading = { sensor.reading }, available = sensor.available, held = held, focus = focus,
                        onLock = { angle ->
                            held = angle
                            pulses++
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        },
                        onRelease = { held = null },
                        onSave = { saving = true },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)) {
            DirtSnackbar(it)
        }
    }

    val angle = held
    if (saving && angle != null) {
        SaveSheet(
            angle, defaultName = "Замер ${measurements.size + 1}",
            onSave = { name ->
                measurements = store.save(angle, name)
                held = null
                saving = false
                scope.launch { snackbar.showSnackbar("Сохранено в журнал") }
            },
            onDismiss = { saving = false },
        )
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

/** Wordmark and the journal toggle, which shows how many measurements are saved. */
@Composable
private fun TopBar(journal: Boolean, count: Int, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(Glyph.Logo, Lime, Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text("DIRT", fontFamily = DisplayFont, fontSize = 28.sp, letterSpacing = 2.sp, color = Paper)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.minimumInteractiveComponentSize().height(40.dp).widthIn(min = 40.dp).clip(CircleShape)
                .background(Panel.copy(alpha = 0.7f)).border(1.dp, Line, CircleShape)
                .clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 12.dp)
                .semantics { contentDescription = if (journal) "Закрыть журнал" else "Журнал, записей: $count" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (journal) {
                GlyphIcon(Glyph.Close, Paper, Modifier.size(16.dp))
            } else {
                GlyphIcon(Glyph.Book, Paper, Modifier.size(18.dp))
                if (count > 0) Text(count.toString(), fontFamily = DisplayFont, fontSize = 18.sp, color = Lime,
                    modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Runs the sensor only while the measure page is visible and the activity is resumed. */
@Composable
private fun SensorWhile(sensor: TiltSensor, active: Boolean) {
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

private fun ambience(journal: Boolean, reading: Reading, held: Boolean, focus: FocusPoint, turn: Float, pulses: Int): Ambience {
    if (journal) return Ambience(0f, Lime, 0.18f, Offset.Unspecified, pulses)
    val edge = edgeWeight(reading)
    return Ambience(
        tilt = if (reading.ready) reading.angle else 0f,
        glow = if (held || reading.stable) Lime else Amber,
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
