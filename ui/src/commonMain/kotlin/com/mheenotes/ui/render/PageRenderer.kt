package com.mheenotes.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.mheenotes.core.geometry.StrokeGeometry
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.TextItem
import com.mheenotes.core.model.Tool
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Template spacing in page units (points). */
const val LINE_SPACING = 26f
const val GRID_SPACING = 20f

fun templateLineColor(paperArgb: Int): Color {
    val dark = Color(paperArgb).luminance() < 0.4f
    return if (dark) Color(0x2EFFFFFF) else Color(0x2E3A5A8C)
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Draws page ruling in page units. Call inside a scope already transformed to page space. */
fun DrawScope.drawTemplate(t: Template, width: Float, height: Float, paperArgb: Int, pxPerUnit: Float = 1f) {
    if (t == Template.Blank) return
    val color = templateLineColor(paperArgb)
    // Keep lines at least one device pixel wide at any zoom.
    val hair = max(0.6f, 1f / pxPerUnit)
    when (t) {
        Template.Lined -> {
            var y = LINE_SPACING * 3
            while (y < height - LINE_SPACING / 2) {
                drawLine(color, Offset(0f, y), Offset(width, y), hair)
                y += LINE_SPACING
            }
            val margin = Color(0x40E5484D)
            drawLine(margin, Offset(56f, 0f), Offset(56f, height), hair)
        }
        Template.Grid -> {
            var x = GRID_SPACING
            while (x < width) { drawLine(color, Offset(x, 0f), Offset(x, height), hair); x += GRID_SPACING }
            var y = GRID_SPACING
            while (y < height) { drawLine(color, Offset(0f, y), Offset(width, y), hair); y += GRID_SPACING }
        }
        Template.Dotted -> {
            val dot = color.copy(alpha = color.alpha * 2.2f)
            val r = max(0.9f, 1.2f / pxPerUnit)
            var y = GRID_SPACING
            while (y < height) {
                var x = GRID_SPACING
                while (x < width) { drawCircle(dot, r, Offset(x, y)); x += GRID_SPACING }
                y += GRID_SPACING
            }
        }
        Template.Blank -> Unit
    }
}

/** Provides decoded images to the renderer; platforms cache and downsample. */
fun interface ImageSource {
    fun image(assetId: String, maxPixels: Int): ImageBitmap?
}

/**
 * Draws page items. The scope must already map page units to pixels with [pxPerUnit].
 * Pure drawing, safe on any thread given a thread-confined [text] measurer.
 */
class ItemPainter(
    private val images: ImageSource?,
    private val text: TextMeasurer?,
    private val fonts: FontFamily? = null,
    private val boldFonts: FontFamily? = null,
) {
    fun DrawScope.draw(items: List<Item>, pxPerUnit: Float) {
        // Below ~0.5px per sample, extra points are invisible; skipping them keeps overviews fast.
        val minDist = 0.6f / pxPerUnit
        for (item in items) when (item) {
            is Stroke -> drawInkStroke(item, minDist)
            is ShapeItem -> drawShape(item)
            is TextItem -> drawTextItem(item)
            is ImageItem -> drawImageItem(item, pxPerUnit)
        }
    }

    private fun DrawScope.drawImageItem(item: ImageItem, pxPerUnit: Float) {
        val maxPx = (max(item.frame.width, item.frame.height) * pxPerUnit).toInt().coerceIn(64, 4096)
        val bmp = images?.image(item.assetId, maxPx)
        rotate(item.rotation, Offset(item.frame.centerX, item.frame.centerY)) {
            if (bmp == null) {
                drawRect(Color(0x14000000), Offset(item.frame.left, item.frame.top), Size(item.frame.width, item.frame.height))
                return@rotate
            }
            val c = item.crop
            val src = IntOffset((c.left * bmp.width).toInt(), (c.top * bmp.height).toInt())
            val srcSize = IntSize(((c.width) * bmp.width).toInt().coerceAtLeast(1), (c.height * bmp.height).toInt().coerceAtLeast(1))
            // Map the crop window onto the frame exactly; IntOffset destinations would round in page units.
            withTransform({
                translate(item.frame.left, item.frame.top)
                scale(item.frame.width / srcSize.width, item.frame.height / srcSize.height, Offset.Zero)
            }) {
                drawImage(bmp, src, srcSize, IntOffset.Zero, srcSize, filterQuality = FilterQuality.Medium)
            }
        }
    }

    private fun DrawScope.drawTextItem(item: TextItem) {
        val tm = text ?: return
        rotate(item.rotation, Offset(item.frame.centerX, item.frame.centerY)) {
            val layout = tm.measure(
                item.text,
                TextStyle(
                    color = Color(item.argb), fontSize = item.fontSize.sp,
                    fontFamily = if (item.bold) boldFonts ?: fonts else fonts,
                    fontWeight = if (item.bold) FontWeight.Bold else FontWeight.Normal,
                ),
                constraints = Constraints(maxWidth = max(1, item.frame.width.toInt())),
            )
            drawText(layout, topLeft = Offset(item.frame.left, item.frame.top))
        }
    }
}

/** Ink is drawn as round-capped segments; joints overlap seamlessly because ink is opaque. */
fun DrawScope.drawInkStroke(s: Stroke, minDist: Float = 0f) {
    val color = Color(s.argb)
    val pts = if (minDist > 0f) StrokeGeometry.decimate(s.points, minDist) else s.points
    val n = pts.size / 3
    if (n == 0) return
    if (s.tool == Tool.Highlighter) {
        if (n == 1) { drawCircle(color, s.width / 2, Offset(pts[0], pts[1])); return }
        val path = Path()
        path.moveTo(pts[0], pts[1])
        var i = 3
        while (i < pts.size) { path.lineTo(pts[i], pts[i + 1]); i += 3 }
        drawPath(path, color, style = DrawStroke(s.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        return
    }
    if (n == 1) {
        drawCircle(color, StrokeGeometry.radius(s.tool, s.width, pts[2]), Offset(pts[0], pts[1]))
        return
    }
    var i = 0
    while (i < pts.size - 3) {
        val r = StrokeGeometry.radius(s.tool, s.width, (pts[i + 2] + pts[i + 5]) * 0.5f)
        drawLine(color, Offset(pts[i], pts[i + 1]), Offset(pts[i + 3], pts[i + 4]), r * 2, StrokeCap.Round)
        i += 3
    }
}

/** Draws only the segments from sample [from] onwards; used for incremental wet ink. */
fun DrawScope.drawInkSegments(tool: Tool, argb: Int, width: Float, pts: FloatArray, from: Int, count: Int) {
    val color = Color(argb)
    if (count == 1 && from == 0) {
        drawCircle(color, StrokeGeometry.radius(tool, width, pts[2]), Offset(pts[0], pts[1]))
        return
    }
    var k = max(1, from)
    while (k < count) {
        val a = (k - 1) * 3
        val b = k * 3
        val r = StrokeGeometry.radius(tool, width, (pts[a + 2] + pts[b + 2]) * 0.5f)
        drawLine(color, Offset(pts[a], pts[a + 1]), Offset(pts[b], pts[b + 1]), r * 2, StrokeCap.Round)
        k++
    }
}

fun DrawScope.drawShape(s: ShapeItem) {
    val color = Color(s.argb)
    val f = s.frame
    when (s.kind) {
        ShapeKind.Line -> drawLine(color, Offset(f.left, f.top), Offset(f.right, f.bottom), s.strokeWidth, StrokeCap.Round)
        ShapeKind.Arrow -> {
            drawLine(color, Offset(f.left, f.top), Offset(f.right, f.bottom), s.strokeWidth, StrokeCap.Round)
            val ang = atan2(f.bottom - f.top, f.right - f.left)
            val head = max(10f, s.strokeWidth * 4.5f)
            for (side in listOf(-1, 1)) {
                val a = ang + kotlin.math.PI.toFloat() + side * 0.47f
                drawLine(color, Offset(f.right, f.bottom), Offset(f.right + cos(a) * head, f.bottom + sin(a) * head), s.strokeWidth, StrokeCap.Round)
            }
        }
        else -> {
            val n = f.normalized()
            rotate(s.rotation, Offset(n.centerX, n.centerY)) {
                val path = shapePath(s.kind, n.left, n.top, n.right, n.bottom)
                s.fillArgb?.let { drawPath(path, Color(it), style = Fill) }
                drawPath(path, color, style = DrawStroke(s.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

fun shapePath(kind: ShapeKind, l: Float, t: Float, r: Float, b: Float): Path = Path().apply {
    when (kind) {
        ShapeKind.Rectangle -> addRect(androidx.compose.ui.geometry.Rect(l, t, r, b))
        ShapeKind.Ellipse -> addOval(androidx.compose.ui.geometry.Rect(l, t, r, b))
        ShapeKind.Triangle -> { moveTo((l + r) / 2, t); lineTo(r, b); lineTo(l, b); close() }
        else -> { moveTo(l, t); lineTo(r, b) }
    }
}
