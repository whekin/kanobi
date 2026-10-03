package com.example.dirt.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.dirt.R

val Void = Color(0xFF080A07)
val Ink = Color(0xFF0E110D)
val Panel = Color(0xFF141812)
val PanelHigh = Color(0xFF1A1F17)
val Line = Color(0xFF2B3327)
val Lime = Color(0xFFD1F86B)
val Paper = Color(0xFFF1F3E8)
val Muted = Color(0xFF9AA595)
val Amber = Color(0xFFF5C46E)

/** Barlow Condensed has no Cyrillic: use it for numbers and the wordmark only. */
val DisplayFont = FontFamily(Font(R.font.barlow_condensed, FontWeight.SemiBold))

/** Golos Text is a variable font; each weight maps to its wght axis. */
val BodyFont = FontFamily(
    Font(R.font.golos_text, FontWeight.Normal),
    Font(R.font.golos_text, FontWeight.Medium),
    Font(R.font.golos_text, FontWeight.SemiBold),
)

private val AppTypography = Typography().run {
    fun TextStyle.body() = copy(fontFamily = BodyFont)
    copy(
        displayLarge = displayLarge.body(), displayMedium = displayMedium.body(), displaySmall = displaySmall.body(),
        headlineLarge = headlineLarge.body(), headlineMedium = headlineMedium.body(), headlineSmall = headlineSmall.body(),
        titleLarge = titleLarge.body(), titleMedium = titleMedium.body(), titleSmall = titleSmall.body(),
        bodyLarge = bodyLarge.body(), bodyMedium = bodyMedium.body(), bodySmall = bodySmall.body(),
        labelLarge = labelLarge.body(), labelMedium = labelMedium.body(), labelSmall = labelSmall.body(),
    )
}

@Composable
fun DirtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime, onPrimary = Ink, primaryContainer = Lime, onPrimaryContainer = Ink,
            secondary = Muted, onSecondary = Ink, tertiary = Amber, onTertiary = Ink,
            background = Void, onBackground = Paper,
            surface = Panel, onSurface = Paper, surfaceVariant = PanelHigh, onSurfaceVariant = Muted,
            surfaceContainerLowest = Void, surfaceContainerLow = Panel, surfaceContainer = Panel,
            surfaceContainerHigh = PanelHigh, surfaceContainerHighest = PanelHigh,
            inverseSurface = Paper, inverseOnSurface = Ink, outline = Line, outlineVariant = Line,
            error = Amber, onError = Ink,
        ),
        typography = AppTypography,
        content = content,
    )
}
