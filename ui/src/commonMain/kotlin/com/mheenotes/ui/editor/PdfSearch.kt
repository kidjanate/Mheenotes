package com.mheenotes.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Page
import com.mheenotes.ui.design.PlatformBackHandler
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.Divider
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Spinner
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.i18n.S
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** One occurrence of the query: characters [start, end) of the page's extracted PDF text. */
class PdfHit(val pageIndex: Int, val pageId: Id, val start: Int, val end: Int)

/** Find-in-PDF state for one tab. Hits are in page order; highlight boxes resolve lazily per page. */
class PdfSearchState {
    var query by mutableStateOf("")
    var hits by mutableStateOf<List<PdfHit>>(emptyList())
        private set
    var current by mutableIntStateOf(-1)
    var searching by mutableStateOf(false)
    /** Highlight boxes in page units, by hit index. */
    val boxes = mutableStateMapOf<Int, List<Rect>>()
    /** Hit indices per page (contiguous because hits are sorted). */
    var byPage: Map<Id, IntRange> = emptyMap()
        private set
    private val resolving = HashSet<Id>()

    fun setResults(list: List<PdfHit>, from: Int) {
        boxes.clear()
        resolving.clear()
        byPage = list.indices.groupBy { list[it].pageId }.mapValues { (_, v) -> v.first()..v.last() }
        hits = list
        current = if (list.isEmpty()) -1 else list.indexOfFirst { it.pageIndex >= from }.takeIf { it >= 0 } ?: 0
    }

    fun step(delta: Int) {
        if (hits.isNotEmpty()) current = ((current + delta) % hits.size + hits.size) % hits.size
    }

    /** Computes highlight boxes for every hit on [page], once. */
    suspend fun resolve(tools: PdfTools, page: Page) {
        val range = byPage[page.id] ?: return
        if (range.first in boxes || !resolving.add(page.id)) return
        val list = hits
        val result = tools.boxes(page, list.subList(range.first, range.last + 1))
        if (hits !== list) return
        result.forEachIndexed { k, b -> boxes[range.first + k] = b }
    }
}

/** Compact find bar at the top right: query, "n/total", previous, next, close. */
@Composable
fun PdfSearchBar(tab: EditorTab, doc: NotebookDocument, tools: PdfTools, topInsetPx: Int) {
    var last by remember { mutableStateOf<PdfSearchState?>(null) }
    tab.search?.let { last = it }
    val state = last ?: return
    val s = S
    val c = M.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    PlatformBackHandler(tab.search != null) { tab.search = null }

    if (tab.search != null) {
        LaunchedEffect(state.query) {
            val q = state.query.trim()
            if (q.isEmpty()) { state.setResults(emptyList(), 0); return@LaunchedEffect }
            delay(250)
            state.searching = true
            try {
                state.setResults(tools.search(q), tab.currentPage)
            } finally {
                state.searching = false
            }
        }
        // Scroll to the current hit, centred on its highlight once that is known.
        LaunchedEffect(state.current, state.hits) {
            val hit = state.hits.getOrNull(state.current) ?: return@LaunchedEffect
            val index = doc.pages.value.indexOfFirst { it.id == hit.pageId }
            val page = doc.pages.value.getOrNull(index) ?: return@LaunchedEffect
            state.resolve(tools, page)
            val b = state.boxes[state.current]
            tab.scrollRequest = if (b.isNullOrEmpty()) ScrollRequest.ToPage(index) else ScrollRequest.Reveal(index, b.reduce { a, r -> a.union(r) })
        }
        // Highlight hits on the pages around the view as it moves.
        LaunchedEffect(state.hits) {
            snapshotFlow { tab.currentPage }.collectLatest { cp ->
                for (i in cp - 1..cp + 2) doc.pages.value.getOrNull(i)?.let { state.resolve(tools, it) }
            }
        }
        LaunchedEffect(state) { focus.requestFocus() }
    }
    DisposableEffect(state) { onDispose { tools.endSearch() } }

    val go: (Int) -> Unit = { d -> keyboard?.hide(); state.step(d) }
    val top = with(LocalDensity.current) { topInsetPx.toDp() }
    Box(Modifier.fillMaxSize().padding(top = top + 12.dp, end = 12.dp), contentAlignment = Alignment.TopEnd) {
        AnimatedVisibility(
            tab.search != null,
            enter = scaleIn(Motion.bouncy(), initialScale = 0.85f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)) + fadeIn(Motion.fade()),
            exit = scaleOut(Motion.snappy(), targetScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)) + fadeOut(Motion.fade()),
        ) {
            Card(shape = RoundedCornerShape(Radii.lg), color = c.elevated, elevation = 14.dp) {
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Search, tint = c.textSecondary, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        state.query,
                        { state.query = it },
                        Modifier.width(180.dp).focusRequester(focus),
                        singleLine = true,
                        textStyle = M.type.callout.copy(color = c.text),
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { go(1) }),
                        decorationBox = { inner ->
                            Box {
                                if (state.query.isEmpty()) Text(s.searchPdf, style = M.type.callout, color = c.textTertiary, maxLines = 1)
                                inner()
                            }
                        },
                    )
                    Box(Modifier.widthIn(min = 52.dp).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterEnd) {
                        when {
                            state.searching -> Spinner(size = 14.dp)
                            state.query.isNotBlank() -> Text(
                                if (state.hits.isEmpty()) "0/0" else "${state.current + 1}/${state.hits.size}",
                                style = M.type.caption,
                                color = if (state.hits.isEmpty()) c.textTertiary else c.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                    Divider(Modifier.height(20.dp), vertical = true)
                    Spacer(Modifier.width(2.dp))
                    IconButton(Lucide.ChevronUp, { go(-1) }, enabled = state.hits.isNotEmpty(), size = 34.dp, iconSize = 18.dp)
                    IconButton(Lucide.ChevronDown, { go(1) }, enabled = state.hits.isNotEmpty(), size = 34.dp, iconSize = 18.dp)
                    IconButton(Lucide.X, { keyboard?.hide(); tab.search = null }, size = 34.dp, iconSize = 18.dp)
                }
            }
        }
    }
}
