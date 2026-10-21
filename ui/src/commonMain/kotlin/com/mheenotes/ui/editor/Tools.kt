package com.mheenotes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.Tool
import com.mheenotes.ui.AppGraph

enum class ToolKind { Pen, Pencil, Highlighter, Eraser, Lasso, Text, Shape, Hand }

enum class EraserMode { Whole, Partial }

val INK_COLORS = listOf(
    0xFF1C1C1F, 0xFF3A63F0, 0xFFE5484D, 0xFF2FA36B, 0xFFE79A2C, 0xFF8E4EC6, 0xFF0B8A9A, 0xFF8A5A3C,
).map { it.toInt() }

val HIGHLIGHT_COLORS = listOf(0x66FFE14D, 0x667CE38B, 0x6669B7FF, 0x66FF8FB1, 0x66FFB062).map { it.toInt() }

/** Width presets in page units (points). */
val PEN_WIDTHS = listOf(1.0f, 1.8f, 3.0f)
val PENCIL_WIDTHS = listOf(0.9f, 1.5f, 2.6f)
val HIGHLIGHT_WIDTHS = listOf(10f, 16f, 24f)
val ERASER_WIDTHS = listOf(6f, 14f, 30f)

/**
 * Current tool and per-tool settings, shared by every tab. Persisted as a compact string so
 * adding fields never needs a schema change.
 */
object Tools {
    var current by mutableStateOf(ToolKind.Pen)
    /** Tool to return to after a temporary switch (e.g. stylus eraser end). */
    var previousInk by mutableStateOf(ToolKind.Pen)

    var penColor by mutableIntStateOf(INK_COLORS[0])
    var pencilColor by mutableIntStateOf(0xFF4A4A52.toInt())
    var highlighterColor by mutableIntStateOf(HIGHLIGHT_COLORS[0])
    var shapeColor by mutableIntStateOf(INK_COLORS[1])
    var textColor by mutableIntStateOf(INK_COLORS[0])

    var penWidth by mutableFloatStateOf(PEN_WIDTHS[1])
    var pencilWidth by mutableFloatStateOf(PENCIL_WIDTHS[1])
    var highlighterWidth by mutableFloatStateOf(HIGHLIGHT_WIDTHS[1])
    var eraserWidth by mutableFloatStateOf(ERASER_WIDTHS[1])
    var shapeWidth by mutableFloatStateOf(PEN_WIDTHS[1])
    var textSize by mutableFloatStateOf(16f)
    /** Style for new text boxes; the last style used becomes the next default. */
    var textStyle by mutableStateOf(
        com.mheenotes.core.model.TextItem("", 0, "", com.mheenotes.core.geometry.Rect.Empty, 16f, INK_COLORS[0]),
    )

    /** User-made colors, most recent first. */
    val customColors = androidx.compose.runtime.mutableStateListOf<Int>()

    fun addCustomColor(argb: Int) {
        val opaque = argb or 0xFF000000.toInt()
        customColors.remove(opaque)
        customColors.add(0, opaque)
        while (customColors.size > 8) customColors.removeAt(customColors.lastIndex)
        save()
    }

    fun updateCustomColor(index: Int, argb: Int) {
        if (index !in customColors.indices) return
        customColors[index] = argb or 0xFF000000.toInt()
        save()
    }

    fun removeCustomColor(index: Int) {
        if (index !in customColors.indices) return
        customColors.removeAt(index)
        save()
    }

    var eraserMode by mutableStateOf(EraserMode.Whole)
    var shapeKind by mutableStateOf(ShapeKind.Rectangle)
    var shapeFill by mutableStateOf(false)

    val isInk: Boolean get() = current == ToolKind.Pen || current == ToolKind.Pencil || current == ToolKind.Highlighter

    fun inkTool(kind: ToolKind = current): Tool? = when (kind) {
        ToolKind.Pen -> Tool.Pen
        ToolKind.Pencil -> Tool.Pencil
        ToolKind.Highlighter -> Tool.Highlighter
        else -> null
    }

    fun colorOf(kind: ToolKind): Int = when (kind) {
        ToolKind.Pen -> penColor
        ToolKind.Pencil -> pencilColor
        ToolKind.Highlighter -> highlighterColor
        ToolKind.Shape -> shapeColor
        ToolKind.Text -> textColor
        else -> penColor
    }

    fun setColor(kind: ToolKind, argb: Int) {
        when (kind) {
            ToolKind.Pen -> penColor = argb
            ToolKind.Pencil -> pencilColor = argb
            ToolKind.Highlighter -> highlighterColor = argb
            ToolKind.Shape -> shapeColor = argb
            ToolKind.Text -> textColor = argb
            else -> return
        }
        save()
    }

    fun widthOf(kind: ToolKind): Float = when (kind) {
        ToolKind.Pen -> penWidth
        ToolKind.Pencil -> pencilWidth
        ToolKind.Highlighter -> highlighterWidth
        ToolKind.Eraser -> eraserWidth
        ToolKind.Shape -> shapeWidth
        else -> penWidth
    }

    /** Three width presets per tool; the slider edits whichever preset is active. */
    val widthPresets = androidx.compose.runtime.mutableStateMapOf<ToolKind, List<Float>>()
    val activePreset = androidx.compose.runtime.mutableStateMapOf<ToolKind, Int>()

    fun presetsOf(kind: ToolKind): List<Float> = widthPresets[kind] ?: widthsOf(kind)

    fun selectPreset(kind: ToolKind, i: Int) {
        activePreset[kind] = i
        setWidth(kind, presetsOf(kind)[i])
    }

    fun setWidth(kind: ToolKind, w: Float) {
        activePreset[kind]?.let { i ->
            widthPresets[kind] = presetsOf(kind).toMutableList().also { it[i] = w }
        }
        when (kind) {
            ToolKind.Pen -> penWidth = w
            ToolKind.Pencil -> pencilWidth = w
            ToolKind.Highlighter -> highlighterWidth = w
            ToolKind.Eraser -> eraserWidth = w
            ToolKind.Shape -> shapeWidth = w
            else -> return
        }
        save()
    }

    fun widthsOf(kind: ToolKind): List<Float> = when (kind) {
        ToolKind.Pencil -> PENCIL_WIDTHS
        ToolKind.Highlighter -> HIGHLIGHT_WIDTHS
        ToolKind.Eraser -> ERASER_WIDTHS
        else -> PEN_WIDTHS
    }

    fun select(kind: ToolKind) {
        if (kind == ToolKind.Pen || kind == ToolKind.Pencil || kind == ToolKind.Highlighter) previousInk = kind
        current = kind
        save()
    }

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        val m = AppGraph.settings.toolPresets.split(';').mapNotNull { kv ->
            val i = kv.indexOf('=')
            if (i <= 0) null else kv.substring(0, i) to kv.substring(i + 1)
        }.toMap()
        fun i(k: String, set: (Int) -> Unit) = m[k]?.toLongOrNull()?.let { set(it.toInt()) }
        fun f(k: String, set: (Float) -> Unit) = m[k]?.toFloatOrNull()?.let(set)
        m["tool"]?.let { v -> ToolKind.entries.firstOrNull { it.name == v }?.let { current = it } }
        i("pc") { penColor = it }; i("nc") { pencilColor = it }; i("hc") { highlighterColor = it }
        i("sc") { shapeColor = it }; i("tc") { textColor = it }
        f("pw") { penWidth = it }; f("nw") { pencilWidth = it }; f("hw") { highlighterWidth = it }
        f("ew") { eraserWidth = it }; f("sw") { shapeWidth = it }; f("ts") { textSize = it }
        m["em"]?.let { v -> EraserMode.entries.firstOrNull { it.name == v }?.let { eraserMode = it } }
        m["sk"]?.let { v -> ShapeKind.entries.firstOrNull { it.name == v }?.let { shapeKind = it } }
        for (k in listOf(ToolKind.Pen, ToolKind.Pencil, ToolKind.Highlighter, ToolKind.Eraser, ToolKind.Shape)) {
            m["wp${k.ordinal}"]?.split(',')?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 3 }?.let { widthPresets[k] = it }
            m["ap${k.ordinal}"]?.toIntOrNull()?.takeIf { it in 0..2 }?.let { activePreset[k] = it }
        }
        m["tst"]?.split(',')?.let { p ->
            if (p.size == 5) textStyle = textStyle.copy(
                bold = p[0] == "1", italic = p[1] == "1", underline = p[2] == "1",
                align = com.mheenotes.core.model.TextAlign.entries.getOrElse(p[3].toIntOrNull() ?: 0) { com.mheenotes.core.model.TextAlign.Start },
                font = com.mheenotes.core.model.FontKind.entries.getOrElse(p[4].toIntOrNull() ?: 0) { com.mheenotes.core.model.FontKind.Sans },
            )
        }
        m["cc"]?.split(',')?.mapNotNull { it.toLongOrNull()?.toInt() }?.let { customColors.clear(); customColors.addAll(it) }
        if (current == ToolKind.Hand) current = ToolKind.Pen
    }

    fun save() {
        val s = buildString {
            append("tool=${current.name};pc=${penColor.toLong() and 0xffffffffL};nc=${pencilColor.toLong() and 0xffffffffL};")
            append("hc=${highlighterColor.toLong() and 0xffffffffL};sc=${shapeColor.toLong() and 0xffffffffL};tc=${textColor.toLong() and 0xffffffffL};")
            append("pw=$penWidth;nw=$pencilWidth;hw=$highlighterWidth;ew=$eraserWidth;sw=$shapeWidth;ts=$textSize;")
            append("em=${eraserMode.name};sk=${shapeKind.name};")
            append("cc=" + customColors.joinToString(",") { (it.toLong() and 0xffffffffL).toString() } + ";")
            val t = textStyle
            fun b(v: Boolean) = if (v) "1" else "0"
            append("tst=${b(t.bold)},${b(t.italic)},${b(t.underline)},${t.align.ordinal},${t.font.ordinal}")
            for ((k, v) in widthPresets) append(";wp${k.ordinal}=" + v.joinToString(","))
            for ((k, v) in activePreset) append(";ap${k.ordinal}=$v")
        }
        AppGraph.settings.updateTools(s)
    }
}
