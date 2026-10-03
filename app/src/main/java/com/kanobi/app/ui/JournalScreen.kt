package com.kanobi.app.ui

import android.text.format.DateFormat
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanobi.app.R
import com.kanobi.app.measurement.Measurement
import com.kanobi.app.theme.DisplayFont
import com.kanobi.app.theme.KanobiTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun JournalScreen(items: List<Measurement>, onDelete: (Measurement) -> Unit, onMeasure: () -> Unit) {
    val colors = KanobiTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "dMMMHHmm"), locale) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            PageHeader(stringResource(R.string.journal), stringResource(R.string.journal_note),
                trailing = items.size.toString().padStart(2, '0'))
        }
        if (items.isEmpty()) item(key = "empty") { EmptyJournal(onMeasure) }
        items(items, key = { it.id }) { item ->
            Row(
                Modifier.animateItem().fillMaxWidth().clip(CardShape).background(colors.panel.copy(alpha = 0.86f))
                    .border(1.dp, colors.line, CardShape).padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniDial(item.angle, Modifier.size(48.dp).clip(CircleShape).background(colors.background))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = colors.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(dates.format(Date(item.time)), color = colors.muted, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                Text(degrees(item.angle), fontFamily = DisplayFont, fontSize = 34.sp, color = colors.text)
                Text("°", fontFamily = DisplayFont, fontSize = 20.sp, color = colors.accent, modifier = Modifier.align(Alignment.Top))
                val deleteLabel = stringResource(R.string.delete_item, item.name)
                IconButton(onClick = { onDelete(item) }) {
                    GlyphIcon(Glyph.Close, colors.muted, Modifier.size(16.dp).semantics { contentDescription = deleteLabel })
                }
            }
        }
    }
}

@Composable
private fun EmptyJournal(onMeasure: () -> Unit) {
    val colors = KanobiTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.panel.copy(alpha = 0.7f)).border(1.dp, colors.line, shape)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AngleDial(24f, Tone.Stable, Modifier.fillMaxWidth().height(220.dp).padding(top = 12.dp))
        Text(stringResource(R.string.empty_title), color = colors.text, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_body),
            color = colors.muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp, start = 24.dp, end = 24.dp),
        )
        Button(onClick = onMeasure, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.measure)) }
    }
}
