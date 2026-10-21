package com.mheenotes.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** Hue, saturation, value in [0,360) and [0,1]. */
fun argbToHsv(argb: Int): Triple<Float, Float, Float> {
    val r = (argb shr 16 and 0xff) / 255f
    val g = (argb shr 8 and 0xff) / 255f
    val b = (argb and 0xff) / 255f
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }.let { if (it < 0) it + 360f else it }
    return Triple(h, if (mx == 0f) 0f else d / mx, mx)
}

private val HueStops = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 1f, 1f) }

/** Saturation/value square plus hue strip. Reports opaque colors; callers apply their own alpha. */
@Composable
fun ColorPicker(argb: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val start = remember { argbToHsv(argb) }
    var h by remember { mutableFloatStateOf(start.first) }
    var s by remember { mutableFloatStateOf(start.second) }
    var v by remember { mutableFloatStateOf(start.third) }
    fun emit() = onChange(Color.hsv(h, s, v).toArgb())
    val c = M.colors
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(Radii.sm))
                .pointerInput(Unit) {
                    fun set(p: Offset) {
                        s = (p.x / size.width).coerceIn(0f, 1f); v = 1f - (p.y / size.height).coerceIn(0f, 1f); emit()
                    }
                    detectTapGestures { set(it) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { ch, _ ->
                        ch.consume()
                        s = (ch.position.x / size.width).coerceIn(0f, 1f); v = 1f - (ch.position.y / size.height).coerceIn(0f, 1f); emit()
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(h, 1f, 1f))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val p = Offset(s * size.width, (1f - v) * size.height)
                drawCircle(Color.White, 9.dp.toPx(), p, style = Stroke(2.5.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.25f), 10.5.dp.toPx(), p, style = Stroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(HueStops))
                    .pointerInput(Unit) { detectTapGestures { h = (it.x / size.width * 360f).coerceIn(0f, 359.9f); emit() } }
                    .pointerInput(Unit) {
                        detectDragGestures { ch, _ -> ch.consume(); h = (ch.position.x / size.width * 360f).coerceIn(0f, 359.9f); emit() }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val x = h / 360f * size.width
                    drawCircle(Color.White, size.height / 2 - 1.dp.toPx(), Offset(x.coerceIn(size.height / 2, size.width - size.height / 2), size.height / 2), style = Stroke(3.dp.toPx()))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.hsv(h, s, v)).border(0.75.dp, c.separator, CircleShape))
        }
    }
}
