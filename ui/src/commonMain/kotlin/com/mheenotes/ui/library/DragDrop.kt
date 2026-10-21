package com.mheenotes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Lucide
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Text
import kotlin.math.hypot
import kotlin.math.roundToInt

/** What is being dragged across the library. */
sealed interface Dragged {
    data class NotebookItem(val notebook: Notebook) : Dragged
    data class FolderItem(val id: Id, val name: String) : Dragged
}

/** Shared drag state: the dragged thing, the pointer in window coordinates, and drop zones. */
class LibraryDrag {
    var item by mutableStateOf<Dragged?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    /** Drop zones by key; the value is the folder to move into (null = top level). */
    val zones = mutableStateMapOf<String, Pair<Id?, Rect>>()

    val hovered: String?
        get() = if (item == null) null else zones.entries.firstOrNull { it.value.second.contains(pointer) }?.key

    fun drop() {
        val target = hovered?.let { zones[it] } ?: return
        when (val d = item) {
            is Dragged.NotebookItem -> if (d.notebook.folderId != target.first) AppGraph.move(listOf(d.notebook.id), target.first)
            is Dragged.FolderItem -> if (d.id != target.first) AppGraph.moveFolder(d.id, target.first)
            null -> Unit
        }
    }

    companion object { val shared = LibraryDrag() }
}

/** Registers this element as a place notebooks and folders can be dropped. */
fun Modifier.dropZone(drag: LibraryDrag, key: String, folderId: Id?): Modifier = composed {
    androidx.compose.runtime.DisposableEffect(key) { onDispose { drag.zones.remove(key) } }
    onGloballyPositioned { drag.zones[key] = folderId to it.boundsInWindow() }
}

/**
 * Long-press and drag to move into a folder. A long press that does not move becomes
 * [onLongPress] (the item's menu), so menus and dragging share one gesture.
 */
fun Modifier.libraryDraggable(drag: LibraryDrag, item: () -> Dragged, onLongPress: () -> Unit): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var start by remember { mutableStateOf(Offset.Zero) }
    var moved by remember { mutableStateOf(false) }
    onGloballyPositioned { coords = it }.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { p ->
                val origin = coords?.positionInWindow() ?: Offset.Zero
                start = origin + p
                moved = false
                drag.pointer = start
                drag.item = item()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDragEnd = {
                if (moved) drag.drop() else onLongPress()
                drag.item = null
            },
            onDragCancel = { drag.item = null },
        ) { change, amount ->
            change.consume()
            drag.pointer += amount
            if (hypot(drag.pointer.x - start.x, drag.pointer.y - start.y) > 12.dp.toPx()) moved = true
        }
    }
}

/** Floating preview under the finger while dragging. [rootOffset] is the overlay's window position. */
@Composable
fun DragPreview(drag: LibraryDrag, rootOffset: Offset) {
    val item = drag.item ?: return
    val c = M.colors
    Box(
        Modifier
            .offset { IntOffset((drag.pointer.x - rootOffset.x - 50.dp.toPx()).roundToInt(), (drag.pointer.y - rootOffset.y - 60.dp.toPx()).roundToInt()) }
            .graphicsLayer { rotationZ = -4f; alpha = 0.92f },
    ) {
        when (item) {
            is Dragged.NotebookItem -> Box(
                Modifier.width(100.dp).aspectRatio(COVER_ASPECT).shadow(14.dp, RoundedCornerShape(Radii.sm)).clip(RoundedCornerShape(Radii.sm)).background(c.surface),
            ) { com.mheenotes.ui.platform.NotebookThumbnail(item.notebook, Modifier.width(100.dp).aspectRatio(COVER_ASPECT)) }
            is Dragged.FolderItem -> Card(color = c.elevated, elevation = 14.dp) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Folder, tint = c.accent, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(item.name, style = M.type.calloutStrong, maxLines = 1)
                }
            }
        }
    }
}
