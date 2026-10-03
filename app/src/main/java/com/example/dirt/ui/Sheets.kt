@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.dirt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dirt.measurement.Measurement
import com.example.dirt.theme.Amber
import com.example.dirt.theme.DisplayFont
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Line
import com.example.dirt.theme.Muted
import com.example.dirt.theme.PanelHigh
import com.example.dirt.theme.Paper
import kotlinx.coroutines.launch

/** Bottom sheet with the app's styling; [content] gets `close(then)` that animates out first. */
@Composable
private fun DirtSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then -> scope.launch { state.hide() }.invokeOnCompletion { then() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = PanelHigh, contentColor = Paper,
        scrimColor = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(36.dp, 4.dp).background(Line, CircleShape)) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp)) { content(close) }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, color = Paper, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun SheetButtons(dismiss: String, onDismiss: () -> Unit, confirm: String, enabled: Boolean = true, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onDismiss, Modifier.weight(1f).height(52.dp)) { Text(dismiss, color = Paper) }
        Button(onClick = onConfirm, Modifier.weight(1f).height(52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp)) {
            Text(confirm, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SaveSheet(angle: Float, defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    DirtSheet(onDismiss) { close ->
        val save = { close { onSave(name.ifBlank { defaultName }) } }
        SheetTitle("Сохранить замер")
        Row {
            Text(degrees(angle), fontFamily = DisplayFont, fontSize = 72.sp, color = Lime)
            Text("°", fontFamily = DisplayFont, fontSize = 40.sp, color = Lime)
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(80) }, singleLine = true,
            label = { Text("Название") }, placeholder = { Text(defaultName) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        SheetButtons("Отмена", { close(onDismiss) }, "Сохранить") { save() }
    }
}

@Composable
fun DeleteDialog(item: Measurement, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = PanelHigh, shape = RoundedCornerShape(24.dp),
        title = { Text("Удалить замер?", color = Paper) },
        text = { Text("${item.name} · ${degrees(item.angle)}°", color = Muted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Удалить", color = Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Оставить", color = Paper) } },
    )
}
