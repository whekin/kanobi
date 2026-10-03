package com.example.dirt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dirt.measurement.Measurement
import com.example.dirt.theme.DisplayFont
import com.example.dirt.theme.Ink
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Line
import com.example.dirt.theme.Muted
import com.example.dirt.theme.Panel
import com.example.dirt.theme.Paper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun JournalScreen(items: List<Measurement>, onDelete: (Measurement) -> Unit, onMeasure: () -> Unit) {
    val dates = remember { SimpleDateFormat("d MMM · HH:mm", Locale.forLanguageTag("ru")) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            PageHeader("Журнал", "Замеры хранятся только на этом телефоне",
                trailing = items.size.toString().padStart(2, '0'))
        }
        if (items.isEmpty()) item(key = "empty") { EmptyJournal(onMeasure) }
        items(items, key = { it.id }) { item ->
            Row(
                Modifier.animateItem().fillMaxWidth().clip(CardShape).background(Panel.copy(alpha = 0.86f))
                    .border(1.dp, Line, CardShape).padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniDial(item.angle, Modifier.size(48.dp).clip(CircleShape).background(Ink))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = Paper, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(dates.format(Date(item.time)), color = Muted, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                Text(degrees(item.angle), fontFamily = DisplayFont, fontSize = 34.sp, color = Paper)
                Text("°", fontFamily = DisplayFont, fontSize = 20.sp, color = Lime, modifier = Modifier.align(Alignment.Top))
                IconButton(onClick = { onDelete(item) }) {
                    GlyphIcon(Glyph.Close, Muted, Modifier.size(16.dp).semantics { contentDescription = "Удалить ${item.name}" })
                }
            }
        }
    }
}

@Composable
private fun EmptyJournal(onMeasure: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel.copy(alpha = 0.7f)).border(1.dp, Line, shape)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AngleDial(24f, Tone.Stable, Modifier.fillMaxWidth().height(220.dp).padding(top = 12.dp))
        Text("Первый вылет — начало", color = Paper, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(
            "Зафиксируй угол и сохрани его.\nТак проще сравнивать форму вылетов.",
            color = Muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp, start = 24.dp, end = 24.dp),
        )
        Button(onClick = onMeasure, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(48.dp)) { Text("Сделать замер") }
    }
}
