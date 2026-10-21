package com.mheenotes.ui.editor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.model.Id
import com.mheenotes.ui.AndroidContext
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.render.drawTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual object Exporter {
    actual suspend fun exportAndShare(tab: EditorTab, kind: ExportKind): Boolean {
        val doc = tab.doc ?: return false
        val title = AppGraph.library.notebook(tab.notebookId)?.title?.ifBlank { null } ?: "Notebook"
        val file = when (kind) {
            ExportKind.Pdf -> exportPdf(tab.notebookId, doc, title)
            ExportKind.PagePng -> exportPng(tab.notebookId, doc, tab.currentPage, title)
            ExportKind.Notebook -> NotebookArchive.write(tab.notebookId, title, doc)
        }
        share(AndroidContext.app, file, mime(kind))
        return true
    }

    private fun mime(k: ExportKind) = when (k) {
        ExportKind.Pdf -> "application/pdf"
        ExportKind.PagePng -> "image/png"
        ExportKind.Notebook -> "application/octet-stream"
    }

    fun exportDir(): File = File(AndroidContext.app.cacheDir, "exports").apply { mkdirs() }

    fun safeName(title: String) = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)

    /** Vector PDF: ink and shapes stay vectors; imported PDF pages are embedded as 2x images. */
    suspend fun exportPdf(notebookId: Id, doc: NotebookDocument, title: String): File {
        val pages = doc.pages.value
        val contents = pages.map { doc.load(it.id).snapshot.items }
        val r = Rasterizer.shared ?: error("renderer not ready")
        return withContext(Dispatchers.Default) {
            val pdf = PdfDocument()
            try {
                pages.forEachIndexed { i, page ->
                    val info = PdfDocument.PageInfo.Builder(page.width.toInt(), page.height.toInt(), i + 1).create()
                    val p = pdf.startPage(info)
                    val canvas = p.canvas
                    canvas.drawColor(page.backgroundArgb)
                    page.pdf?.let { ref ->
                        val k = 2f
                        val bmp = Bitmap.createBitmap((page.width * k).toInt(), (page.height * k).toInt(), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(page.backgroundArgb)
                        PdfPages.render(notebookId, ref.assetId, ref.pageIndex, bmp, Matrix().apply { setScale(k, k) }, page.width)
                        canvas.save(); canvas.scale(1 / k, 1 / k); canvas.drawBitmap(bmp, 0f, 0f, null); canvas.restore()
                        bmp.recycle()
                    }
                    val painter = r.painter(notebookId)
                    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(page.width, page.height)) {
                        if (page.pdf == null) drawTemplate(page.template, page.width, page.height, page.backgroundArgb, 2f)
                        with(painter) { draw(contents[i], 4f) }
                    }
                    pdf.finishPage(p)
                }
                val out = File(exportDir(), "${safeName(title)}.pdf")
                FileOutputStream(out).use { pdf.writeTo(it) }
                out
            } finally {
                pdf.close()
            }
        }
    }

    private suspend fun exportPng(notebookId: Id, doc: NotebookDocument, index: Int, title: String): File {
        val page = doc.pages.value.getOrNull(index) ?: doc.pages.value.first()
        val bmp = renderPageBitmap(notebookId, doc, page, (page.width * 3).toInt())
        return withContext(Dispatchers.IO) {
            val out = File(exportDir(), "${safeName(title)} p${index + 1}.png")
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            out
        }
    }

    fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * `.mheenote` files: a zip of the notebook folder (database + assets) plus a title entry.
 */
object NotebookArchive {
    const val EXT = "mheenote"

    suspend fun write(notebookId: Id, title: String, doc: NotebookDocument?): File {
        doc?.flush()
        doc?.store?.checkpoint()
        return withContext(Dispatchers.IO) {
            val dir = File(AppGraph.files.notebookDir(notebookId))
            val out = File(Exporter.exportDir(), "${Exporter.safeName(title)}.$EXT")
            ZipOutputStream(FileOutputStream(out)).use { zip ->
                zip.putNextEntry(ZipEntry("title.txt")); zip.write(title.toByteArray()); zip.closeEntry()
                dir.walkTopDown().filter { it.isFile && !it.name.endsWith("-shm") && !it.name.endsWith("-wal") }.forEach { f ->
                    zip.putNextEntry(ZipEntry(f.relativeTo(dir).path))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            out
        }
    }
}
