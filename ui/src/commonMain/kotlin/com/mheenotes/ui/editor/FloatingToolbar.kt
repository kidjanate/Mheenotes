package com.mheenotes.ui.editor

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
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
import androidx.compose.runtime.key
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
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Sparkle
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Squircle
import com.composables.icons.lucide.Triangle
import com.composables.icons.lucide.Type
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.ui.render.drawAnimatedStroke
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.ColorDot
import com.mheenotes.ui.design.ColorPicker
import com.composables.icons.lucide.Plus
import androidx.compose.foundation.layout.Box
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
    ToolKind.Laser -> Lucide.Sparkle
    ToolKind.Animated -> Lucide.Clapperboard
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
        // A side dock that no longer fits (short window) falls back to the top edge. The length
        // is computed from the content, not measured, so the choice cannot oscillate.
        val sideLength = with(density) { (TOOL_BUTTON * 10 + 24.dp * 3 + 36.dp).roundToPx() }
        val sideFits = area.height - topInsetPx - margin * 2 >= sideLength
        val effective = if ((dock == Dock.Left || dock == Dock.Right) && !sideFits) Dock.Top else dock
        LaunchedEffect(effective, size, area, topInsetPx) {
            if (size != IntSize.Zero && !dragging) {
                if (pos.value == Offset.Zero) pos.snapTo(target(effective)) else pos.animateTo(target(effective), Motion.bouncy())
            }
        }
        val vertical = effective == Dock.Left || effective == Dock.Right
        Card(
            Modifier
                .offset { IntOffset(pos.value.x.roundToInt(), pos.value.y.roundToInt()) }
                .onSizeChanged { size = it }
                .graphicsLayer { this.alpha = if (size == IntSize.Zero) 0f else alpha },
            shape = RoundedCornerShape(19.dp),
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
                Box(grip.size(if (vertical) 30.dp else 16.dp, if (vertical) 16.dp else 30.dp), contentAlignment = Alignment.Center) {
                    Icon(Lucide.GripVertical, Modifier.graphicsLayer { rotationZ = if (vertical) 90f else 0f }, tint = M.colors.textTertiary, size = 12.dp)
                }
                listOf(ToolKind.Pen, ToolKind.Pencil, ToolKind.Highlighter, ToolKind.Laser, ToolKind.Animated, ToolKind.Eraser, ToolKind.Lasso, ToolKind.Shape, ToolKind.Text).forEach { k ->
                    ToolButton(k, if (vertical) (if (effective == Dock.Right) PopoverSide.Start else PopoverSide.End) else if (effective == Dock.Bottom) PopoverSide.Above else PopoverSide.Below, tab)
                }
                ImageInsertButton(tab, doc)
                Spacer(Modifier.size(if (vertical) 0.dp else 4.dp, if (vertical) 4.dp else 0.dp))
                QuickColors(vertical)
            }
            if (vertical) Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
            else Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) { content() }
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
            size = TOOL_BUTTON,
            iconSize = TOOL_ICON,
            tint = if (selected && (Tools.inkTool(k) != null || k == ToolKind.Laser)) Color(Tools.colorOf(k) or 0xFF000000.toInt()) else null,
        )
        Popover(options, { options = false }, side = side, width = 300.dp) { ToolOptions(k) }
    }
}

@Composable
private fun ToolOptions(k: ToolKind) {
    val c = M.colors
    Column(Modifier.padding(10.dp)) {
        if (k == ToolKind.Eraser) {
            Segmented(EraserMode.entries, Tools.eraserMode, { Tools.eraserMode = it; Tools.save() }, Modifier.fillMaxWidth()) { o, sel ->
                Text(if (o == EraserMode.Whole) stringResource(Res.string.erase_whole) else stringResource(Res.string.erase_partial), style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary)
            }
            Spacer(Modifier.height(14.dp))
        }
        if (k == ToolKind.Shape) {
            Segmented(ShapeKind.entries, Tools.shapeKind, { Tools.shapeKind = it; Tools.save() }, Modifier.fillMaxWidth()) { o, sel ->
                Icon(
                    when (o) {
                        ShapeKind.Line -> Lucide.Minus; ShapeKind.Arrow -> Lucide.ArrowUpRight; ShapeKind.Rectangle -> Lucide.Square
                        ShapeKind.Ellipse -> Lucide.Circle; ShapeKind.Triangle -> Lucide.Triangle
                        ShapeKind.RoundedRectangle -> Lucide.Squircle
                    },
                    tint = if (sel) c.text else c.textSecondary, size = 17.dp,
                )
            }
            Spacer(Modifier.height(6.dp))
            ListRow(stringResource(Res.string.fill), Modifier.padding(horizontal = 0.dp)) { Toggle(Tools.shapeFill, { Tools.shapeFill = it }) }
        }
        if (k != ToolKind.Eraser && k != ToolKind.Lasso) {
            Text(stringResource(Res.string.color), style = M.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(8.dp))
            val palette = when (k) { ToolKind.Highlighter -> HIGHLIGHT_COLORS; ToolKind.Laser -> LASER_COLORS; else -> INK_COLORS }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                palette.forEach { col ->
                    ColorDot(Color(col or 0xFF000000.toInt()), Tools.colorOf(k) == col, { Tools.setColor(k, col) }, size = 32.dp)
                }
            }
            CustomColors(k)
            Spacer(Modifier.height(14.dp))
        }
        if (k == ToolKind.Animated) {
            MotionOptions()
            Spacer(Modifier.height(14.dp))
        }
        if (k == ToolKind.Laser) {
            Text(stringResource(Res.string.laser_hint), style = M.type.caption, color = c.textTertiary)
        } else if (k == ToolKind.Text) {
            Text(stringResource(Res.string.font_size), style = M.type.caption, color = c.textSecondary)
            Slider(Tools.textSize, { Tools.textSize = it }, Modifier.fillMaxWidth(), range = 8f..48f, onChangeFinished = { Tools.save() })
        } else if (k != ToolKind.Lasso) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.width), Modifier.weight(1f), style = M.type.caption, color = c.textSecondary)
                Text(((Tools.widthOf(k) * 10).roundToInt() / 10f).toString(), style = M.type.caption, color = c.textTertiary)
            }
            val defaults = Tools.widthsOf(k)
            // Three presets: tap to switch, then the slider fine-tunes the chosen one.
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tools.presetsOf(k).forEachIndexed { i, w ->
                    val active = Tools.activePreset[k] == i
                    val maxW = defaults.last() * 2f
                    Box(
                        Modifier.size(40.dp).pressable(pressedScale = 0.88f) { Tools.selectPreset(k, i) }.clip(CircleShape)
                            .background(if (active) c.accentSoft else c.fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size((4f + 20f * (w / maxW)).dp).clip(CircleShape).background(if (active) c.accent else c.text))
                    }
                }
            }
            Slider(Tools.widthOf(k), { Tools.setWidth(k, it) }, Modifier.fillMaxWidth(), range = defaults.first() * 0.4f..defaults.last() * 2f)
            if (Tools.inkTool(k) != null) {
                Spacer(Modifier.height(8.dp))
                StrokePreview(k)
            }
        }
    }
}

/**
 * The user's own colors. Tap to use, long-press to edit or delete, "+" to add a new one.
 * Highlighters apply their usual translucency to whatever color is picked.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomColors(k: ToolKind) {
    val c = M.colors
    // null = closed, -1 = adding a new color, otherwise the index being edited.
    var editing by remember { mutableStateOf<Int?>(null) }
    fun forTool(argb: Int) = if (k == ToolKind.Highlighter) (argb and 0x00FFFFFF) or 0x66000000 else argb or 0xFF000000.toInt()
    Spacer(Modifier.height(6.dp))
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Tools.customColors.forEachIndexed { i, col ->
            ColorDot(
                Color(col), editing == i || (editing == null && Tools.colorOf(k) == forTool(col)),
                { Tools.setColor(k, forTool(col)); if (editing != null) editing = i },
                size = 32.dp,
                onLongClick = { Tools.setColor(k, forTool(col)); editing = i },
            )
        }
        Box(
            Modifier.size(32.dp).pressable(pressedScale = 0.85f) { editing = if (editing == -1) null else -1 }.clip(CircleShape)
                .background(if (editing == -1) c.accentSoft else c.fill),
            contentAlignment = Alignment.Center,
        ) { Icon(Lucide.Plus, tint = if (editing == -1) c.accent else c.textSecondary, size = 16.dp) }
    }
    if (Tools.customColors.isNotEmpty() && editing == null) {
        Text(stringResource(Res.string.custom_color_hint), Modifier.padding(top = 4.dp), style = M.type.caption, color = c.textTertiary)
    }
    androidx.compose.animation.AnimatedVisibility(editing != null) {
        val target = editing ?: return@AnimatedVisibility
        val original = remember(target) { Tools.colorOf(k) }
        key(target) {
            Column(Modifier.padding(top = 10.dp)) {
                Text(if (target == -1) stringResource(Res.string.new_color) else stringResource(Res.string.edit_color), style = M.type.calloutStrong, color = c.textSecondary)
                Spacer(Modifier.height(8.dp))
                val start = if (target >= 0) Tools.customColors.getOrNull(target) ?: original else original or 0xFF000000.toInt()
                var chosen by remember { mutableStateOf(start) }
                ColorPicker(start, { chosen = it; Tools.setColor(k, forTool(it)) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (target >= 0) {
                        com.mheenotes.ui.design.Button(stringResource(Res.string.delete), {
                            Tools.removeCustomColor(target); Tools.setColor(k, original); editing = null
                        }, style = com.mheenotes.ui.design.ButtonStyle.Danger, compact = true)
                    } else {
                        com.mheenotes.ui.design.Button(stringResource(Res.string.cancel), { Tools.setColor(k, original); editing = null }, compact = true)
                    }
                    com.mheenotes.ui.design.Button(stringResource(Res.string.done), {
                        if (target >= 0) Tools.updateCustomColor(target, chosen) else Tools.addCustomColor(chosen)
                        editing = null
                    }, Modifier.weight(1f), style = com.mheenotes.ui.design.ButtonStyle.Primary, compact = true)
                }
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
    // The animated pen previews its motion live.
    val s0 = if (tool == com.mheenotes.core.model.Tool.Animated) androidx.compose.runtime.produceState(0L) {
        while (true) androidx.compose.runtime.withFrameMillis { value = it }
    } else null
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
        val s = com.mheenotes.core.model.Stroke("p", 0, tool, color, width, pts, if (s0 != null) Tools.inkMotion.pack() else 0)
        scale(2.2f, 2.2f, Offset.Zero) {
            if (s0 != null) drawAnimatedStroke(s, s0.value)
            else with(com.mheenotes.ui.render.ItemPainter(null, null)) { draw(listOf(s), 2.2f) }
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
    val enabled = Tools.inkTool(k) != null || k == ToolKind.Laser || k == ToolKind.Shape || k == ToolKind.Text
    val palette = when (k) { ToolKind.Highlighter -> HIGHLIGHT_COLORS; ToolKind.Laser -> LASER_COLORS; else -> INK_COLORS }.take(3)
    val alpha by animateFloatAsState(if (enabled) 1f else 0.25f, Motion.fade(), label = "qc")
    val content: @Composable () -> Unit = {
        palette.forEach { col ->
            ColorDot(Color(col or 0xFF000000.toInt()), enabled && Tools.colorOf(k) == col, { if (enabled) Tools.setColor(k, col) }, size = 24.dp)
        }
    }
    val m = Modifier.graphicsLayer { this.alpha = alpha }
    if (vertical) Column(m) { content() } else Row(m) { content() }
}

/** Motion, speed, color effect and strength for the animated pen. */
@Composable
private fun MotionOptions() {
    val c = M.colors
    val m = Tools.inkMotion
    fun set(v: com.mheenotes.core.model.InkMotion) { Tools.inkMotion = v; Tools.save() }
    @Composable
    fun Choice(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        Text(label, style = M.type.caption, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        Segmented(options.indices.toList(), selected, onSelect, Modifier.fillMaxWidth()) { i, sel ->
            Text(options[i], style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
    }
    Choice(
        stringResource(Res.string.motion),
        listOf(stringResource(Res.string.motion_boil), stringResource(Res.string.motion_wave), stringResource(Res.string.motion_bounce)),
        m.motion,
    ) { set(m.copy(motion = it)) }
    Choice(
        stringResource(Res.string.speed),
        listOf(stringResource(Res.string.speed_slow), stringResource(Res.string.speed_normal), stringResource(Res.string.speed_fast)),
        m.speed.coerceAtMost(2),
    ) { set(m.copy(speed = it)) }
    Choice(
        stringResource(Res.string.color_effect),
        listOf(stringResource(Res.string.effect_solid), stringResource(Res.string.effect_rainbow), stringResource(Res.string.effect_flicker)),
        m.color,
    ) { set(m.copy(color = it)) }
    Choice(
        stringResource(Res.string.strength),
        listOf(stringResource(Res.string.strength_subtle), stringResource(Res.string.strength_medium), stringResource(Res.string.strength_wild)),
        m.strength.coerceAtMost(2),
    ) { set(m.copy(strength = it)) }
}

/** Toolbar button and icon sizes (30% smaller than the first version). */
val TOOL_BUTTON = 32.dp
val TOOL_ICON = 16.dp

@Composable
expect fun ImageInsertButton(tab: EditorTab, doc: NotebookDocument)
