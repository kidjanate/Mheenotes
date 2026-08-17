package com.mheenotes.app.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Build
import android.provider.OpenableColumns
import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageState
import com.mheenotes.core.model.ObjectCrop
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.TextAlignment
import com.mheenotes.ink.engine.PressureNormalizer
import java.io.File
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

data class ImportedPdf(
    val sourceId: String,
    val localFile: File,
    val pageSizes: List<Pair<Int, Int>>,
    val displayName: String = "Imported PDF",
)

data class PdfSearchHit(val pageIndex: Int, val matchCount: Int)
data class PdfOutlineLink(val sourcePageIndex: Int, val targetPageIndex: Int)

/** All PDF I/O is serialized off the UI thread because PdfRenderer opens one page at a time. */
class PdfServices(
    private val contentResolver: ContentResolver,
    private val root: File,
) : AutoCloseable {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "mheenotes-pdf").apply { isDaemon = true }
    }

    fun importPdf(uri: Uri, result: (Result<ImportedPdf>) -> Unit) {
        worker.execute {
            result(runCatching {
                val id = UUID.randomUUID().toString()
                val file = root.resolve("sources/$id.pdf")
                file.parentFile?.mkdirs()
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Unable to open PDF" }
                    file.outputStream().buffered().use(input::copyTo)
                }
                val sizes = openRenderer(file).use { renderer ->
                    (0 until renderer.pageCount).map { index ->
                        renderer.openPage(index).use { it.width to it.height }
                    }
                }
                ImportedPdf(id, file, sizes, queryDisplayName(uri))
            })
        }
    }

    private fun queryDisplayName(uri: Uri): String = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Imported PDF"

    fun renderPage(file: File, pageIndex: Int, targetWidth: Int, result: (Result<Bitmap>) -> Unit) {
        worker.execute {
            result(runCatching {
                openRenderer(file).use { renderer ->
                    renderer.openPage(pageIndex).use { page ->
                        val width = targetWidth.coerceIn(128, 4096)
                        val height = max(1, (width * page.height.toFloat() / page.width).roundToInt())
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            })
        }
    }

    fun search(file: File, query: String, result: (Result<List<PdfSearchHit>>) -> Unit) {
        worker.execute {
            result(runCatching {
                if (Build.VERSION.SDK_INT < 35 || query.isBlank()) return@runCatching emptyList()
                openRenderer(file).use { renderer ->
                    (0 until renderer.pageCount).mapNotNull { index ->
                        renderer.openPage(index).use { page ->
                            page.searchText(query).size.takeIf { it > 0 }?.let { PdfSearchHit(index, it) }
                        }
                    }
                }
            })
        }
    }

    fun outline(file: File, result: (Result<List<PdfOutlineLink>>) -> Unit) {
        worker.execute {
            result(runCatching {
                if (Build.VERSION.SDK_INT < 35) return@runCatching emptyList()
                openRenderer(file).use { renderer ->
                    (0 until renderer.pageCount).flatMap { index ->
                        renderer.openPage(index).use { page ->
                            page.gotoLinks.map { PdfOutlineLink(index, it.destination.pageNumber) }
                        }
                    }
                }
            })
        }
    }

    private fun openRenderer(file: File): PdfRenderer = PdfRenderer(
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY),
    )

    override fun close() { worker.shutdown() }
}

object PageExporter {
    sealed interface ExportObject {
        val zIndex: Long
    }

    data class ExportImage(
        val bitmap: Bitmap,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val rotationDegrees: Float,
        val opacity: Float,
        val crop: ObjectCrop,
        override val zIndex: Long,
    ) : ExportObject

    data class ExportText(
        val text: String,
        val x: Float,
        val y: Float,
        val textSize: Float,
        val argb: Int,
        val rotationDegrees: Float,
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean,
        val alignment: TextAlignment,
        val fontFamily: String,
        val outlineArgb: Int?,
        val outlineWidth: Float,
        val curveDegrees: Float,
        override val zIndex: Long,
    ) : ExportObject

    data class ExportShape(
        val kind: ShapeKind,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val rotationDegrees: Float,
        val strokeArgb: Int,
        val strokeWidth: Float,
        val fillArgb: Int?,
        override val zIndex: Long,
    ) : ExportObject

    data class ExportPage(
        val page: PageState,
        val background: Bitmap? = null,
        val widthMicrons: Float = 210_000f,
        val heightMicrons: Float = 297_000f,
        val objects: List<ExportObject> = emptyList(),
    )

    fun writePdf(pages: List<ExportPage>, output: OutputStream) {
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, exportPage ->
                val width = 1240
                val height = (width * exportPage.heightMicrons / exportPage.widthMicrons).roundToInt()
                    .coerceAtLeast(1)
                val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                exportPage.background?.let { page.canvas.drawBitmap(it, null, Rect(0, 0, width, height), null) }
                val scale = width / exportPage.widthMicrons
                drawStrokes(exportPage.page.strokes, page.canvas, scale)
                drawObjects(exportPage.objects, page.canvas, scale)
                document.finishPage(page)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    fun renderImage(page: ExportPage, width: Int = 1600): Bitmap {
        val height = (width * page.heightMicrons / page.widthMicrons).roundToInt().coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.background?.let { canvas.drawBitmap(it, null, Rect(0, 0, width, height), null) }
            val scale = width / page.widthMicrons
            drawStrokes(page.page.strokes, canvas, scale)
            drawObjects(page.objects, canvas, scale)
        }
    }

    private fun drawObjects(objects: List<ExportObject>, canvas: android.graphics.Canvas, scale: Float) {
        objects.sortedBy(ExportObject::zIndex).forEach { item ->
            when (item) {
                is ExportImage -> withRotation(
                    canvas, item.rotationDegrees,
                    (item.x + item.width / 2f) * scale,
                    (item.y + item.height / 2f) * scale,
                ) {
                    canvas.drawBitmap(
                        item.bitmap,
                        Rect(
                            (item.bitmap.width * item.crop.leftFraction).roundToInt(),
                            (item.bitmap.height * item.crop.topFraction).roundToInt(),
                            (item.bitmap.width * (1f - item.crop.rightFraction)).roundToInt(),
                            (item.bitmap.height * (1f - item.crop.bottomFraction)).roundToInt(),
                        ),
                        RectF(
                            item.x * scale,
                            item.y * scale,
                            (item.x + item.width) * scale,
                            (item.y + item.height) * scale,
                        ),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (item.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
                        },
                    )
                }
                is ExportText -> withRotation(canvas, item.rotationDegrees, item.x * scale, item.y * scale) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = item.argb
                        textSize = item.textSize * scale
                        isUnderlineText = item.underline
                        typeface = android.graphics.Typeface.create(
                            if (item.fontFamily.startsWith("file:")) null else item.fontFamily,
                            when {
                                item.bold && item.italic -> android.graphics.Typeface.BOLD_ITALIC
                                item.bold -> android.graphics.Typeface.BOLD
                                item.italic -> android.graphics.Typeface.ITALIC
                                else -> android.graphics.Typeface.NORMAL
                            },
                        )
                        textAlign = when (item.alignment) {
                            TextAlignment.Start -> Paint.Align.LEFT
                            TextAlignment.Center -> Paint.Align.CENTER
                            TextAlignment.End -> Paint.Align.RIGHT
                        }
                    }
                    if (item.fontFamily.startsWith("file:")) runCatching {
                        paint.typeface = android.graphics.Typeface.createFromFile(item.fontFamily.removePrefix("file:"))
                    }
                    item.text.lines().forEachIndexed { lineIndex, line ->
                        val baseline = (item.y + lineIndex * item.textSize * 1.2f) * scale
                        val path = curvedTextPath(item, baseline, scale)
                        item.outlineArgb?.takeIf { item.outlineWidth > 0f }?.let { outlineColor ->
                            val fillColor = paint.color
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = item.outlineWidth * scale
                            paint.color = outlineColor
                            if (path == null) canvas.drawText(line, item.x * scale, baseline, paint)
                            else canvas.drawTextOnPath(line, path, 0f, 0f, paint)
                            paint.style = Paint.Style.FILL
                            paint.color = fillColor
                        }
                        if (path == null) canvas.drawText(line, item.x * scale, baseline, paint)
                        else canvas.drawTextOnPath(line, path, 0f, 0f, paint)
                    }
                }
                is ExportShape -> withRotation(
                    canvas, item.rotationDegrees,
                    (item.x + item.width / 2f) * scale,
                    (item.y + item.height / 2f) * scale,
                ) {
                    drawShape(item, canvas, scale)
                }
            }
        }
    }

    private fun curvedTextPath(item: ExportText, baseline: Float, scale: Float): android.graphics.Path? {
        if (kotlin.math.abs(item.curveDegrees) < 1f) return null
        val width = 90_000f * scale
        val bend = width * (item.curveDegrees / 180f) * .45f
        return android.graphics.Path().apply {
            moveTo(item.x * scale, baseline)
            quadTo(item.x * scale + width / 2f, baseline - bend, item.x * scale + width, baseline)
        }
    }

    private fun drawShape(item: ExportShape, canvas: android.graphics.Canvas, scale: Float) {
        val bounds = RectF(
            item.x * scale,
            item.y * scale,
            (item.x + item.width) * scale,
            (item.y + item.height) * scale,
        )
        item.fillArgb?.let { fill ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill; style = Paint.Style.FILL }
            when (item.kind) {
                ShapeKind.Rectangle -> canvas.drawRect(bounds, paint)
                ShapeKind.RoundedRectangle -> canvas.drawRoundRect(bounds, 18f, 18f, paint)
                ShapeKind.Ellipse -> canvas.drawOval(bounds, paint)
                ShapeKind.Triangle -> canvas.drawPath(trianglePath(bounds), paint)
                ShapeKind.Line, ShapeKind.Arrow -> Unit
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.strokeArgb
            style = Paint.Style.STROKE
            strokeWidth = (item.strokeWidth * scale).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        when (item.kind) {
            ShapeKind.Rectangle -> canvas.drawRect(bounds, paint)
            ShapeKind.RoundedRectangle -> canvas.drawRoundRect(bounds, 18f, 18f, paint)
            ShapeKind.Ellipse -> canvas.drawOval(bounds, paint)
            ShapeKind.Triangle -> canvas.drawPath(trianglePath(bounds), paint)
            ShapeKind.Line -> canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, paint)
            ShapeKind.Arrow -> {
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, paint)
                val angle = kotlin.math.atan2(bounds.bottom - bounds.top, bounds.right - bounds.left)
                val head = 12f.coerceAtLeast(paint.strokeWidth * 4f)
                canvas.drawLine(bounds.right, bounds.bottom, bounds.right - head * kotlin.math.cos(angle - .55f), bounds.bottom - head * kotlin.math.sin(angle - .55f), paint)
                canvas.drawLine(bounds.right, bounds.bottom, bounds.right - head * kotlin.math.cos(angle + .55f), bounds.bottom - head * kotlin.math.sin(angle + .55f), paint)
            }
        }
    }

    private fun trianglePath(bounds: RectF) = android.graphics.Path().apply {
        moveTo(bounds.centerX(), bounds.top)
        lineTo(bounds.right, bounds.bottom)
        lineTo(bounds.left, bounds.bottom)
        close()
    }

    private inline fun withRotation(
        canvas: android.graphics.Canvas,
        degrees: Float,
        pivotX: Float,
        pivotY: Float,
        block: () -> Unit,
    ) {
        val checkpoint = canvas.save()
        if (degrees != 0f) canvas.rotate(degrees, pivotX, pivotY)
        try {
            block()
        } finally {
            canvas.restoreToCount(checkpoint)
        }
    }

    private fun drawStrokes(strokes: List<InkStroke>, canvas: android.graphics.Canvas, scale: Float) {
        strokes.sortedBy(InkStroke::zIndex).forEach { stroke ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.style.argb
                alpha = (stroke.style.opacity * 255).roundToInt()
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            if (stroke.samples.size == 1) {
                val sample = stroke.samples.single()
                paint.style = Paint.Style.FILL
                val radius = PressureNormalizer.width(sample.pressure, stroke.style) * scale / 2f
                canvas.drawCircle(sample.position.x * scale, sample.position.y * scale, radius, paint)
            } else {
                stroke.samples.zipWithNext().forEach { (start, end) ->
                    paint.strokeWidth = PressureNormalizer.width(end.pressure, stroke.style) * scale
                    canvas.drawLine(
                        start.position.x * scale,
                        start.position.y * scale,
                        end.position.x * scale,
                        end.position.y * scale,
                        paint,
                    )
                }
            }
        }
    }
}
