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

            override suspend fun search(query: String): List<Pair<Int, Int>> = searchPdfText(tab, doc, query)
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

/**
 * Searches the text layer of imported PDF pages, on any Android version. Page text is extracted
 * once per PDF with PdfBox and cached next to the PDF, so later searches are instant.
 */
private suspend fun searchPdfText(tab: EditorTab, doc: NotebookDocument, query: String): List<Pair<Int, Int>> {
    val pages = doc.pages.value
    return withContext(Dispatchers.IO) {
        val needle = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        val out = ArrayList<Pair<Int, Int>>()
        val byAsset = pages.withIndex().filter { it.value.pdf != null }.groupBy { it.value.pdf!!.assetId }
        for ((assetId, entries) in byAsset) {
            val texts = PdfText.pages(File(AppGraph.files.asset(tab.notebookId, "$assetId.pdf"))) ?: continue
            for ((index, page) in entries) {
                val n = texts.getOrNull(page.pdf!!.pageIndex)?.let { needle.findAll(it).count() } ?: 0
                if (n > 0) out += index to n
            }
        }
        out.sortedBy { it.first }
    }
}

/** Extracted PDF text, one string per page, cached in memory and in a sidecar file. */
object PdfText {
    private val memory = HashMap<String, List<String>>()
    private const val PAGE_BREAK = '\u000C'

    fun pages(pdf: File): List<String>? = synchronized(this) {
        if (!pdf.exists()) return null
        memory[pdf.path]?.let { return it }
        val cache = File(pdf.path + ".txt")
        val texts = if (cache.exists() && cache.lastModified() >= pdf.lastModified()) {
            cache.readText().split(PAGE_BREAK)
        } else {
            extract(pdf)?.also { list -> runCatching { cache.writeText(list.joinToString(PAGE_BREAK.toString())) } }
        } ?: return null
        memory[pdf.path] = texts
        texts
    }

    private fun extract(pdf: File): List<String>? = runCatching {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(AndroidContext.app)
        com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdf, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly()).use { d ->
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            List(d.numberOfPages) { i ->
                stripper.startPage = i + 1
                stripper.endPage = i + 1
                runCatching { stripper.getText(d).replace(PAGE_BREAK, ' ') }.getOrDefault("")
            }
        }
    }.onFailure { it.printStackTrace() }.getOrNull()
}

actual suspend fun loadImageAsset(notebookId: String, assetId: String, maxPixels: Int): androidx.compose.ui.graphics.ImageBitmap? =
    withContext(Dispatchers.IO) { ImageAssets.get(notebookId, assetId, maxPixels) }
