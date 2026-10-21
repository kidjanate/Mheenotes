package com.mheenotes.ui.editor

import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.view.SurfaceView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import com.mheenotes.core.model.Tool
import com.mheenotes.ui.render.drawInkSegments

/** New samples of the stroke being written, in page units, plus the page-to-screen mapping. */
class WetSegment(
    val tool: Tool,
    val argb: Int,
    val width: Float,
    /** Starts with the last already-drawn sample so the new piece joins it. */
    val points: FloatArray,
    val first: Boolean,
    val scale: Float,
    val ox: Float,
    val oy: Float,
)

/**
 * Draws the stroke being written straight into the display's front buffer, skipping the usual
 * frame queue. Finished ink is handed to the canvas view; this layer is then cleared.
 */
class FrontInk(val surface: SurfaceView) {
    private val scope = CanvasDrawScope()

    private val renderer = CanvasFrontBufferedRenderer(surface, object : CanvasFrontBufferedRenderer.Callback<WetSegment> {
        override fun onDrawFrontBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int, param: WetSegment) {
            scope.draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(bufferWidth.toFloat(), bufferHeight.toFloat())) {
                withTransform({ translate(param.ox, param.oy); scale(param.scale, param.scale, Offset.Zero) }) {
                    val n = param.points.size / 3
                    drawInkSegments(param.tool, param.argb, param.width, param.points, if (param.first) 0 else 1, n)
                }
            }
        }

        override fun onDrawMultiBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int, params: Collection<WetSegment>) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        }
    })

    init {
        surface.setZOrderOnTop(true)
        surface.holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    fun render(segment: WetSegment) = renderer.renderFrontBufferedLayer(segment)

    /** Removes the wet stroke once the canvas view is showing it. */
    fun clear() = renderer.clear()

    fun release() = renderer.release(true)
}
