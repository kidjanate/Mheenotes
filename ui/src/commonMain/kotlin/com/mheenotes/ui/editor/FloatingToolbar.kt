package com.mheenotes.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Highlighter
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.LassoSelect
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Triangle
import com.composables.icons.lucide.Type
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.ColorDot
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.ListRow
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Popover
import com.mheenotes.ui.design.PopoverSide
import com.mheenotes.ui.design.Segmented
import com.mheenotes.ui.design.Slider
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.Toggle
import com.mheenotes.ui.design.pressable
import com.mheenotes.ui.i18n.S
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Dock { Top, Bottom, Left, Right }

private fun toolIcon(k: ToolKind): ImageVector = when (k) {
    ToolKind.Pen -> Lucide.PenLine
    ToolKind.Pencil -> Lucide.Pencil
    ToolKind.Highlighter -> Lucide.Highlighter
    ToolKind.Eraser -> Lucide.Eraser
    ToolKind.Lasso -> Lucide.LassoSelect
    ToolKind.Shape -> Lucide.Shapes
    ToolKind.Text -> Lucide.Type
    ToolKind.Hand -> Lucide.Hand
}

/**
 * Floating tool pill. Drag the grip to any edge; it springs into place and rotates its
 * layout for side docks. Fades back while the pen is down.
 */
@Composable
fun FloatingToolbar(tab: EditorTab, doc: NotebookDocument, topInsetPx: Int) {
    var dock by remember { mutableStateOf(runCatching { Dock.valueOf(AppGraph.settings.toolbarDock) }.getOrDefault(Dock.Top)) }
    val alpha by animateFloatAsState(if (tab.writing) 0.35f else 1f, Motion.gentle(), label = "tb")
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val area = IntSize(constraints.maxWidth, constraints.maxHeight)
        var size by remember { mutableStateOf(IntSize.Zero) }
        val margin = with(density) { 16.dp.roundToPx() }
        fun target(d: Dock): Offset = when (d) {
            Dock.Top -> Offset((area.width - size.width) / 2f, topInsetPx + margin.toFloat())
            Dock.Bottom -> Offset((area.width - size.width) / 2f, area.height - size.height - margin * 1.5f)
            Dock.Left -> Offset(margin.toFloat(), (area.height - size.height) / 2f)
            Dock.Right -> Offset(area.width - size.width - margin.toFloat(), (area.height - size.height) / 2f)
        }
        val pos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        var dragging by remember { mutableStateOf(false) }
        LaunchedEffect(dock, size, area, topInsetPx) {
            if (size != IntSize.Zero && !dragging) {
                if (pos.value == Offset.Zero) pos.snapTo(target(dock)) else pos.animateTo(target(dock), Motion.bouncy())
            }
        }
        val vertical = dock == Dock.Left || dock == Dock.Right
        Card(
            Modifier
                .offset { IntOffset(pos.value.x.roundToInt(), pos.value.y.roundToInt()) }
                .onSizeChanged { size = it }
                .graphicsLayer { this.alpha = if (size == IntSize.Zero) 0f else alpha },
            shape = RoundedCornerShape(26.dp),
            color = M.colors.elevated,
            elevation = 16.dp,
        ) {
            val grip = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        val c = pos.value + Offset(size.width / 2f, size.height / 2f)
                        val dists = mapOf(
                            Dock.Top to c.y, Dock.Bottom to area.height - c.y,
                            Dock.Left to c.x, Dock.Right to area.width - c.x,
                        )
                        val next = dists.minBy { it.value }.key
                        dock = next
                        AppGraph.settings.updateDock(next.name)
                        scope.launch { pos.animateTo(target(next), Motion.bouncy()) }
                    },
                    onDragCancel = { dragging = false },
                ) { change, amount ->
                    change.consume()
                    scope.launch { pos.snapTo(pos.value + amount) }
                }
            }
            val content: @Composable () -> Unit = {
                Box(grip.size(if (vertical) 40.dp else 22.dp, if (vertical) 22.dp else 40.dp), contentAlignment = Alignment.Center) {
                    Icon(Lucide.GripVertical, Modifier.graphicsLayer { rotationZ = if (vertical) 90f else 0f }, tint = M.colors.textTertiary, size = 16.dp)
                }
                listOf(ToolKind.Pen, ToolKind.Pencil, ToolKind.Highlighter, ToolKind.Eraser, ToolKind.Lasso, ToolKind.Shape, ToolKind.Text).forEach { k ->
                    ToolButton(k, if (vertical) PopoverSide.End else if (dock == Dock.Bottom) PopoverSide.Above else PopoverSide.Below, tab)
                }
                ImageInsertButton(tab, doc)
                Spacer(Modifier.size(if (vertical) 0.dp else 6.dp, if (vertical) 6.dp else 0.dp))
                QuickColors(vertical)
            }
            if (vertical) Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
            else Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) { content() }
        }
    }
}

@Composable
private fun ToolButton(k: ToolKind, side: PopoverSide, tab: EditorTab) {
    var options by remember { mutableStateOf(false) }
    val selected = Tools.current == k
    Box {
        IconButton(
            toolIcon(k),
            {
                if (selected && k != ToolKind.Lasso) options = true
                else { tab.canvas?.commitSelection(); Tools.select(k) }
            },
            selected = selected,
            size = 44.dp,
            iconSize = 21.dp,
            tint = if (selected && (k == ToolKind.Pen || k == ToolKind.Pencil || k == ToolKind.Highlighter)) Color(Tools.colorOf(k) or 0xFF000000.toInt()) else null,
        )
        Popover(options, { options = false }, side = side, width = 300.dp) { ToolOptions(k) }
    }
}

@Composable
private fun ToolOptions(k: ToolKind) {
    val s = S
    val c = M.colors
    Column(Modifier.padding(10.dp)) {
        if (k == ToolKind.Eraser) {
            Segmented(EraserMode.entries, Tools.eraserMode, { Tools.eraserMode = it; Tools.save() }, Modifier.fillMaxWidth()) { o, sel ->
                Text(if (o == EraserMode.Whole) s.eraseWhole else s.erasePartial, style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary)
            }
            Spacer(Modifier.height(14.dp))
        }
        if (k == ToolKind.Shape) {
            Segmented(ShapeKind.entries, Tools.shapeKind, { Tools.shapeKind = it; Tools.save() }, Modifier.fillMaxWidth()) { o, sel ->
                Icon(
                    when (o) {
                        ShapeKind.Line -> Lucide.Minus; ShapeKind.Arrow -> Lucide.ArrowUpRight; ShapeKind.Rectangle -> Lucide.Square
                        ShapeKind.Ellipse -> Lucide.Circle; ShapeKind.Triangle -> Lucide.Triangle
                    },
                    tint = if (sel) c.text else c.textSecondary, size = 17.dp,
                )
            }
            Spacer(Modifier.height(6.dp))
            ListRow(s.fill, Modifier.padding(horizontal = 0.dp)) { Toggle(Tools.shapeFill, { Tools.shapeFill = it }) }
        }
        if (k != ToolKind.Eraser && k != ToolKind.Lasso) {
            Text(s.color, style = M.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(8.dp))
            val palette = if (k == ToolKind.Highlighter) HIGHLIGHT_COLORS else INK_COLORS
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                palette.forEach { col ->
                    ColorDot(Color(col or 0xFF000000.toInt()), Tools.colorOf(k) == col, { Tools.setColor(k, col) }, size = 32.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (k == ToolKind.Text) {
            Text(s.fontSize, style = M.type.caption, color = c.textSecondary)
            Slider(Tools.textSize, { Tools.textSize = it }, Modifier.fillMaxWidth(), range = 8f..48f, onChangeFinished = { Tools.save() })
        } else if (k != ToolKind.Lasso) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.width, Modifier.weight(1f), style = M.type.caption, color = c.textSecondary)
                Text(((Tools.widthOf(k) * 10).roundToInt() / 10f).toString(), style = M.type.caption, color = c.textTertiary)
            }
            val presets = Tools.widthsOf(k)
            Slider(Tools.widthOf(k), { Tools.setWidth(k, it) }, Modifier.fillMaxWidth(), range = presets.first() * 0.4f..presets.last() * 2f)
            if (k == ToolKind.Pen || k == ToolKind.Pencil || k == ToolKind.Highlighter) {
                Spacer(Modifier.height(8.dp))
                StrokePreview(k)
            }
        }
    }
}

@Composable
private fun StrokePreview(k: ToolKind) {
    val c = M.colors
    val tool = Tools.inkTool(k) ?: return
    val color = Tools.colorOf(k)
    val width = Tools.widthOf(k)
    Canvas(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)) {
        val n = 40
        val pts = FloatArray(n * 3) { i ->
            val j = i / 3; val t = j / (n - 1f)
            when (i % 3) {
                0 -> 14f + t * (size.width - 28f) / 2.2f
                1 -> size.height / 2 / 2.2f + kotlin.math.sin(t * 6.28f) * 5f
                else -> 0.2f + 0.8f * kotlin.math.sin(t * 3.14f)
            }
        }
        val s = com.mheenotes.core.model.Stroke("p", 0, tool, color, width, pts)
        scale(2.2f, 2.2f, Offset.Zero) {
            with(com.mheenotes.ui.render.ItemPainter(null, null)) { draw(listOf(s), 2.2f) }
        }
    }
}

/**
 * Quick colors for the active tool. Always present (dimmed when the tool has no color) so the
 * toolbar never changes width and buttons never shift under the finger.
 */
@Composable
private fun QuickColors(vertical: Boolean) {
    val k = Tools.current
    val enabled = k == ToolKind.Pen || k == ToolKind.Pencil || k == ToolKind.Highlighter || k == ToolKind.Shape || k == ToolKind.Text
    val palette = (if (k == ToolKind.Highlighter) HIGHLIGHT_COLORS else INK_COLORS).take(3)
    val alpha by animateFloatAsState(if (enabled) 1f else 0.25f, Motion.fade(), label = "qc")
    val content: @Composable () -> Unit = {
        palette.forEach { col ->
            ColorDot(Color(col or 0xFF000000.toInt()), enabled && Tools.colorOf(k) == col, { if (enabled) Tools.setColor(k, col) }, size = 34.dp)
        }
    }
    val m = Modifier.graphicsLayer { this.alpha = alpha }
    if (vertical) Column(m) { content() } else Row(m) { content() }
}

@Composable
expect fun ImageInsertButton(tab: EditorTab, doc: NotebookDocument)
