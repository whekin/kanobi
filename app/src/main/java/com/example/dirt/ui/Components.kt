package com.example.dirt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dirt.theme.DisplayFont
import com.example.dirt.theme.Lime
import com.example.dirt.theme.Muted
import com.example.dirt.theme.PanelHigh
import com.example.dirt.theme.Paper
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tan

/** One decimal, a typographic minus and no "−0.0". */
fun degrees(value: Float): String =
    String.format(Locale.US, "%.1f", if (abs(value) < 0.05f) 0f else value).replace('-', '−')

fun slope(angle: Float): String = if (abs(angle) >= 85f) "почти вертикально"
    else "${(tan(Math.toRadians(angle.toDouble())) * 100).roundToInt()} %".replace('-', '−')

@Composable
fun PageHeader(title: String, note: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Paper, letterSpacing = (-0.5).sp)
            Text(note, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        if (trailing != null) Text(trailing, fontFamily = DisplayFont, fontSize = 32.sp, color = Lime)
    }
}

@Composable
fun DirtSnackbar(data: SnackbarData) {
    Snackbar(data, Modifier.padding(horizontal = 20.dp), shape = RoundedCornerShape(16.dp),
        containerColor = PanelHigh, contentColor = Paper)
}

enum class Glyph { Logo, Book, Lock, Play, Save, Close }

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
            lines(.06f, .84f, .42f, .18f, .57f, .44f, .71f, .18f, .96f, .84f, close = true)
            lines(.33f, .84f, .58f, .42f)
        }
        Glyph.Book -> {
            lines(.2f, .14f, .8f, .14f, .8f, .86f, .2f, .86f, close = true)
            lines(.37f, .15f, .37f, .85f)
            lines(.52f, .36f, .66f, .36f)
            lines(.52f, .54f, .66f, .54f)
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
