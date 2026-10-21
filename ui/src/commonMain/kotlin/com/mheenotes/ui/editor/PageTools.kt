package com.mheenotes.ui.editor

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.mheenotes.core.doc.Change
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.geometry.Similarity
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.PAPER_WHITE
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PageSize
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.newId
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.AppState
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.Checkbox
import com.mheenotes.ui.design.ColorDot
import com.mheenotes.ui.design.Divider
import com.mheenotes.ui.design.ListRow
import com.mheenotes.ui.design.LocalOverlays
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.ModalActions
import com.mheenotes.ui.design.ModalTitle
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Segmented
import com.mheenotes.ui.design.Spinner
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.TextField
import com.mheenotes.ui.library.PAPER_COLORS
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Dialogs opened from page menus, rendered once by the editor screen. */
sealed interface PageDialog {
    data class Settings(val index: Int) : PageDialog
    data class Insert(val afterIndex: Int) : PageDialog
    data object GoTo : PageDialog
    data class CopyMove(val index: Int, val move: Boolean) : PageDialog
}

enum class PaperPreset { A4, Letter, A5, A3, Square, Wide, Screen, Custom }

/** Everything that describes a blank page's paper. */
data class PageSetup(
    val template: Template,
    val preset: PaperPreset,
    val landscape: Boolean,
    val paperArgb: Int,
    val lineArgb: Int?,
    val customMm: Pair<Float, Float> = 210f to 297f,
) {
    fun size(screenAspect: Float): PageSize {
        val base = when (preset) {
            PaperPreset.A4 -> PageSize.A4
            PaperPreset.Letter -> PageSize.Letter
            PaperPreset.A5 -> PageSize.A5
            PaperPreset.A3 -> PageSize.A3
            PaperPreset.Square -> PageSize.Square
            PaperPreset.Wide -> PageSize.Wide.portrait()
            PaperPreset.Screen -> PageSize(595f, 595f / screenAspect.coerceIn(0.3f, 3f)).portrait()
            PaperPreset.Custom -> PageSize(customMm.first * MM, customMm.second * MM).portrait()
        }
        return if (landscape) base.landscape() else base.portrait()
    }

    companion object {
        const val MM = 72f / 25.4f

        fun from(page: Page): PageSetup {
            val known = mapOf(
                PageSize.A4 to PaperPreset.A4, PageSize.Letter to PaperPreset.Letter, PageSize.A5 to PaperPreset.A5,
                PageSize.A3 to PaperPreset.A3, PageSize.Square to PaperPreset.Square, PageSize.Wide.portrait() to PaperPreset.Wide,
            )
            val portrait = PageSize(page.width, page.height).portrait()
            val preset = known[portrait] ?: PaperPreset.Custom
            return PageSetup(page.template, preset, page.width > page.height, page.backgroundArgb, page.lineArgb, portrait.width / MM to portrait.height / MM)
        }
    }
}

val LINE_COLORS = listOf(null, 0x2E3A5A8C, 0x40E5484D, 0x402FA36B, 0x408E4EC6, 0x55000000)

@Composable
fun PageSetupForm(setup: PageSetup, onChange: (PageSetup) -> Unit, showSize: Boolean = true) {
    val c = M.colors
    Text(stringResource(Res.string.template), style = M.type.calloutStrong, color = c.textSecondary)
    Spacer(Modifier.height(8.dp))
    Segmented(Template.entries, setup.template, { onChange(setup.copy(template = it)) }, Modifier.fillMaxWidth()) { o, sel ->
        Text(templateLabel(o), style = M.type.caption, color = if (sel) c.text else c.textSecondary, maxLines = 1)
    }
    if (showSize) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.paper), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PaperPreset.entries.forEach { p -> com.mheenotes.ui.design.Chip(presetLabel(p), p == setup.preset, { onChange(setup.copy(preset = p)) }) }
        }
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(setup.preset == PaperPreset.Custom) {
            Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var w by remember { mutableStateOf(setup.customMm.first.roundToInt().toString()) }
                var h by remember { mutableStateOf(setup.customMm.second.roundToInt().toString()) }
                fun push() {
                    val wv = w.toFloatOrNull()?.coerceIn(30f, 1500f) ?: return
                    val hv = h.toFloatOrNull()?.coerceIn(30f, 1500f) ?: return
                    onChange(setup.copy(customMm = wv to hv))
                }
                TextField(w, { w = it.filter(Char::isDigit).take(4); push() }, Modifier.weight(1f), placeholder = "mm")
                Text("×", color = c.textSecondary)
                TextField(h, { h = it.filter(Char::isDigit).take(4); push() }, Modifier.weight(1f), placeholder = "mm")
                Text("mm", style = M.type.callout, color = c.textTertiary)
            }
        }
        Segmented(listOf(false, true), setup.landscape, { onChange(setup.copy(landscape = it)) }, Modifier.fillMaxWidth()) { o, sel ->
            Text(if (o) stringResource(Res.string.landscape) else stringResource(Res.string.portrait), style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.paper_color), style = M.type.calloutStrong, color = c.textSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PAPER_COLORS.forEach { p -> ColorDot(Color(p), p == setup.paperArgb, { onChange(setup.copy(paperArgb = p)) }) }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.line_color), style = M.type.calloutStrong, color = c.textSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                LINE_COLORS.forEach { l ->
                    // Line colors are faint on paper; show them at full strength so they can be told apart.
                    val shown = (l?.let { Color(it) } ?: com.mheenotes.ui.render.templateLineColor(setup.paperArgb)).copy(alpha = 1f)
                    ColorDot(shown, l == setup.lineArgb, { onChange(setup.copy(lineArgb = l)) }, size = 28.dp)
                }
            }
        }
    }
}

@Composable
private fun presetLabel(p: PaperPreset): String {
    return when (p) {
        PaperPreset.A4 -> stringResource(Res.string.size_a4); PaperPreset.Letter -> stringResource(Res.string.size_letter); PaperPreset.A5 -> stringResource(Res.string.size_a5); PaperPreset.A3 -> stringResource(Res.string.size_a3)
        PaperPreset.Square -> stringResource(Res.string.size_square); PaperPreset.Wide -> stringResource(Res.string.size_wide); PaperPreset.Screen -> stringResource(Res.string.size_screen); PaperPreset.Custom -> stringResource(Res.string.size_custom)
    }
}

@Composable
fun screenAspect(): Float {
    val size = LocalWindowInfo.current.containerSize
    return if (size.height == 0) 0.7f else size.width.toFloat() / size.height
}

// ---- Operations ----------------------------------------------------------------------------

fun insertPage(tab: EditorTab, doc: NotebookDocument, afterIndex: Int, setup: PageSetup, aspect: Float) {
    tab.canvas?.commitSelection()
    val pages = doc.pages.value
    val before = pages.getOrNull(afterIndex)?.ord ?: (pages.first().ord - 1.0)
    val after = pages.getOrNull(afterIndex + 1)?.ord ?: (before + 2.0)
    val size = setup.size(aspect)
    val page = Page(newId(), (before + after) / 2, size.width, size.height, setup.template, setup.paperArgb, lineArgb = setup.lineArgb)
    doc.editPages(pages + page)
    tab.scrollRequest = ScrollRequest.ToPage(afterIndex + 1)
}

fun applyPageSetup(doc: NotebookDocument, indices: Collection<Int>, setup: PageSetup, aspect: Float) {
    val size = setup.size(aspect)
    doc.editPages(doc.pages.value.mapIndexed { i, p ->
        if (i !in indices) p
        else if (p.pdf != null) p.copy(backgroundArgb = setup.paperArgb)
        else p.copy(template = setup.template, width = size.width, height = size.height, backgroundArgb = setup.paperArgb, lineArgb = setup.lineArgb)
    })
}

fun toggleBookmark(doc: NotebookDocument, index: Int) {
    doc.editPages(doc.pages.value.mapIndexed { i, p -> if (i == index) p.copy(bookmarked = !p.bookmarked) else p })
}

/** Turns the page a quarter clockwise: its size swaps and everything on it turns with it. */
fun rotatePage(tab: EditorTab, doc: NotebookDocument, index: Int) {
    val pages = doc.pages.value
    val page = pages.getOrNull(index) ?: return
    tab.canvas?.commitSelection()
    tab.scope.launch {
        val items = doc.load(page.id).snapshot.items
        val turn = Similarity(0f, 0f, rotation = 90f, dx = page.height)
        val rotated = page.copy(width = page.height, height = page.width, rotation = (page.rotation + 90) % 360)
        doc.batch(
            listOf(
                Change.Pages(pages, pages.map { if (it.id == page.id) rotated else it }),
                Change.Items(page.id, items, items.map { transformItem(it, turn) }),
            ),
        )
    }
}

/** Copies (or moves) a page with its ink, objects and files into another open notebook. */
fun copyPageTo(from: EditorTab, fromDoc: NotebookDocument, index: Int, to: EditorTab, toDoc: NotebookDocument, move: Boolean) {
    val page = fromDoc.pages.value.getOrNull(index) ?: return
    from.canvas?.commitSelection()
    from.scope.launch {
        val items = fromDoc.load(page.id).snapshot.items
        // Files the page refers to must exist in the target notebook too.
        val assets = items.filterIsInstance<ImageItem>().map { it.assetId } + listOfNotNull(page.pdf?.assetId)
        AppGraph.io.launch {
            for (id in assets.distinct()) copyAsset(from.notebookId, to.notebookId, id)
        }.join()
        val target = toDoc.pages.value
        val copy = page.copy(id = newId(), ord = (target.lastOrNull()?.ord ?: 0.0) + 1.0, bookmarked = false)
        toDoc.batch(
            listOf(
                Change.Pages(target, target + copy),
                Change.Items(copy.id, emptyList(), items.map { reId(it, toDoc.nextSeq()) }),
            ),
        )
        toDoc.load(copy.id)
        if (move && fromDoc.pages.value.size > 1) fromDoc.editPages(fromDoc.pages.value.filter { it.id != page.id })
    }
}

/** Copies one asset file (any extension) between notebook folders. */
expect fun copyAsset(fromNotebook: String, toNotebook: String, assetId: String)

/** Platform PDF features for the editor. */
interface PdfTools {
    /** Picks a PDF and inserts its pages after [afterIndex]. */
    fun insertPages(afterIndex: Int)

    /** Every occurrence of [query] in the notebook's PDF text, in page order. */
    suspend fun search(query: String): List<PdfHit>

    /** Highlight boxes in page units (one per line a hit spans) for [hits], all on [page]. */
    suspend fun boxes(page: Page, hits: List<PdfHit>): List<List<Rect>>

    /** Frees text-layer resources kept open while searching. */
    fun endSearch()
}

@Composable
expect fun rememberPdfTools(tab: EditorTab, doc: NotebookDocument): PdfTools

// ---- Dialogs -------------------------------------------------------------------------------

@Composable
fun PageDialogs(tab: EditorTab, doc: NotebookDocument, pdfTools: PdfTools) {
    val dialog = tab.pageDialog
    val close = { tab.pageDialog = null }
    val aspect = screenAspect()
    val pages by doc.pages.collectAsState()

    // Page settings
    val settings = dialog as? PageDialog.Settings
    val settingsPage = settings?.let { pages.getOrNull(it.index) }
    var setup by remember(settings) { mutableStateOf(settingsPage?.let { PageSetup.from(it) }) }
    var applyAll by remember(settings) { mutableStateOf(false) }
    Modal(settings != null, close, width = 560.dp) {
        ModalTitle(stringResource(Res.string.page_settings))
        setup?.let { st -> PageSetupForm(st, { setup = it }, showSize = settingsPage?.pdf == null) }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(applyAll, { applyAll = it })
            Spacer(Modifier.width(10.dp))
            Text(stringResource(Res.string.apply_to_all), style = M.type.body)
        }
        ModalActions {
            Button(stringResource(Res.string.cancel), close)
            Button(stringResource(Res.string.done), {
                val st = setup
                if (st != null && settings != null) applyPageSetup(doc, if (applyAll) pages.indices.toList() else listOf(settings.index), st, aspect)
                close()
            }, style = ButtonStyle.Primary)
        }
    }

    // Insert page
    val insert = dialog as? PageDialog.Insert
    var insertSetup by remember(insert) {
        mutableStateOf(pages.getOrNull(insert?.afterIndex ?: 0)?.let { PageSetup.from(it).copy(template = if (it.pdf != null) Template.Lined else it.template) })
    }
    var dontAsk by remember(insert) { mutableStateOf(false) }
    Modal(insert != null, close, width = 560.dp) {
        ModalTitle(stringResource(Res.string.add_page))
        insertSetup?.let { st -> PageSetupForm(st, { insertSetup = it }) }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(dontAsk, { dontAsk = it })
            Spacer(Modifier.width(10.dp))
            Text(stringResource(Res.string.dont_ask_again), style = M.type.body)
        }
        ModalActions {
            Button(stringResource(Res.string.cancel), close)
            Button(stringResource(Res.string.insert), {
                val st = insertSetup
                if (st != null && insert != null) insertPage(tab, doc, insert.afterIndex, st, aspect)
                if (dontAsk) AppGraph.settings.updateAskInsert(false)
                close()
            }, style = ButtonStyle.Primary)
        }
    }

    // Go to page
    var goTo by remember(dialog) { mutableStateOf("") }
    Modal(dialog == PageDialog.GoTo, close, width = 360.dp) {
        ModalTitle(stringResource(Res.string.go_to_page), stringResource(Res.string.page_of, tab.currentPage + 1, pages.size))
        val target = goTo.toIntOrNull()?.takeIf { it in 1..pages.size }
        TextField(goTo, { goTo = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), placeholder = "1–${pages.size}", onSubmit = {
            target?.let { tab.scrollRequest = ScrollRequest.ToPage(it - 1); close() }
        })
        ModalActions {
            Button(stringResource(Res.string.cancel), close)
            Button(stringResource(Res.string.open), { target?.let { tab.scrollRequest = ScrollRequest.ToPage(it - 1) }; close() }, style = ButtonStyle.Primary, enabled = target != null)
        }
    }

    // Copy or move to another open notebook
    val copyMove = dialog as? PageDialog.CopyMove
    val lib by AppGraph.library.state.collectAsState()
    Modal(copyMove != null, close, width = 420.dp) {
        ModalTitle(if (copyMove?.move == true) stringResource(Res.string.move_to_notebook) else stringResource(Res.string.copy_to_notebook))
        val others = AppState.allTabs().filter { it !== tab && it.doc != null }
        if (others.isEmpty()) Text(stringResource(Res.string.open_another_notebook), style = M.type.callout, color = M.colors.textSecondary)
        Card(Modifier.fillMaxWidth().heightIn(max = 320.dp), color = M.colors.fill, border = false, shape = RoundedCornerShape(Radii.md)) {
            LazyColumn {
                items(others, key = { it.notebookId }) { other ->
                    val title = lib.notebooks.firstOrNull { it.id == other.notebookId }?.title ?: stringResource(Res.string.untitled)
                    ListRow(title, icon = Lucide.FileText, onClick = {
                        val cm = copyMove ?: return@ListRow
                        copyPageTo(tab, doc, cm.index, other, other.doc ?: return@ListRow, cm.move)
                        close()
                    })
                    Divider(Modifier.padding(start = 48.dp))
                }
            }
        }
        ModalActions { Button(stringResource(Res.string.cancel), close) }
    }
}
