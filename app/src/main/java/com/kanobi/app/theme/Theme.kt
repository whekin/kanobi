package com.kanobi.app.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kanobi.app.R

/** Colours of the instrument: one accent over near-black neutrals tinted towards it. */
@Immutable
data class Palette(
    val accent: Color,
    val onAccent: Color,
    /** Readings that are still moving. */
    val warn: Color,
    val background: Color,
    val panel: Color,
    val panelHigh: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
)

val BrandPalette = Palette(
    accent = Color(0xFFD1F86B), onAccent = Color(0xFF0E110D), warn = Color(0xFFF5C46E),
    background = Color(0xFF080A07), panel = Color(0xFF141812), panelHigh = Color(0xFF1A1F17),
    line = Color(0xFF2B3327), text = Color(0xFFF1F3E8), muted = Color(0xFF9AA595),
)

/** Material You: the system accent from the wallpaper, everything else derived from it. */
fun systemPalette(context: Context): Palette {
    val scheme = dynamicDarkColorScheme(context)
    val accent = scheme.primary
    fun tint(base: Float, amount: Float) =
        Color(base + accent.red * amount, base + accent.green * amount, base + accent.blue * amount)
    return Palette(
        accent = accent, onAccent = scheme.onPrimary, warn = scheme.tertiary,
        background = tint(0.018f, 0.016f), panel = tint(0.055f, 0.03f), panelHigh = tint(0.07f, 0.04f),
        line = tint(0.12f, 0.065f), text = scheme.onSurface, muted = scheme.onSurfaceVariant,
    )
}

val LocalPalette = staticCompositionLocalOf { BrandPalette }

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

object KanobiTheme {
    val colors: Palette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
}

@Composable
fun KanobiTheme(palette: Palette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = with(palette) {
                darkColorScheme(
                    primary = accent, onPrimary = onAccent, primaryContainer = accent, onPrimaryContainer = onAccent,
                    secondary = muted, onSecondary = onAccent, tertiary = warn, onTertiary = onAccent,
                    background = background, onBackground = text, surface = panel, onSurface = text,
                    surfaceVariant = panelHigh, onSurfaceVariant = muted,
                    surfaceContainerLowest = background, surfaceContainerLow = panel, surfaceContainer = panel,
                    surfaceContainerHigh = panelHigh, surfaceContainerHighest = panelHigh,
                    inverseSurface = text, inverseOnSurface = onAccent, outline = line, outlineVariant = line,
                    error = warn, onError = onAccent,
                )
            },
            typography = AppTypography,
            content = content,
        )
    }
}
