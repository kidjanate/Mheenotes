package com.mheenotes.ui.editor

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.PAPER_WHITE
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PdfRef
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.newId
import com.mheenotes.ui.AndroidContext
import com.mheenotes.ui.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

actual fun copyAsset(fromNotebook: String, toNotebook: String, assetId: String) {
    val src = File(AppGraph.files.assetsDir(fromNotebook)).listFiles()?.firstOrNull { it.name.startsWith(assetId) } ?: return
    val dst = File(AppGraph.files.assetsDir(toNotebook), src.name)
    if (!dst.exists()) { dst.parentFile?.mkdirs(); src.copyTo(dst) }
}

@Composable
actual fun rememberPdfTools(tab: EditorTab, doc: NotebookDocument): PdfTools {
    val scope = rememberCoroutineScope()
    var after = 0
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { insertPdf(tab, doc, uri, after) }
    }
    return remember(tab, doc) {
        object : PdfTools {
            override fun insertPages(afterIndex: Int) { after = afterIndex; picker.launch(arrayOf("application/pdf")) }

            override suspend fun search(query: String): List<PdfHit> = searchPdfText(tab, doc, query)

            override suspend fun boxes(page: Page, hits: List<PdfHit>): List<List<Rect>> =
                withContext(Dispatchers.IO) { hitBoxes(tab, page, hits) }

            override fun endSearch() = PdfText.release()
        }
    }
}

/** Copies a PDF into the notebook and adds its pages after [afterIndex], as one undo step. */
private suspend fun insertPdf(tab: EditorTab, doc: NotebookDocument, uri: Uri, afterIndex: Int) {
    val assetId = newId()
    val sizes = withContext(Dispatchers.IO) {
        val target = File(AppGraph.files.asset(tab.notebookId, "$assetId.pdf"))
        target.parentFile?.mkdirs()
        AndroidContext.app.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        runCatching { PdfPages.sizes(target) }.getOrElse { target.delete(); emptyList() }
    }
    if (sizes.isEmpty()) return
    tab.canvas?.commitSelection()
    val pages = doc.pages.value
    val start = pages.getOrNull(afterIndex)?.ord ?: (pages.first().ord - 1.0)
    val end = pages.getOrNull(afterIndex + 1)?.ord ?: (start + sizes.size + 1.0)
    val step = (end - start) / (sizes.size + 1)
    val added = sizes.mapIndexed { i, (w, h) -> Page(newId(), start + step * (i + 1), w, h, Template.Blank, PAPER_WHITE, PdfRef(assetId, i)) }
    doc.editPages(pages + added)
    tab.scrollRequest = ScrollRequest.ToPage(afterIndex + 1)
}

private fun pdfFile(tab: EditorTab, assetId: String) = File(AppGraph.files.asset(tab.notebookId, "$assetId.pdf"))

/**
 * Finds [query] in the text layer of imported PDF pages, on any Android version. Page text is
 * extracted once per PDF with PdfBox and cached next to the PDF, so later searches are instant.
 */
private suspend fun searchPdfText(tab: EditorTab, doc: NotebookDocument, query: String): List<PdfHit> {
    val pages = doc.pages.value
    return withContext(Dispatchers.IO) {
        val needle = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        val out = ArrayList<PdfHit>()
        val byAsset = pages.withIndex().filter { it.value.pdf != null }.groupBy { it.value.pdf!!.assetId }
        for ((assetId, entries) in byAsset) {
            val texts = PdfText.pages(pdfFile(tab, assetId)) ?: continue
            for ((index, page) in entries) {
                val text = texts.getOrNull(page.pdf!!.pageIndex) ?: continue
                needle.findAll(text).forEach { out += PdfHit(index, page.id, it.range.first, it.range.last + 1) }
            }
        }
        out.sortedWith(compareBy({ it.pageIndex }, { it.start }))
    }
}

/** Maps each hit's characters to page-unit rectangles, one per line it spans. */
private fun hitBoxes(tab: EditorTab, page: Page, hits: List<PdfHit>): List<List<Rect>> {
    val ref = page.pdf ?: return hits.map { emptyList() }
    val pt = PdfText.page(pdfFile(tab, ref.assetId), ref.pageIndex) ?: return hits.map { emptyList() }
    val r = ((page.rotation % 360) + 360) % 360
    val k = page.width / (if (r % 180 == 0) pt.width else pt.height)
    // Same placement as PdfPages.render: scale to the page, then rotate clockwise.
    fun toPage(b: Rect): Rect {
        val l = b.left * k; val t = b.top * k; val rr = b.right * k; val bb = b.bottom * k
        return when (r) {
            90 -> Rect(page.width - bb, l, page.width - t, rr)
            180 -> Rect(page.width - rr, page.height - bb, page.width - l, page.height - t)
            270 -> Rect(t, page.height - rr, bb, page.height - l)
            else -> Rect(l, t, rr, bb)
        }
    }
    return hits.map { h ->
        val lines = ArrayList<Rect>()
        for (i in h.start until minOf(h.end, pt.text.length)) {
            val l = pt.boxes[i * 4]
            if (l.isNaN()) continue
            val box = Rect(l, pt.boxes[i * 4 + 1], pt.boxes[i * 4 + 2], pt.boxes[i * 4 + 3])
            val cur = lines.lastOrNull()
            if (cur != null && kotlin.math.abs(box.centerY - cur.centerY) < cur.height / 2 && box.left >= cur.left - 1) lines[lines.lastIndex] = cur.union(box)
            else lines += box
        }
        lines.map(::toPage)
    }
}

/** One PDF page's text with a box per character (left, top, right, bottom; NaN for separators), in display points. */
class PdfPageText(val text: String, val boxes: FloatArray, val width: Float, val height: Float)

/** Extracted PDF text: all pages' strings cached in memory and in a sidecar file; boxes on demand. */
object PdfText {
    private val memory = HashMap<String, List<String>>()
    private val pageCache = object : LinkedHashMap<String, PdfPageText>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PdfPageText>) = size > 24
    }
    private var open: Pair<String, com.tom_roush.pdfbox.pdmodel.PDDocument>? = null
    private const val PAGE_BREAK = '\u000C'

    fun pages(pdf: File): List<String>? = synchronized(this) {
        if (!pdf.exists()) return null
        memory[pdf.path]?.let { return it }
        val cache = File(pdf.path + ".text")
        val texts = if (cache.exists() && cache.lastModified() >= pdf.lastModified()) {
            cache.readText().split(PAGE_BREAK)
        } else {
            runCatching {
                val d = document(pdf)
                val c = Collector(boxes = false)
                List(d.numberOfPages) { i -> runCatching { c.page(d, i).text.replace(PAGE_BREAK, ' ') }.getOrDefault("") }
            }.onFailure { it.printStackTrace() }.getOrNull()
                ?.also { list -> runCatching { cache.writeText(list.joinToString(PAGE_BREAK.toString())) } }
        } ?: return null
        memory[pdf.path] = texts
        texts
    }

    /** Text and character boxes of page [index]; same text (and offsets) as [pages]. */
    fun page(pdf: File, index: Int): PdfPageText? = synchronized(this) {
        val key = "${pdf.path}#$index"
        pageCache[key]?.let { return it }
        runCatching {
            val d = document(pdf)
            if (index !in 0 until d.numberOfPages) return null
            Collector(boxes = true).page(d, index)
        }.onFailure { it.printStackTrace() }.getOrNull()?.also { pageCache[key] = it }
    }

    fun release() = synchronized(this) {
        runCatching { open?.second?.close() }
        open = null
        pageCache.clear()
    }

    private fun document(pdf: File): com.tom_roush.pdfbox.pdmodel.PDDocument {
        open?.let { (path, d) -> if (path == pdf.path) return d }
        release()
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(AndroidContext.app)
        val d = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdf, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
        open = pdf.path to d
        return d
    }

    /** Builds page text from glyph positions, so text offsets and boxes always line up. */
    private class Collector(private val boxes: Boolean) : com.tom_roush.pdfbox.text.PDFTextStripper() {
        private val text = StringBuilder()
        private var box = FloatArray(0)
        private var n = 0

        fun page(d: com.tom_roush.pdfbox.pdmodel.PDDocument, index: Int): PdfPageText {
            text.setLength(0); n = 0
            startPage = index + 1
            endPage = index + 1
            writeText(d, java.io.StringWriter())
            val p = d.getPage(index)
            val crop = p.cropBox
            val turned = ((p.rotation % 360) + 360) % 180 == 90
            return PdfPageText(text.toString(), box.copyOf(n), if (turned) crop.height else crop.width, if (turned) crop.width else crop.height)
        }

        private fun add(ch: Char, l: Float, t: Float, r: Float, b: Float) {
            text.append(ch)
            if (!boxes) return
            if (n + 4 > box.size) box = box.copyOf(maxOf(256, box.size * 2))
            box[n++] = l; box[n++] = t; box[n++] = r; box[n++] = b
        }

        override fun writeString(text: String?, textPositions: MutableList<com.tom_roush.pdfbox.text.TextPosition>?) {
            for (p in textPositions ?: return) {
                val u = p.unicode ?: continue
                // Baseline-relative glyph height is tight; pad up for ascenders and down for descenders.
                val h = p.heightDir
                for (ch in u) add(ch, p.xDirAdj, p.yDirAdj - h * 1.25f, p.xDirAdj + p.widthDirAdj, p.yDirAdj + h * 0.3f)
            }
        }

        override fun writeWordSeparator() = add(' ', Float.NaN, 0f, 0f, 0f)

        override fun writeLineSeparator() = add(' ', Float.NaN, 0f, 0f, 0f)
    }
}

actual suspend fun loadImageAsset(notebookId: String, assetId: String, maxPixels: Int): androidx.compose.ui.graphics.ImageBitmap? =
    withContext(Dispatchers.IO) { ImageAssets.get(notebookId, assetId, maxPixels) }
