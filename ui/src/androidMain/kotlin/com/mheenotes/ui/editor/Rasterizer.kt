package com.mheenotes.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.render.ImageSource
import com.mheenotes.ui.render.ItemPainter
import com.mheenotes.ui.render.drawTemplate
import java.io.File
import kotlin.math.max

/**
 * Draws a region of a page (paper, PDF, template, items) into a bitmap. Thread-safe: every
 * worker thread gets its own text measurer, and PDF pages are rendered under a per-file lock.
 */
class Rasterizer(context: Context, private val fonts: FontFamily?, private val boldFonts: FontFamily?) {
    private val app = context.applicationContext
    private val resolver = createFontFamilyResolver(app)
    private val measurers = ThreadLocal<TextMeasurer>()

    fun painter(notebookId: Id): ItemPainter {
        val tm = measurers.get() ?: TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr, 16).also { measurers.set(it) }
        return ItemPainter(ImageSource { asset, px -> ImageAssets.get(notebookId, asset, px) }, tm, fonts, boldFonts)
    }

    /**
     * Renders page area starting at page-local ([originX], [originY]) at [scale] px per unit.
     * [items] must already be filtered to the area and sorted in draw order.
     */
    fun render(
        bitmap: Bitmap,
        notebookId: Id,
        page: Page,
        items: List<Item>,
        scale: Float,
        originX: Float,
        originY: Float,
    ) {
        bitmap.eraseColor(page.backgroundArgb)
        page.pdf?.let { ref ->
            val m = Matrix().apply { setScale(scale, scale); postTranslate(-originX * scale, -originY * scale) }
            PdfPages.render(notebookId, ref.assetId, ref.pageIndex, bitmap, m, page.width)
        }
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bitmap))
        val painter = painter(notebookId)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
            withTransform({
                scale(scale, scale, Offset.Zero)
                translate(-originX, -originY)
            }) {
                if (page.pdf == null) drawTemplate(page.template, page.width, page.height, page.backgroundArgb, scale)
                with(painter) { draw(items, scale) }
            }
        }
    }

    companion object {
        @Volatile
        var shared: Rasterizer? = null
    }
}

/** Opened PDF assets. PdfRenderer allows one open page at a time, so each file has a lock. */
object PdfPages {
    private class Doc(val pfd: ParcelFileDescriptor, val renderer: PdfRenderer)

    private val docs = HashMap<String, Doc>()

    private fun path(notebookId: Id, assetId: Id): String = AppGraph.files.asset(notebookId, "$assetId.pdf")

    private fun open(path: String): Doc? = synchronized(docs) {
        docs[path] ?: runCatching {
            val pfd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            Doc(pfd, PdfRenderer(pfd))
        }.getOrNull()?.also { docs[path] = it }
    }

    /** Renders PDF page [index] scaled so its width fills [pageWidth] page units. */
    fun render(notebookId: Id, assetId: Id, index: Int, bitmap: Bitmap, matrix: Matrix, pageWidth: Float) {
        val doc = open(path(notebookId, assetId)) ?: return
        synchronized(doc) {
            if (index !in 0 until doc.renderer.pageCount) return
            doc.renderer.openPage(index).use { p ->
                val m = Matrix(matrix).apply { preScale(pageWidth / p.width, pageWidth / p.width) }
                p.render(bitmap, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    /** Page sizes in points. */
    fun sizes(file: File): List<Pair<Float, Float>> {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                return List(r.pageCount) { i -> r.openPage(i).use { it.width.toFloat() to it.height.toFloat() } }
            }
        }
    }

    fun closeAll(notebookId: Id) = synchronized(docs) {
        val prefix = AppGraph.files.assetsDir(notebookId)
        docs.keys.filter { it.startsWith(prefix) }.forEach { k ->
            docs.remove(k)?.let { d -> synchronized(d) { d.renderer.close(); d.pfd.close() } }
        }
    }
}

/** Decoded image assets, downsampled to what the current zoom needs. */
object ImageAssets {
    private val cache = object : LruCache<String, ImageBitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    private fun file(notebookId: Id, assetId: Id): File? =
        File(AppGraph.files.assetsDir(notebookId)).listFiles()?.firstOrNull { it.name.startsWith(assetId) }

    /** Returns a bitmap whose long side is at least [maxPixels] (or the source size). */
    fun get(notebookId: Id, assetId: Id, maxPixels: Int): ImageBitmap? {
        // Bucket sizes in powers of two so zooming does not decode on every level.
        var bucket = 256
        while (bucket < maxPixels && bucket < 4096) bucket *= 2
        val key = "$notebookId/$assetId/$bucket"
        cache.get(key)?.let { return it }
        val f = file(notebookId, assetId) ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, opts)
        var sample = 1
        while (max(opts.outWidth, opts.outHeight) / (sample * 2) >= bucket) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap() ?: return null
        cache.put(key, bmp)
        return bmp
    }

    fun size(notebookId: Id, assetId: Id): Pair<Int, Int>? {
        val f = file(notebookId, assetId) ?: return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        return if (o.outWidth > 0) o.outWidth to o.outHeight else null
    }
}
