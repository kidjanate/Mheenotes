package com.mheenotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.TextItem
import com.mheenotes.core.model.newId
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.PlatformBackHandler
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Live text box over the canvas, matched to page zoom. Committing measures the text in page
 * units so the stored frame is exact at every zoom.
 */
@Composable
fun TextEditorOverlay(tab: EditorTab, doc: NotebookDocument) {
    val edit = tab.textEdit ?: return
    val existing = edit.itemId?.let { id -> doc.content(edit.pageId)?.get(id) as? TextItem }
    var value by remember(edit) { mutableStateOf(edit.initial) }
    val focus = remember(edit) { FocusRequester() }
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val pageMeasurer = remember(resolver) { TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr, 4) }
    val fonts = M.fonts
    val fontSize = existing?.fontSize ?: Tools.textSize
    val argb = existing?.argb ?: Tools.textColor
    val bold = existing?.bold ?: false
    var hadFocus by remember(edit) { mutableStateOf(false) }

    fun commit() {
        if (tab.textEdit != edit) return
        tab.textEdit = null
        val text = value.trimEnd()
        if (existing != null) {
            if (text == existing.text) { doc.preview(edit.pageId, emptyList(), listOf(existing)); return }
            if (text.isEmpty()) { doc.commit(edit.pageId, listOf(existing), emptyList()); return }
            val h = measure(pageMeasurer, text, fontSize, edit.width, fonts.forWeight(if (bold) FontWeight.Bold else FontWeight.Normal))
            val updated = existing.copy(text = text, frame = Rect(edit.x, edit.y, edit.x + edit.width, edit.y + h))
            doc.preview(edit.pageId, emptyList(), listOf(updated))
            doc.commit(edit.pageId, listOf(existing), listOf(updated))
        } else if (text.isNotEmpty()) {
            val h = measure(pageMeasurer, text, fontSize, edit.width, fonts.regular)
            val item = TextItem(newId(), doc.nextSeq(), text, Rect(edit.x, edit.y, edit.x + edit.width, edit.y + h), fontSize, argb)
            doc.edit(edit.pageId, emptyList(), listOf(item))
        }
    }

    // Existing text is lifted off the page while it is edited.
    LaunchedEffect(edit) {
        existing?.let { doc.preview(edit.pageId, listOf(it), emptyList()) }
        focus.requestFocus()
    }
    PlatformBackHandler(true) { commit() }
    androidx.compose.runtime.SideEffect { tab.finishText = ::commit }

    val v = tab.viewport
    Box(Modifier.fillMaxSize()) {
        val scale = v.scale
        val px = with(density) { (fontSize * scale).toSp() }
        BasicTextField(
            value, { value = it },
            textStyle = TextStyle(
                color = Color(argb), fontSize = px, fontFamily = if (bold) fonts.bold else fonts.regular,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            ),
            cursorBrush = SolidColor(M.colors.accent),
            modifier = Modifier
                .offset { IntOffset(v.screenX(edit.pageId, edit.x).roundToInt(), v.screenY(edit.pageId, edit.y).roundToInt()) }
                .width(with(density) { (edit.width * scale).toDp() })
                .border(1.5.dp, M.colors.accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .background(Color(0x08000000))
                .focusRequester(focus)
                .onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) commit() },
        )
    }
}

private fun measure(m: TextMeasurer, text: String, fontSize: Float, width: Float, family: androidx.compose.ui.text.font.FontFamily): Float {
    val layout = m.measure(text, TextStyle(fontSize = fontSize.sp, fontFamily = family), constraints = Constraints(maxWidth = max(1, width.toInt())))
    return max(layout.size.height.toFloat(), fontSize * 1.3f)
}
