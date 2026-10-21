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

            override suspend fun search(query: String): List<Pair<Int, Int>>? = searchPdfText(tab, doc, query)
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
 * Searches the text layer of imported PDF pages. Needs Android 15 (API 35) text extraction;
 * returns null on older systems so the UI can explain why.
 */
private suspend fun searchPdfText(tab: EditorTab, doc: NotebookDocument, query: String): List<Pair<Int, Int>>? {
    if (Build.VERSION.SDK_INT < 35) return null
    val pages = doc.pages.value
    return withContext(Dispatchers.IO) {
        val out = ArrayList<Pair<Int, Int>>()
        val byAsset = pages.withIndex().filter { it.value.pdf != null }.groupBy { it.value.pdf!!.assetId }
        for ((assetId, entries) in byAsset) {
            val file = File(AppGraph.files.asset(tab.notebookId, "$assetId.pdf"))
            if (!file.exists()) continue
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { r ->
                    for ((index, page) in entries) {
                        val pi = page.pdf!!.pageIndex
                        if (pi !in 0 until r.pageCount) continue
                        val text = r.openPage(pi).use { p -> p.textContents.joinToString(" ") { it.text } }
                        val n = Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).count()
                        if (n > 0) out += index to n
                    }
                }
            }
        }
        out.sortedBy { it.first }
    }
}

actual suspend fun loadImageAsset(notebookId: String, assetId: String, maxPixels: Int): androidx.compose.ui.graphics.ImageBitmap? =
    withContext(Dispatchers.IO) { ImageAssets.get(notebookId, assetId, maxPixels) }
