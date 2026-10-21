package com.mheenotes.ui.library

import com.composables.icons.lucide.AppWindow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArchiveRestore
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.FolderInput
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.StarOff
import com.composables.icons.lucide.Trash2
import com.mheenotes.core.model.Folder
import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.nowMillis
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Checkbox
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.MenuDivider
import com.mheenotes.ui.design.MenuItem
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Popover
import com.mheenotes.ui.design.PopoverSide
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.pressable
import com.mheenotes.ui.i18n.S
import com.mheenotes.ui.i18n.Strings
import com.mheenotes.ui.platform.NotebookThumbnail

class NotebookActions(
    val open: (Notebook) -> Unit,
    val rename: (Notebook) -> Unit,
    val move: (Notebook) -> Unit,
    val deleteForever: (Notebook) -> Unit,
    val export: (Notebook) -> Unit,
    val sharePdf: (Notebook) -> Unit,
    val openInNewWindow: (Notebook) -> Unit,
)

/** Cover aspect for the grid. Thumbnails letterbox inside it. */
const val COVER_ASPECT = 0.74f

@Composable
fun NotebookCard(
    n: Notebook,
    trash: Boolean,
    selecting: Boolean,
    selected: Boolean,
    actions: NotebookActions,
    modifier: Modifier = Modifier,
) {
    val c = M.colors
    var menu by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (selected) 0.94f else 1f, Motion.bouncy(), label = "sel")
    Column(modifier) {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT)
                    .graphicsLayer { scaleX = lift; scaleY = lift }
                    .pressable(pressedScale = 0.96f) { actions.open(n) }
                    .libraryDraggable(LibraryDrag.shared, { Dragged.NotebookItem(n) }) { menu = true }
                    .shadow(if (c.isDark) 0.dp else 6.dp, RoundedCornerShape(Radii.md), ambientColor = c.shadow, spotColor = c.shadow)
                    .clip(RoundedCornerShape(Radii.md))
                    .background(c.surface)
                    .border(if (selected) 2.5.dp else 0.75.dp, if (selected) c.accent else c.separator, RoundedCornerShape(Radii.md)),
            ) {
                NotebookThumbnail(n, Modifier.fillMaxWidth().aspectRatio(COVER_ASPECT))
                if (n.favorite && !trash) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(CircleShape).background(Color(0xE6FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Lucide.Star, tint = Color(0xFFE7A02C), size = 13.dp) }
                }
                androidx.compose.animation.AnimatedVisibility(
                    selecting,
                    Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    enter = scaleIn(Motion.bouncy()) + fadeIn(Motion.fade()),
                    exit = scaleOut(Motion.snappy()) + fadeOut(Motion.fade()),
                ) {
                    Box(Modifier.clip(CircleShape).background(c.surface)) { Checkbox(selected, { actions.open(n) }) }
                }
            }
            NotebookMenu(n, trash, menu, { menu = false }, actions)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(n.title, style = M.type.calloutStrong, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(subtitle(n, trash, S), style = M.type.caption, color = c.textTertiary, maxLines = 1)
            }
            if (!selecting) {
                Box {
                    IconButton(Lucide.Ellipsis, { menu = true }, size = 28.dp, iconSize = 16.dp, tint = c.textTertiary)
                }
            }
        }
    }
}

@Composable
fun NotebookRow(
    n: Notebook,
    trash: Boolean,
    selecting: Boolean,
    selected: Boolean,
    actions: NotebookActions,
    modifier: Modifier = Modifier,
) {
    val c = M.colors
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.985f) { actions.open(n) }
                .libraryDraggable(LibraryDrag.shared, { Dragged.NotebookItem(n) }) { menu = true }
                .clip(RoundedCornerShape(Radii.md))
                .background(if (selected) c.accentSoft else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) {
                Checkbox(selected, { actions.open(n) })
                Spacer(Modifier.width(14.dp))
            }
            Box(
                Modifier.width(44.dp).aspectRatio(COVER_ASPECT).clip(RoundedCornerShape(6.dp)).background(c.surface)
                    .border(0.75.dp, c.separator, RoundedCornerShape(6.dp)),
            ) { NotebookThumbnail(n, Modifier.fillMaxWidth().aspectRatio(COVER_ASPECT)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.title, Modifier.weight(1f, fill = false), style = M.type.bodyStrong, maxLines = 1)
                    if (n.favorite && !trash) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Lucide.Star, tint = Color(0xFFE7A02C), size = 13.dp)
                    }
                }
                Text(subtitle(n, trash, S), style = M.type.callout, color = c.textTertiary, maxLines = 1)
            }
            if (!selecting) IconButton(Lucide.Ellipsis, { menu = true }, size = 32.dp, iconSize = 17.dp, tint = c.textTertiary)
        }
        Box(Modifier.align(Alignment.CenterEnd)) { NotebookMenu(n, trash, menu, { menu = false }, actions) }
    }
}

@Composable
private fun NotebookMenu(n: Notebook, trash: Boolean, expanded: Boolean, onDismiss: () -> Unit, a: NotebookActions) {
    val s = S
    Popover(expanded, onDismiss, side = PopoverSide.Below) {
        if (trash) {
            MenuItem(s.restore, icon = Lucide.ArchiveRestore) { onDismiss(); AppGraph.restore(listOf(n.id)) }
            MenuItem(s.deleteForever, icon = Lucide.Trash2, danger = true) { onDismiss(); a.deleteForever(n) }
        } else {
            MenuItem(s.openInNewWindow, icon = Lucide.AppWindow) { onDismiss(); a.openInNewWindow(n) }
            MenuItem(s.rename, icon = Lucide.PenLine) { onDismiss(); a.rename(n) }
            MenuItem(if (n.favorite) s.unfavorite else s.favorite, icon = if (n.favorite) Lucide.StarOff else Lucide.Star) {
                onDismiss(); AppGraph.setFavorite(listOf(n.id), !n.favorite)
            }
            MenuItem(s.moveTo, icon = Lucide.FolderInput) { onDismiss(); a.move(n) }
            MenuItem(s.duplicate, icon = Lucide.Copy) { onDismiss(); AppGraph.duplicate(n.id, "(2)") }
            MenuItem(s.sharePdf, icon = Lucide.FileText) { onDismiss(); a.sharePdf(n) }
            MenuItem(s.shareNotebook, icon = Lucide.Share2) { onDismiss(); a.export(n) }
            MenuDivider()
            MenuItem(s.moveToTrash, icon = Lucide.Trash2, danger = true) { onDismiss(); AppGraph.trash(listOf(n.id)) }
        }
    }
}

@Composable
fun FolderTile(
    f: Folder,
    count: Int,
    grid: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    trashed: Boolean = false,
    onDeleteForever: () -> Unit = {},
) {
    val c = M.colors
    val s = S
    var menu by remember { mutableStateOf(false) }
    val drag = LibraryDrag.shared
    val hovered = drag.hovered == "grid:${f.id}" && (drag.item as? Dragged.FolderItem)?.id != f.id
    val lift by animateFloatAsState(if (hovered) 1.05f else 1f, Motion.bouncy(), label = "drop")
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = lift; scaleY = lift }
                .dropZone(drag, "grid:${f.id}", f.id)
                .pressable(pressedScale = 0.96f, onClick = onOpen)
                .then(if (trashed) Modifier else Modifier.libraryDraggable(drag, { Dragged.FolderItem(f.id, f.name) }) { menu = true })
                .clip(RoundedCornerShape(Radii.md))
                .background(if (hovered) c.accentSoft else if (grid) c.surface else Color.Transparent)
                .then(if (grid || hovered) Modifier.border(if (hovered) 2.dp else 0.75.dp, if (hovered) c.accent else c.separator, RoundedCornerShape(Radii.md)) else Modifier)
                .padding(horizontal = 14.dp, vertical = if (grid) 14.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(Lucide.Folder, tint = c.accent, size = 19.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = M.type.calloutStrong, maxLines = 1)
                Text(if (trashed) s.folderTrashed(count) else s.notebooksCount(count), style = M.type.caption, color = c.textTertiary, maxLines = 1)
            }
            if (trashed) {
                IconButton(Lucide.ArchiveRestore, { AppGraph.restoreFolder(f.id) }, size = 32.dp, iconSize = 16.dp, tint = c.textSecondary)
                IconButton(Lucide.Trash2, onDeleteForever, size = 32.dp, iconSize = 16.dp, tint = c.danger)
            }
        }
        Popover(menu, { menu = false }) {
            MenuItem(s.rename, icon = Lucide.PenLine) { menu = false; onRename() }
            MenuItem(s.moveToTrash, icon = Lucide.Trash2, danger = true) { menu = false; onDelete() }
        }
    }
}

fun subtitle(n: Notebook, trash: Boolean, s: Strings): String {
    if (trash) {
        val left = AppGraph.TRASH_DAYS - (nowMillis() - (n.trashedAt ?: 0)) / 86_400_000L
        return s.deletedDaysLeft(left.coerceAtLeast(1))
    }
    return "${relativeTime(n.updatedAt, s)} · ${s.pages(n.pageCount)}"
}

fun relativeTime(at: Long, s: Strings, now: Long = nowMillis()): String {
    val d = (now - at).coerceAtLeast(0)
    val min = d / 60_000
    return when {
        min < 1 -> s.justNow
        min < 60 -> s.minutesAgo(min)
        min < 24 * 60 -> s.hoursAgo(min / 60)
        min < 48 * 60 -> s.yesterday
        else -> s.daysAgo(min / (24 * 60))
    }
}
