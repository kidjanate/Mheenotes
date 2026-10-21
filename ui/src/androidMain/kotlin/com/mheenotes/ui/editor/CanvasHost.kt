package com.mheenotes.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.newId
import com.mheenotes.ui.AndroidContext
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.M
import com.mheenotes.ui.platform.ThumbCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

fun rasterizer(fonts: com.mheenotes.ui.design.AppFonts): Rasterizer =
    Rasterizer.shared ?: Rasterizer(AndroidContext.app, fonts.regular, fonts.bold).also { Rasterizer.shared = it }

@Composable
actual fun InkCanvas(tab: EditorTab, doc: NotebookDocument, topInset: Dp, modifier: Modifier) {
    val context = LocalContext.current
    val colors = M.colors
    val fonts = M.fonts
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val chrome = remember(tab) {
        CanvasChrome(
            desk = colors.desk.toArgb(), accent = colors.accent.toArgb(), shadow = colors.shadow.toArgb(),
            topInset = 0f, bottomInset = 0f,
            onTextTap = { pageId, x, y, existing ->
                tab.textEdit = if (existing != null) TextEdit(pageId, existing.id, existing.frame.left, existing.frame.top, existing.frame.width, existing.text)
                else TextEdit(pageId, null, x, y - Tools.textSize * 0.6f, 260f, "")
            },
            onUndo = { tab.canvas?.commitSelection(); scope.launch { doc.undo() } },
            onRedo = { scope.launch { doc.redo() } },
        )
    }
    chrome.desk = colors.desk.toArgb(); chrome.accent = colors.accent.toArgb(); chrome.shadow = colors.shadow.toArgb()
    chrome.topInset = with(density) { topInset.toPx() }
    chrome.bottomInset = with(density) { 24.dp.toPx() }
    val view = remember(tab, doc) { InkCanvasView(context, tab, doc, rasterizer(fonts), chrome) }
    // Split screen and floating windows resize the surface underneath us; the front buffer can
    // then show stale frames above the app. Use the normal path there.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val multiWindow = remember(configuration) { (context as? android.app.Activity)?.isInMultiWindowMode == true }
    val lowLatency = AppGraph.settings.lowLatencyInk && !multiWindow
    val container = remember(view, lowLatency) {
        android.widget.FrameLayout(context).apply {
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            addView(view)
            if (lowLatency) {
                // Not clickable, so touches fall through to the canvas underneath.
                val surface = android.view.SurfaceView(context)
                addView(surface)
                view.front = runCatching { FrontInk(surface) }.getOrNull()
            } else {
                view.front?.release(); view.front = null
            }
        }
    }
    DisposableEffect(view) {
        tab.canvas = object : CanvasController {
            override fun commitSelection() = view.commitSelection()
            override fun deleteSelection() = view.deleteSelection()
            override fun replaceSelection(transform: (Item) -> Item) = view.replaceSelection(transform)
            override fun selectionItems(): List<Item> = view.currentSelectionItems()
            override fun select(pageId: Id, items: List<Item>) = view.select(pageId, items)
        }
        onDispose {
            view.commitSelection()
            view.release()
            tab.canvas = null
        }
    }
    LaunchedEffect(view) { doc.pages.collect { view.setPages(it) } }
    LaunchedEffect(view, tab.scrollRequest) {
        when (val r = tab.scrollRequest) {
            is ScrollRequest.ToPage -> { delay(16); view.scrollToPage(r.index, r.animate) }
            ScrollRequest.FitWidth -> view.fitWidth()
            null -> Unit
        }
        tab.scrollRequest = null
    }
    LaunchedEffect(Tools.current) {
        if (Tools.current != ToolKind.Lasso) { view.commitSelection(); tab.captureArea = null; view.invalidate() }
    }
    AndroidView({ container }, modifier, update = {
        tab.captureArea // repaint when the capture outline appears or goes away
        view.invalidate()
    })
}

// ---- Page thumbnails -----------------------------------------------------------------------

private object PageThumbs {
    val cache = object : LruCache<Id, Pair<Long, ImageBitmap>>(40 * 1024 * 1024) {
        override fun sizeOf(key: Id, value: Pair<Long, ImageBitmap>) = value.second.width * value.second.height * 4
    }
    val io = Dispatchers.Default.limitedParallelism(2)
}

/** Renders a page (or its first part) at [widthPx] wide. */
suspend fun renderPageBitmap(notebookId: Id, doc: NotebookDocument, page: Page, widthPx: Int, fonts: com.mheenotes.ui.design.AppFonts? = null): Bitmap {
    val content = doc.load(page.id)
    val snapshot = content.snapshot
    val r = Rasterizer.shared ?: rasterizer(fonts ?: error("fonts needed"))
    return withContext(PageThumbs.io) {
        val scale = widthPx / page.width
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
        r.render(bmp, notebookId, page, snapshot.items, scale, 0f, 0f)
        bmp
    }
}

@Composable
actual fun PageThumbnail(tab: EditorTab, doc: NotebookDocument, page: Page, modifier: Modifier) {
    val fonts = M.fonts
    val revision by doc.revision.collectAsState()
    var bmp by remember(page.id) { mutableStateOf(PageThumbs.cache.get(page.id)?.second) }
    LaunchedEffect(page, revision) {
        val version = doc.content(page.id)?.snapshot?.version ?: -1L
        val cached = PageThumbs.cache.get(page.id)
        if (cached != null && (version == -1L || cached.first == version)) { bmp = cached.second; return@LaunchedEffect }
        if (cached != null) delay(500) // let bursts of edits settle
        val rendered = renderPageBitmap(tab.notebookId, doc, page, 280, fonts).asImageBitmap()
        PageThumbs.cache.put(page.id, (doc.content(page.id)?.snapshot?.version ?: 0L) to rendered)
        bmp = rendered
    }
    Box(modifier.background(Color(page.backgroundArgb))) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
    }
}

actual object Thumbnails {
    actual suspend fun render(notebookId: Id, doc: NotebookDocument): Boolean {
        val page = doc.pages.value.firstOrNull() ?: return false
        if (Rasterizer.shared == null) return false
        val bmp = renderPageBitmap(notebookId, doc, page, 360)
        return withContext(Dispatchers.IO) {
            runCatching {
                val f = File(AppGraph.files.thumb(notebookId))
                val tmp = File(f.path + ".tmp")
                FileOutputStream(tmp).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                tmp.renameTo(f)
                AppGraph.library.notebook(notebookId)?.let { n -> ThumbCache.put(ThumbCache.key(n.id, n.thumbVersion + 1), bmp.asImageBitmap()) }
                true
            }.getOrDefault(false)
        }
    }
}

// ---- Images --------------------------------------------------------------------------------

@Composable
actual fun ImageInsertButton(tab: EditorTab, doc: NotebookDocument) {
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { insertImage(tab, doc, uri) }
    }
    IconButton(Lucide.ImagePlus, {
        tab.canvas?.commitSelection()
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }, size = TOOL_BUTTON, iconSize = TOOL_ICON)
}

/** Copies a picked image into the notebook (capped at 2560 px), places it and selects it. */
suspend fun insertImage(tab: EditorTab, doc: NotebookDocument, uri: Uri) {
    val page = doc.pages.value.getOrNull(tab.currentPage) ?: return
    val assetId = newId()
    val size = withContext(Dispatchers.IO) { copyImageAsset(tab.notebookId, assetId, uri) } ?: return
    val (w, h) = size
    val aspect = w.toFloat() / h
    val fw = min(page.width * 0.6f, w.toFloat())
    val fh = fw / aspect
    // Center in the visible part of the page.
    val v = tab.viewport
    val layout = v.layout ?: return
    val i = layout.indexOf(page.id)
    val viewCenterY = ((AndroidContext.app.resources.displayMetrics.heightPixels / 2f) - v.ty) / v.scale - layout.tops[i]
    val cy = viewCenterY.coerceIn(fh / 2, max(fh / 2, page.height - fh / 2))
    val cx = page.width / 2
    doc.load(page.id)
    val item = ImageItem(newId(), doc.nextSeq(), assetId, Rect(cx - fw / 2, cy - fh / 2, cx + fw / 2, cy + fh / 2), aspect = aspect)
    doc.edit(page.id, emptyList(), listOf(item))
    Tools.select(ToolKind.Lasso)
    tab.canvas?.select(page.id, listOf(item))
}

private fun copyImageAsset(notebookId: Id, assetId: Id, uri: Uri): Pair<Int, Int>? {
    val resolver = AndroidContext.app.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2560) sample *= 2
    val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
    val png = bounds.outMimeType == "image/png"
    val file = File(AppGraph.files.asset(notebookId, "$assetId.${if (png) "png" else "jpg"}"))
    file.parentFile?.mkdirs()
    FileOutputStream(file).use { bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, it) }
    return bmp.width to bmp.height
}
