package com.kanobi.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanobi.app.R
import com.kanobi.app.theme.DisplayFont
import com.kanobi.app.theme.KanobiTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** One decimal, a typographic minus and no "−0.0". */
fun degrees(value: Float): String =
    String.format(Locale.US, "%.1f", if (abs(value) < 0.05f) 0f else value).replace('-', '−')

/** Grade in percent, as people read it off road signs; past 85° only "nearly vertical". */
@Composable
fun slopeText(angle: Float): String = if (abs(angle) >= 85f) stringResource(R.string.nearly_vertical)
    else stringResource(R.string.slope_percent, (tan(Math.toRadians(angle.toDouble())) * 100).roundToInt()).replace('-', '−')

@Composable
fun PageHeader(title: String, note: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = KanobiTheme.colors
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = colors.text, letterSpacing = (-0.5).sp)
            Text(note, fontSize = 13.sp, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
        }
        if (trailing != null) Text(trailing, fontFamily = DisplayFont, fontSize = 32.sp, color = colors.accent)
    }
}

/** Round outlined button for the top bar; [content] is a glyph and maybe a count. */
@Composable
fun PillButton(description: String, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val colors = KanobiTheme.colors
    Row(
        Modifier.minimumInteractiveComponentSize().height(40.dp).widthIn(min = 40.dp).clip(CircleShape)
            .background(colors.panel.copy(alpha = 0.7f)).border(1.dp, colors.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 11.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun AppSnackbar(data: SnackbarData) {
    val colors = KanobiTheme.colors
    Snackbar(data, Modifier.padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp),
        containerColor = colors.panelHigh, contentColor = colors.text)
}

enum class Glyph { Logo, Book, Settings, Lock, Play, Save, Close }

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawGlyph(glyph, tint) }
}

/** Line icons drawn in a unit square, so they stay crisp at any size without an icon library. */
fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val stroke = Stroke(max(1.6.dp.toPx(), size.minDimension / 13f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun at(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    fun lines(vararg xy: Float, close: Boolean = false) {
        val path = Path().apply {
            moveTo(size.width * xy[0], size.height * xy[1])
            for (i in 2 until xy.size step 2) lineTo(size.width * xy[i], size.height * xy[i + 1])
            if (close) close()
        }
        drawPath(path, color, style = stroke)
    }
    when (glyph) {
        Glyph.Logo -> {
            // The dial mark: a ring, the horizon and a needle leaving at 30°.
            val r = size.minDimension * 0.44f
            drawCircle(color, r, style = stroke)
            lines(.08f, .5f, .2f, .5f)
            lines(.8f, .5f, .92f, .5f)
            val t = Math.toRadians(30.0).toFloat()
            drawLine(color, center, center + Offset(cos(t), -sin(t)) * r * 0.86f, stroke.width * 1.15f, StrokeCap.Round)
            drawCircle(color, stroke.width * 1.1f, center)
        }
        Glyph.Book -> {
            lines(.2f, .14f, .8f, .14f, .8f, .86f, .2f, .86f, close = true)
            lines(.37f, .15f, .37f, .85f)
            lines(.52f, .36f, .66f, .36f)
            lines(.52f, .54f, .66f, .54f)
        }
        Glyph.Settings -> {
            lines(.14f, .32f, .86f, .32f)
            lines(.14f, .68f, .86f, .68f)
            drawCircle(color, size.minDimension * .1f, at(.36f, .32f))
            drawCircle(color, size.minDimension * .1f, at(.64f, .68f))
        }
        Glyph.Lock -> {
            lines(.22f, .46f, .78f, .46f, .78f, .86f, .22f, .86f, close = true)
            drawArc(color, 180f, 180f, false, at(.33f, .16f), Size(size.width * .34f, size.height * .6f), style = stroke)
            lines(.5f, .62f, .5f, .7f)
        }
        Glyph.Play -> lines(.32f, .2f, .8f, .5f, .32f, .8f, close = true)
        Glyph.Save -> {
            lines(.5f, .14f, .5f, .6f)
            lines(.32f, .44f, .5f, .62f, .68f, .44f)
            lines(.18f, .62f, .18f, .86f, .82f, .86f, .82f, .62f)
        }
        Glyph.Close -> {
            lines(.27f, .27f, .73f, .73f)
            lines(.73f, .27f, .27f, .73f)
        }
    }
}
