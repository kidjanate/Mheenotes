package com.mheenotes.ui.editor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.ItemDrawOrder
import com.mheenotes.core.geometry.Lasso
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.newId
import com.mheenotes.ui.AndroidContext
import com.mheenotes.ui.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

actual object LassoCapture {
    actual suspend fun render(tab: EditorTab, doc: NotebookDocument, pageId: Id, lasso: FloatArray): ImageBitmap? {
        val page = doc.page(pageId) ?: return null
        val r = Rasterizer.shared ?: return null
        val area = Lasso(lasso).bounds.outset(1f)
        val content = doc.load(pageId)
        // Lifted selection items are not in the page snapshot; draw them where they are now.
        val lifted = tab.canvas?.selectionItems().orEmpty().takeIf { tab.selection?.pageId == pageId }.orEmpty()
        val items = (content.snapshot.query(area) + lifted).sortedWith(ItemDrawOrder)
        return withContext(Dispatchers.Default) {
            // About 3 px per point, capped so large areas stay under ~2400 px.
            val scale = min(3f, 2400f / max(area.width, area.height))
            val w = ceil(area.width * scale).toInt().coerceAtLeast(1)
            val h = ceil(area.height * scale).toInt().coerceAtLeast(1)
            val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            r.render(full, tab.notebookId, page, items, scale, area.left, area.top)
            // Keep only the inside of the lasso; the rest is transparent.
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val path = Path().apply {
                moveTo((lasso[0] - area.left) * scale, (lasso[1] - area.top) * scale)
                var i = 2
                while (i < lasso.size) { lineTo((lasso[i] - area.left) * scale, (lasso[i + 1] - area.top) * scale); i += 2 }
                close()
            }
            Canvas(out).apply { clipPath(path); drawBitmap(full, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)) }
            full.recycle()
            out.asImageBitmap()
        }
    }

    actual suspend fun insert(tab: EditorTab, doc: NotebookDocument, image: ImageBitmap, pageId: Id, lasso: FloatArray) {
        val assetId = newId()
        val bmp = image.asAndroidBitmap()
        withContext(Dispatchers.IO) {
            val f = File(AppGraph.files.asset(tab.notebookId, "$assetId.png"))
            f.parentFile?.mkdirs()
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val area = Lasso(lasso).bounds.outset(1f)
        val page = doc.page(pageId) ?: return
        // Place the copy just below-right of the original, kept on the page.
        val dx = min(18f, page.width - area.right).coerceAtLeast(-area.left)
        val dy = min(18f, page.height - area.bottom).coerceAtLeast(-area.top)
        val frame = Rect(area.left + dx, area.top + dy, area.right + dx, area.bottom + dy)
        tab.canvas?.commitSelection()
        tab.captureArea = null
        doc.load(pageId)
        val item = ImageItem(newId(), doc.nextSeq(), assetId, frame, aspect = bmp.width.toFloat() / bmp.height)
        doc.edit(pageId, emptyList(), listOf(item))
        Tools.select(ToolKind.Lasso)
        tab.canvas?.select(pageId, listOf(item))
    }

    actual suspend fun saveToPhotos(image: ImageBitmap, title: String): Boolean = withContext(Dispatchers.IO) {
        val resolver = AndroidContext.app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${Exporter.safeName(title)} ${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Mheenotes")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        runCatching {
            resolver.openOutputStream(uri)!!.use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            true
        }.getOrElse {
            resolver.delete(uri, null, null)
            false
        }
    }

    actual suspend fun share(image: ImageBitmap, title: String) {
        val f = withContext(Dispatchers.IO) {
            File(Exporter.exportDir(), "${Exporter.safeName(title)} capture.png").also { out ->
                FileOutputStream(out).use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        Exporter.share(AndroidContext.app, f, "image/png")
    }
}
