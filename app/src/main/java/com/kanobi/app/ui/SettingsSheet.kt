package com.kanobi.app.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanobi.app.R
import com.kanobi.app.settings.AccentMode
import com.kanobi.app.settings.Settings
import com.kanobi.app.theme.BrandPalette
import com.kanobi.app.theme.DisplayFont
import com.kanobi.app.theme.KanobiTheme

const val RepositoryUrl = "https://github.com/whekin/kanobi"

@Composable
fun SettingsSheet(settings: Settings, systemAccent: Color, onCalibrate: () -> Unit, onGuide: () -> Unit, onDismiss: () -> Unit) {
    val colors = KanobiTheme.colors
    AppSheet(onDismiss) { close ->
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetTitle(stringResource(R.string.settings))
            Caption(stringResource(R.string.color))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentOption(stringResource(R.string.color_lime), BrandPalette.accent, settings.accent == AccentMode.Brand, Modifier.weight(1f)) {
                    settings.accent = AccentMode.Brand
                }
                AccentOption(stringResource(R.string.color_system), systemAccent, settings.accent == AccentMode.System, Modifier.weight(1f)) {
                    settings.accent = AccentMode.System
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { close(onCalibrate) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.zero_title), color = colors.text, fontSize = 16.sp)
                    Text(stringResource(R.string.calibration_note), color = colors.muted, fontSize = 12.sp)
                }
                Text(if (settings.calibration == com.kanobi.app.measurement.Calibration()) stringResource(R.string.calibration_factory) else stringResource(R.string.calibration_custom),
                    color = colors.accent, fontSize = 14.sp)
                Text("  ›", color = colors.muted, fontSize = 18.sp)
            }
            TextButton(onClick = { close(onGuide) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.guide_title), color = colors.accent)
            }
            Toggle(stringResource(R.string.haptics), null, settings.haptics) { settings.haptics = it }
            Toggle(stringResource(R.string.live_background), stringResource(R.string.live_background_note), settings.live) { settings.live = it }
            HorizontalDivider(color = colors.line, modifier = Modifier.padding(vertical = 18.dp))
            About()
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text.uppercase(), color = KanobiTheme.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun AccentOption(label: String, swatch: Color, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = KanobiTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.height(56.dp).clip(shape).background(if (selected) swatch.copy(alpha = 0.12f) else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) swatch else colors.line, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).background(swatch, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(label, color = colors.text, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun Toggle(title: String, note: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = KanobiTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.text, fontSize = 16.sp)
            if (note != null) Text(note, color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent, checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.muted, uncheckedTrackColor = colors.panel, uncheckedBorderColor = colors.line,
            ),
        )
    }
}

@Composable
private fun About() {
    val colors = KanobiTheme.colors
    val context = LocalContext.current
    val links = LocalUriHandler.current
    val version = remember {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        "${info.versionName} (${info.longVersionCode})"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(Glyph.Logo, colors.accent, Modifier.size(40.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("KANOBI", fontFamily = DisplayFont, fontSize = 26.sp, letterSpacing = 2.sp, color = colors.text)
            Text(stringResource(R.string.about_meaning), color = colors.muted, fontSize = 13.sp)
        }
    }
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.version, version), color = colors.muted, fontSize = 13.sp)
        Text(stringResource(R.string.made_by), color = colors.muted, fontSize = 13.sp)
        Text(
            stringResource(R.string.source_code), color = colors.accent, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { links.openUri(RepositoryUrl) }
                .padding(vertical = 6.dp),
        )
        Text(stringResource(R.string.fonts), color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
