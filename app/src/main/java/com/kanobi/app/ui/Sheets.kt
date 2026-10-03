@file:OptIn(ExperimentalMaterial3Api::class)

package com.kanobi.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanobi.app.R
import com.kanobi.app.measurement.Measurement
import com.kanobi.app.measurement.Reading
import com.kanobi.app.theme.DisplayFont
import com.kanobi.app.theme.KanobiTheme
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Bottom sheet with the app's styling; [content] gets `close(then)` that animates out first. */
@Composable
fun AppSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit) {
    val colors = KanobiTheme.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then -> scope.launch { state.hide() }.invokeOnCompletion { then() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = colors.panelHigh, contentColor = colors.text,
        scrimColor = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(36.dp, 4.dp).background(colors.line, CircleShape)) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp)) { content(close) }
    }
}

@Composable
fun SheetTitle(text: String) {
    Text(text, color = KanobiTheme.colors.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun SheetButtons(dismiss: String, onDismiss: () -> Unit, confirm: String, enabled: Boolean = true, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onDismiss, Modifier.weight(1f).height(52.dp)) { Text(dismiss, color = KanobiTheme.colors.text) }
        Button(onClick = onConfirm, Modifier.weight(1f).height(52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp)) {
            Text(confirm, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SaveSheet(angle: Float, defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val colors = KanobiTheme.colors
    AppSheet(onDismiss) { close ->
        val save = { close { onSave(name.ifBlank { defaultName }) } }
        SheetTitle(stringResource(R.string.save_measurement))
        Row {
            Text(degrees(angle), fontFamily = DisplayFont, fontSize = 72.sp, color = colors.accent)
            Text("°", fontFamily = DisplayFont, fontSize = 40.sp, color = colors.accent)
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(80) }, singleLine = true,
            label = { Text(stringResource(R.string.name)) }, placeholder = { Text(defaultName) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        SheetButtons(stringResource(R.string.cancel), { close(onDismiss) }, stringResource(R.string.save)) { save() }
    }
}

/** Zeroing on a level surface, screen up: subtracts the phone's own offset from later readings. */
@Composable
fun ZeroSheet(reading: () -> Reading, available: Boolean, zero: Float, onSet: (Float) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    val colors = KanobiTheme.colors
    AppSheet(onDismiss) { close ->
        val current = reading()
        val canZero = current.ready && current.stable && abs(current.angle) <= 5f
        val (status, color) = when {
            !available -> R.string.zero_unavailable to colors.warn
            !current.ready -> R.string.zero_waiting to colors.muted
            abs(current.angle) > 5f -> R.string.zero_too_tilted to colors.warn
            !current.stable -> R.string.zero_hold_still to colors.warn
            else -> R.string.zero_ready to colors.accent
        }
        SheetTitle(stringResource(R.string.zero_title))
        Text(stringResource(R.string.zero_hint),
            color = colors.muted, fontSize = 14.sp, lineHeight = 21.sp)
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (current.ready) degrees(current.angle) else "—", fontFamily = DisplayFont, fontSize = 56.sp,
                color = if (canZero) colors.accent else colors.text)
            Text("°", fontFamily = DisplayFont, fontSize = 32.sp, color = colors.accent, modifier = Modifier.align(Alignment.Top))
            Spacer(Modifier.width(16.dp))
            Text(stringResource(status), color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        LevelBar(if (current.ready) current.angle else null, canZero, Modifier.fillMaxWidth().height(34.dp).padding(top = 8.dp))
        Text(
            if (abs(zero) < 0.01f) stringResource(R.string.zero_current_factory) else stringResource(R.string.zero_current_offset, degrees(zero)),
            color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp),
        )
        SheetButtons(stringResource(R.string.zero_reset), { close(onReset) }, stringResource(R.string.zero_set), enabled = canZero) {
            val value = current.angle
            close { onSet(value) }
        }
    }
}

/** Deviation meter for zeroing: ±5° range, the marker turns to the accent once zeroing is possible. */
@Composable
private fun LevelBar(angle: Float?, ready: Boolean, modifier: Modifier = Modifier) {
    val colors = KanobiTheme.colors
    Canvas(modifier) {
        val mid = size.height / 2f
        val radius = CornerRadius(size.height / 2f)
        val reach = size.width / 2f - size.height / 2f
        drawRoundRect(colors.line.copy(alpha = 0.45f), cornerRadius = radius)
        drawRoundRect(colors.line, cornerRadius = radius, style = Stroke(1.dp.toPx()))
        for (deg in -5..5) {
            val x = size.width / 2f + deg / 5f * reach
            val tick = if (deg == 0) size.height * 0.36f else size.height * 0.16f
            drawLine(if (deg == 0) colors.text else colors.muted.copy(alpha = 0.5f), Offset(x, mid - tick), Offset(x, mid + tick), 1.dp.toPx())
        }
        if (angle != null) {
            val x = size.width / 2f + (angle / 5f).coerceIn(-1f, 1f) * reach
            val color = if (ready) colors.accent else colors.warn
            drawCircle(color.copy(alpha = 0.25f), size.height * 0.5f, Offset(x, mid))
            drawCircle(color, size.height * 0.24f, Offset(x, mid))
        }
    }
}

@Composable
fun DeleteDialog(item: Measurement, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = KanobiTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = colors.panelHigh, shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.delete_title), color = colors.text) },
        text = { Text("${item.name} · ${degrees(item.angle)}°", color = colors.muted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete), color = colors.warn) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.keep), color = colors.text) } },
    )
}
