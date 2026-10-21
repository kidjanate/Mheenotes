package com.mheenotes.ui.editor

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.model.Page
import com.mheenotes.ui.design.Divider
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Popover
import com.mheenotes.ui.design.PopoverSide
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.pressable
import com.mheenotes.ui.i18n.S
import kotlin.math.roundToInt

/** Thumbnail column for jumping, reordering (long-press drag) and page actions. */
@Composable
fun PageRail(tab: EditorTab, doc: NotebookDocument, modifier: Modifier) {
    val c = M.colors
    val pages by doc.pages.collectAsState()
    val list = rememberLazyListState()
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeight by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(tab.currentPage) {
        val visible = list.layoutInfo.visibleItemsInfo.map { it.index }
        if (tab.currentPage !in visible) list.animateScrollToItem(tab.currentPage.coerceAtLeast(0))
    }
    Column(modifier.width(176.dp).fillMaxHeight().background(c.surface)) {
        LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(vertical = 16.dp)) {
            itemsIndexed(pages, key = { _, p -> p.id }) { i, page ->
                val dragging = i == dragIndex
                val lift by animateFloatAsState(if (dragging) 1.06f else 1f, Motion.bouncy(), label = "lift")
                Box(
                    Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f; scaleX = lift; scaleY = lift }
                        .then(if (dragging) Modifier else Modifier.animateItem())
                        .pointerInput(pages) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragIndex = i; dragOffset = 0f; itemHeight = size.height.toFloat() },
                                onDragEnd = {
                                    val steps = (dragOffset / itemHeight).roundToInt()
                                    val to = (dragIndex + steps).coerceIn(0, pages.lastIndex)
                                    if (to != dragIndex) movePage(doc, dragIndex, to)
                                    dragIndex = -1; dragOffset = 0f
                                },
                                onDragCancel = { dragIndex = -1; dragOffset = 0f },
                            ) { change, amount -> change.consume(); dragOffset += amount.y }
                        },
                ) {
                    RailItem(tab, doc, page, i, i == tab.currentPage)
                }
            }
        }
        Divider()
        Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
            IconButton(Lucide.Plus, { addPageAfter(tab, doc, pages.lastIndex) }, tint = c.textSecondary)
        }
    }
}

@Composable
private fun RailItem(tab: EditorTab, doc: NotebookDocument, page: Page, index: Int, current: Boolean) {
    val c = M.colors
    var menu by remember { mutableStateOf(false) }
    val border by animateDpAsState(if (current) 2.5.dp else 0.75.dp, Motion.snappy(), label = "b")
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(page.width / page.height)
                    .pressable(pressedScale = 0.95f, onLongClick = { menu = true }) { tab.scrollRequest = ScrollRequest.ToPage(index) }
                    .shadow(3.dp, RoundedCornerShape(6.dp), ambientColor = c.shadow, spotColor = c.shadow)
                    .clip(RoundedCornerShape(6.dp))
                    .border(border, if (current) c.accent else c.separator, RoundedCornerShape(6.dp)),
            ) { PageThumbnail(tab, doc, page, Modifier.fillMaxWidth().aspectRatio(page.width / page.height)) }
            Popover(menu, { menu = false }, side = PopoverSide.End) { PageMenuItems(tab, doc, index) { menu = false } }
        }
        Spacer(Modifier.height(6.dp))
        Text((index + 1).toString(), style = M.type.caption, color = if (current) c.accent else c.textTertiary)
    }
}

fun movePage(doc: NotebookDocument, from: Int, to: Int) {
    val pages = doc.pages.value.toMutableList()
    val p = pages.removeAt(from)
    pages.add(to, p)
    // Give the moved page an order key between its new neighbours.
    val before = pages.getOrNull(to - 1)?.ord
    val after = pages.getOrNull(to + 1)?.ord
    val ord = when {
        before == null && after == null -> 1.0
        before == null -> after!! - 1.0
        after == null -> before + 1.0
        else -> (before + after) / 2
    }
    pages[to] = p.copy(ord = ord)
    doc.editPages(pages)
}

/** Small rendering of a page, refreshed after edits. Platform-provided. */
@Composable
expect fun PageThumbnail(tab: EditorTab, doc: NotebookDocument, page: Page, modifier: Modifier)
