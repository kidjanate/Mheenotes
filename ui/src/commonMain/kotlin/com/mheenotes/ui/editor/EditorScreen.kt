package com.mheenotes.ui.editor

import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Crop
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.LockOpen
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.FileInput
import com.composables.icons.lucide.Files
import com.composables.icons.lucide.FolderInput
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.RotateCw
import com.composables.icons.lucide.Ruler
import com.composables.icons.lucide.Search
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowDownToLine
import com.composables.icons.lucide.ArrowUpToLine
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.CopyPlus
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.FilePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.PanelLeft
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Scissors
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Undo2
import com.composables.icons.lucide.X
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.ShapeItem
import com.mheenotes.core.model.Stroke
import com.mheenotes.core.model.TextItem
import com.mheenotes.core.model.newId
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.AppState
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.ListRow
import com.mheenotes.ui.design.Slider
import com.mheenotes.ui.design.Toggle
import com.mheenotes.ui.design.ColorDot
import com.mheenotes.ui.design.Divider
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.MenuDivider
import com.mheenotes.ui.design.MenuItem
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.PlatformBackHandler
import com.mheenotes.ui.design.Popover
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Spinner
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.pressable
import com.mheenotes.ui.i18n.S
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** In-app clipboard for page items. */
object ItemClipboard {
    var items by mutableStateOf<List<Item>>(emptyList())
}

@Composable
fun EditorScreen(app: AppState) {
    val tab = app.activeTab ?: return
    LaunchedEffect(Unit) { Tools.load() }
    PlatformBackHandler(tab.selection == null && tab.textEdit == null) { app.backToLibrary() }
    PlatformBackHandler(tab.selection != null) { tab.canvas?.commitSelection() }
    var rail by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var topBarPx by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize().background(M.colors.desk)) {
        key(tab.notebookId) {
            val doc = tab.doc
            if (doc == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner() }
            } else {
                Row(Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        rail,
                        enter = slideInHorizontally(Motion.standard()) { -it } + fadeIn(Motion.fade()),
                        exit = slideOutHorizontally(Motion.standard()) { -it } + fadeOut(Motion.fade()),
                    ) {
                        PageRail(tab, doc, Modifier.padding(top = with(density) { topBarPx.toDp() }))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        InkCanvas(tab, doc, with(density) { topBarPx.toDp() }, Modifier.fillMaxSize())
                        var capture by remember { mutableStateOf<CaptureRequest?>(null) }
                        val requestCapture: () -> Unit = { tab.captureTarget()?.let { (page, lasso) -> capture = CaptureRequest(page, lasso) } }
                        SelectionBar(tab, doc, requestCapture)
                        CaptureAreaBar(tab, requestCapture)
                        CaptureModal(tab, doc, capture) { capture = null }
                        val pdfTools = rememberPdfTools(tab, doc)
                        androidx.compose.runtime.SideEffect { tab.pdfTools = pdfTools }
                        PageDialogs(tab, doc, pdfTools)
                        TextEditorOverlay(tab, doc)
                        FloatingToolbar(tab, doc, topBarPx)
                        PageIndicator(tab, doc, Modifier.align(Alignment.BottomEnd)) { rail = !rail }
                    }
                }
            }
        }
        TopBar(app, tab, rail, { rail = !rail }, Modifier.onSizeChanged { topBarPx = it.height })
    }
}

// ---- Top bar -------------------------------------------------------------------------------

@Composable
private fun TopBar(app: AppState, tab: EditorTab, rail: Boolean, onRail: () -> Unit, modifier: Modifier) {
    val c = M.colors
    val s = S
    val doc = tab.doc
    val canUndo by (doc?.canUndo ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()
    val canRedo by (doc?.canRedo ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()
    val scope = rememberCoroutineScope()
    val lib by AppGraph.library.state.collectAsState()
    var more by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().background(c.surface.copy(alpha = 0.94f)).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(Lucide.ChevronLeft, { app.backToLibrary() })
            IconButton(Lucide.PanelLeft, onRail, selected = rail)
            Spacer(Modifier.width(6.dp))
            // Long-press a tab and drag sideways to reorder; neighbours swap as it passes them.
            val widths = remember { mutableMapOf<String, Int>() }
            var dragId by remember { mutableStateOf<String?>(null) }
            var dragX by remember { mutableStateOf(0f) }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                app.tabs.forEach { t ->
                    key(t.notebookId) {
                        val title = lib.notebooks.firstOrNull { it.id == t.notebookId }?.title ?: s.untitled
                        val dragging = dragId == t.notebookId
                        Box(
                            Modifier
                                .onSizeChanged { widths[t.notebookId] = it.width }
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer { translationX = if (dragging) dragX else 0f; scaleX = if (dragging) 1.04f else 1f; scaleY = scaleX }
                                .pointerInput(t.notebookId) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragId = t.notebookId; dragX = 0f },
                                        onDragEnd = { dragId = null; dragX = 0f },
                                        onDragCancel = { dragId = null; dragX = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        dragX += amount.x
                                        val i = app.tabs.indexOfFirst { it.notebookId == t.notebookId }
                                        val gap = 6.dp.toPx()
                                        val next = app.tabs.getOrNull(i + 1)
                                        val prev = app.tabs.getOrNull(i - 1)
                                        if (next != null && dragX > (widths[next.notebookId] ?: 0) / 2f) {
                                            app.tabs.add(i + 1, app.tabs.removeAt(i)); dragX -= (widths[next.notebookId] ?: 0) + gap
                                        } else if (prev != null && dragX < -(widths[prev.notebookId] ?: 0) / 2f) {
                                            app.tabs.add(i - 1, app.tabs.removeAt(i)); dragX += (widths[prev.notebookId] ?: 0) + gap
                                        }
                                    }
                                },
                        ) {
                            TabPill(title, t.notebookId == app.activeTabId, app.tabs.size > 1, { tab.canvas?.commitSelection(); app.activeTabId = t.notebookId }) { app.closeTab(t.notebookId) }
                        }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(Lucide.Undo2, { tab.canvas?.commitSelection(); scope.launch { doc?.undo() } }, enabled = canUndo)
            IconButton(Lucide.Redo2, { scope.launch { doc?.redo() } }, enabled = canRedo)
            Spacer(Modifier.width(4.dp))
            IconButton(Lucide.FilePlus, { doc?.let { requestAddPage(tab, it, tab.currentPage) } })
            IconButton(Lucide.Share2, { export = true })
            ExportModal(tab, export) { export = false }
            Box {
                IconButton(Lucide.Ellipsis, { more = true })
                Popover(more, { more = false }, align = Alignment.End) {
                    MenuItem(s.resetZoom, icon = Lucide.Maximize) { more = false; tab.scrollRequest = ScrollRequest.FitWidth }
                    MenuItem(s.goToPage, icon = Lucide.Hash) { more = false; tab.pageDialog = PageDialog.GoTo }
                    if (doc?.pages?.value?.any { it.pdf != null } == true) {
                        MenuItem(s.searchPdf, icon = Lucide.Search) { more = false; tab.pageDialog = PageDialog.PdfSearch }
                    }
                    if (ItemClipboard.items.isNotEmpty()) MenuItem(s.paste, icon = Lucide.ClipboardPaste) { more = false; doc?.let { paste(tab, it) } }
                    MenuDivider()
                    PageMenuItems(tab, doc, tab.currentPage) { more = false }
                }
            }
        }
        Divider()
    }
}

@Composable
private fun TabPill(title: String, active: Boolean, closable: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val c = M.colors
    Row(
        Modifier
            .pressable(pressedScale = 0.96f, onClick = onClick)
            .height(36.dp)
            .clip(RoundedCornerShape(Radii.sm))
            .background(if (active) c.fillStrong else Color.Transparent)
            .padding(start = 14.dp, end = if (closable) 4.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.widthIn(max = 200.dp), style = if (active) M.type.calloutStrong else M.type.callout, color = if (active) c.text else c.textSecondary, maxLines = 1)
        if (closable) IconButton(Lucide.X, onClose, size = 28.dp, iconSize = 14.dp, tint = c.textTertiary)
    }
}

@Composable
fun PageMenuItems(tab: EditorTab, doc: NotebookDocument?, index: Int, dismiss: () -> Unit) {
    val s = S
    doc ?: return
    val page = doc.pages.value.getOrNull(index)
    MenuItem(s.insertPageBefore, icon = Lucide.ArrowUpToLine) { dismiss(); requestAddPage(tab, doc, index - 1) }
    MenuItem(s.insertPageAfter, icon = Lucide.ArrowDownToLine) { dismiss(); requestAddPage(tab, doc, index) }
    MenuItem(s.insertPdfPages, icon = Lucide.FileInput) { dismiss(); tab.pdfTools?.insertPages(index) }
    MenuItem(s.duplicatePage, icon = Lucide.CopyPlus) { dismiss(); duplicatePage(tab, doc, index) }
    MenuDivider()
    MenuItem(s.pageSettings, icon = Lucide.Ruler) { dismiss(); tab.pageDialog = PageDialog.Settings(index) }
    MenuItem(s.bookmark, icon = Lucide.Bookmark, trailing = if (page?.bookmarked == true) "✓" else null) { dismiss(); toggleBookmark(doc, index) }
    MenuItem(s.rotatePage, icon = Lucide.RotateCw) { dismiss(); rotatePage(tab, doc, index) }
    MenuItem(s.copyToNotebook, icon = Lucide.Files) { dismiss(); tab.pageDialog = PageDialog.CopyMove(index, move = false) }
    MenuItem(s.moveToNotebook, icon = Lucide.FolderInput) { dismiss(); tab.pageDialog = PageDialog.CopyMove(index, move = true) }
    MenuDivider()
    if (doc.pages.value.size > 1) MenuItem(s.deletePage, icon = Lucide.Trash2, danger = true) { dismiss(); deletePage(tab, doc, index) }
}

/** Adds a page after [index]: asks for paper first unless the user turned that off. */
fun requestAddPage(tab: EditorTab, doc: NotebookDocument, index: Int) {
    if (AppGraph.settings.askInsertPage) tab.pageDialog = PageDialog.Insert(index) else addPageAfter(tab, doc, index)
}

fun addPageAfter(tab: EditorTab, doc: NotebookDocument, index: Int) {
    tab.canvas?.commitSelection()
    val pages = doc.pages.value
    val ref = pages.getOrNull(index.coerceIn(0, pages.lastIndex)) ?: return
    val before = pages.getOrNull(index)?.ord ?: (pages.first().ord - 1.0)
    val after = pages.getOrNull(index + 1)?.ord ?: (before + 2.0)
    val page = Page(newId(), (before + after) / 2, ref.width, ref.height, ref.template, ref.backgroundArgb)
    doc.editPages(pages + page)
    tab.scrollRequest = ScrollRequest.ToPage(index + 1)
}

fun duplicatePage(tab: EditorTab, doc: NotebookDocument, index: Int) {
    val pages = doc.pages.value
    val src = pages.getOrNull(index) ?: return
    tab.scope.launch {
        val items = doc.load(src.id).snapshot.items
        val after = pages.getOrNull(index + 1)?.ord ?: (src.ord + 2.0)
        val copy = src.copy(id = newId(), ord = (src.ord + after) / 2)
        doc.batch(
            listOf(
                com.mheenotes.core.doc.Change.Pages(pages, (pages + copy).sortedBy { it.ord }),
                com.mheenotes.core.doc.Change.Items(copy.id, emptyList(), items.map { reId(it, doc.nextSeq()) }),
            ),
        )
        doc.load(copy.id)
        tab.scrollRequest = ScrollRequest.ToPage(index + 1)
    }
}

fun deletePage(tab: EditorTab, doc: NotebookDocument, index: Int) {
    tab.canvas?.commitSelection()
    val pages = doc.pages.value
    if (pages.size <= 1) return
    doc.editPages(pages.filterIndexed { i, _ -> i != index })
}

fun reId(item: Item, seq: Long): Item = when (item) {
    is Stroke -> item.copy(id = newId(), seq = seq)
    is ShapeItem -> item.copy(id = newId(), seq = seq)
    is TextItem -> item.copy(id = newId(), seq = seq)
    is ImageItem -> item.copy(id = newId(), seq = seq)
}

fun paste(tab: EditorTab, doc: NotebookDocument) {
    val items = ItemClipboard.items
    if (items.isEmpty()) return
    val page = doc.pages.value.getOrNull(tab.currentPage) ?: return
    tab.canvas?.commitSelection()
    tab.scope.launch {
        doc.load(page.id)
        val offset = com.mheenotes.core.geometry.Similarity(0f, 0f, dx = 18f, dy = 18f)
        val placed = items.map { reId(transformItem(it, offset), doc.nextSeq()) }
        doc.edit(page.id, emptyList(), placed)
        tab.canvas?.select(page.id, placed)
    }
}

// ---- Selection bar -------------------------------------------------------------------------

@Composable
private fun SelectionBar(tab: EditorTab, doc: NotebookDocument, onCapture: () -> Unit) {
    val s = S
    val c = M.colors
    val sel = tab.selection
    val bounds = tab.selectionScreenBounds()
    var colors by remember { mutableStateOf(false) }
    androidx.compose.animation.AnimatedVisibility(
        sel != null && bounds != null && tab.textEdit == null,
        enter = scaleIn(Motion.bouncy(), initialScale = 0.8f) + fadeIn(Motion.fade()),
        exit = scaleOut(Motion.snappy(), targetScale = 0.9f) + fadeOut(Motion.fade()),
    ) {
        var w by remember { mutableStateOf(0) }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxW = constraints.maxWidth
            Card(
                Modifier
                    .onSizeChanged { w = it.width }
                    .offset {
                        val b = tab.selectionScreenBounds() ?: return@offset IntOffset.Zero
                        val x = (b.centerX - w / 2).roundToInt().coerceIn(16, (maxW - w - 16).coerceAtLeast(16))
                        val above = b.top - 64.dp.toPx() - 40.dp.toPx() // clear the rotate handle
                        val y = if (above > 80.dp.toPx()) above else b.bottom + 16.dp.toPx()
                        IntOffset(x, y.roundToInt())
                    },
                shape = RoundedCornerShape(Radii.lg), color = c.elevated, elevation = 14.dp,
            ) {
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    SelectionActions(tab, doc, sel ?: return@Row, onCapture)
                }
            }
        }
    }
}

/** Context-aware actions: what shows depends on what kind of items are selected. */
@Composable
private fun SelectionActions(tab: EditorTab, doc: NotebookDocument, sel: Selection, onCapture: () -> Unit) {
    val s = S
    val c = M.colors
    val canvas = tab.canvas
    val images = sel.items.filterIsInstance<ImageItem>()
    val texts = sel.items.filterIsInstance<TextItem>()
    val shapes = sel.items.filterIsInstance<ShapeItem>()
    // A locked image only offers unlocking.
    if (images.any { it.locked }) {
        Button(s.unlock, { canvas?.replaceSelection { if (it is ImageItem) it.copy(locked = false) else it } }, icon = Lucide.LockOpen, style = ButtonStyle.Ghost, compact = true)
        return
    }
    var colors by remember { mutableStateOf(false) }
    var textStyle by remember { mutableStateOf(false) }
    var shapeStyle by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf<ImageItem?>(null) }
    IconButton(Lucide.Copy, { ItemClipboard.items = canvas?.selectionItems().orEmpty() })
    IconButton(Lucide.Scissors, { ItemClipboard.items = canvas?.selectionItems().orEmpty(); canvas?.deleteSelection() })
    IconButton(Lucide.CopyPlus, {
        val items = canvas?.selectionItems().orEmpty()
        canvas?.commitSelection()
        val off = com.mheenotes.core.geometry.Similarity(0f, 0f, dx = 18f, dy = 18f)
        val placed = items.map { reId(transformItem(it, off), doc.nextSeq()) }
        doc.edit(sel.pageId, emptyList(), placed)
        canvas?.select(sel.pageId, placed)
    })
    if (sel.items.any { it is Stroke || it is ShapeItem || it is TextItem }) {
        Box {
            val first = sel.items.firstOrNull { it is Stroke || it is ShapeItem || it is TextItem }
            val argb = when (first) { is Stroke -> first.argb; is ShapeItem -> first.argb; is TextItem -> first.argb; else -> 0 }
            Box(Modifier.size(40.dp).pressable(pressedScale = 0.88f) { colors = true }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(Color(argb or 0xFF000000.toInt())))
            }
            Popover(colors, { colors = false }) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(Modifier.padding(6.dp).width(270.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (INK_COLORS + Tools.customColors).distinct().forEach { col ->
                        ColorDot(Color(col), false, { colors = false; canvas?.replaceSelection { recolor(it, col) } })
                    }
                }
            }
        }
    }
    if (texts.isNotEmpty()) {
        Box {
            IconButton(Lucide.Type, { textStyle = true })
            Popover(textStyle, { textStyle = false }, width = 360.dp) {
                TextStyleControls(texts.first(), { proto ->
                    canvas?.replaceSelection { if (it is TextItem) it.copy(bold = proto.bold, italic = proto.italic, underline = proto.underline, align = proto.align, font = proto.font, fontSize = if (proto.fontSize != texts.first().fontSize) proto.fontSize else it.fontSize, argb = if (proto.argb != texts.first().argb) proto.argb else it.argb) else it }
                })
            }
        }
    }
    if (shapes.isNotEmpty()) {
        Box {
            IconButton(Lucide.SlidersHorizontal, { shapeStyle = true })
            Popover(shapeStyle, { shapeStyle = false }, width = 280.dp) {
                Column(Modifier.padding(10.dp)) {
                    val first = shapes.first()
                    Text(s.width, style = M.type.caption, color = c.textSecondary)
                    Slider(first.strokeWidth, { w -> canvas?.replaceSelection { if (it is ShapeItem) it.copy(strokeWidth = w) else it } }, Modifier.fillMaxWidth(), range = 0.5f..12f)
                    ListRow(s.fill) {
                        Toggle(first.fillArgb != null, { on ->
                            canvas?.replaceSelection { if (it is ShapeItem) it.copy(fillArgb = if (on) (it.argb and 0x00FFFFFF) or 0x33000000 else null) else it }
                        })
                    }
                }
            }
        }
    }
    if (images.size == 1) {
        IconButton(Lucide.Crop, { crop = images.single() })
    }
    if (images.isNotEmpty() || texts.isNotEmpty() || shapes.isNotEmpty()) {
        IconButton(Lucide.RotateCw, {
            val b = sel.bounds
            canvas?.replaceSelection { transformItem(it, com.mheenotes.core.geometry.Similarity(b.centerX, b.centerY, rotation = 90f)) }
        })
    }
    IconButton(Lucide.Camera, onCapture)
    Box {
        IconButton(Lucide.Ellipsis, { more = true })
        Popover(more, { more = false }) {
            MenuItem(s.applyPen, icon = Lucide.PenLine) { more = false; canvas?.replaceSelection { applyPen(it) } }
            MenuItem(s.bringToFront, icon = Lucide.ArrowUpToLine) { more = false; canvas?.replaceSelection { reSeq(it, doc.nextSeq()) } }
            MenuItem(s.sendToBack, icon = Lucide.ArrowDownToLine) { more = false; canvas?.replaceSelection { reSeq(it, -doc.nextSeq()) } }
            MenuDivider()
            MenuItem(s.previousPage, icon = Lucide.ArrowUp, enabled = doc.pages.value.indexOfFirst { it.id == sel.pageId } > 0) { more = false; canvas?.moveSelectionToPage(-1) }
            MenuItem(s.nextPage, icon = Lucide.ArrowDown, enabled = doc.pages.value.indexOfFirst { it.id == sel.pageId } < doc.pages.value.lastIndex) { more = false; canvas?.moveSelectionToPage(1) }
            if (images.isNotEmpty()) {
                MenuDivider()
                MenuItem(s.lock, icon = Lucide.Lock) { more = false; canvas?.replaceSelection { if (it is ImageItem) it.copy(locked = true) else it }; canvas?.commitSelection() }
            }
        }
    }
    Spacer(Modifier.width(2.dp))
    Box(Modifier.width(0.75.dp).height(24.dp).background(c.separator))
    IconButton(Lucide.Trash2, { canvas?.deleteSelection() }, tint = c.danger)
    CropModal(tab, crop, onDismiss = { crop = null }) { cropped -> canvas?.replaceSelection { if (it.id == cropped.id) cropped else it } }
}

/** The current ink tool's color and width applied to strokes, shapes and text. */
private fun applyPen(item: Item): Item {
    val tool = Tools.previousInk
    val argb = Tools.colorOf(tool)
    val width = Tools.widthOf(tool)
    return when (item) {
        is Stroke -> item.copy(argb = if (item.tool == com.mheenotes.core.model.Tool.Highlighter) (argb and 0x00FFFFFF) or 0x66000000 else argb or 0xFF000000.toInt(), width = width)
        is ShapeItem -> item.copy(argb = argb or 0xFF000000.toInt(), strokeWidth = width)
        is TextItem -> item.copy(argb = argb or 0xFF000000.toInt())
        else -> item
    }
}

private fun recolor(item: Item, argb: Int): Item = when (item) {
    is Stroke -> if (item.tool == com.mheenotes.core.model.Tool.Highlighter) item.copy(argb = (argb and 0x00FFFFFF) or 0x66000000) else item.copy(argb = argb)
    is ShapeItem -> item.copy(argb = argb, fillArgb = item.fillArgb?.let { (argb and 0x00FFFFFF) or 0x33000000 })
    is TextItem -> item.copy(argb = argb)
    else -> item
}

private fun reSeq(item: Item, seq: Long): Item = when (item) {
    is Stroke -> item.copy(seq = seq)
    is ShapeItem -> item.copy(seq = seq)
    is TextItem -> item.copy(seq = seq)
    is ImageItem -> item.copy(seq = seq)
}

// ---- Page indicator ------------------------------------------------------------------------

@Composable
private fun PageIndicator(tab: EditorTab, doc: NotebookDocument, modifier: Modifier, onClick: () -> Unit) {
    val pages by doc.pages.collectAsState()
    val c = M.colors
    val alpha by animateFloatAsState(if (tab.writing) 0f else 1f, Motion.fade(), label = "pi")
    Box(modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp).graphicsLayer { this.alpha = alpha }) {
        Card(Modifier.pressable(onClick = onClick), shape = RoundedCornerShape(50), color = c.elevated.copy(alpha = 0.95f), elevation = 6.dp) {
            Text(S.pageOf(tab.currentPage + 1, pages.size), Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = M.type.calloutStrong, color = c.textSecondary)
        }
    }
}

@Composable
expect fun InkCanvas(tab: EditorTab, doc: NotebookDocument, topInset: androidx.compose.ui.unit.Dp, modifier: Modifier)
