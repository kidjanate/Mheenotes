package com.mheenotes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bold
import com.composables.icons.lucide.Italic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.TextAlignCenter
import com.composables.icons.lucide.TextAlignEnd
import com.composables.icons.lucide.TextAlignStart
import com.composables.icons.lucide.Underline
import com.mheenotes.core.model.FontKind
import com.mheenotes.core.model.TextAlign
import com.mheenotes.core.model.TextItem
import com.mheenotes.ui.design.ColorDot
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Segmented
import com.mheenotes.ui.design.Text
import kotlin.math.roundToInt

/**
 * Bold/italic/underline, alignment, font, size and color for a text box. Works on a style
 * prototype so the same controls serve live editing and restyling a selection.
 */
@Composable
fun TextStyleControls(style: TextItem, onChange: (TextItem) -> Unit, modifier: Modifier = Modifier) {
    val c = M.colors
    Column(modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(Lucide.Bold, { onChange(style.copy(bold = !style.bold)) }, selected = style.bold, size = 36.dp, iconSize = 17.dp)
            IconButton(Lucide.Italic, { onChange(style.copy(italic = !style.italic)) }, selected = style.italic, size = 36.dp, iconSize = 17.dp)
            IconButton(Lucide.Underline, { onChange(style.copy(underline = !style.underline)) }, selected = style.underline, size = 36.dp, iconSize = 17.dp)
            Spacer(Modifier.width(8.dp))
            IconButton(Lucide.TextAlignStart, { onChange(style.copy(align = TextAlign.Start)) }, selected = style.align == TextAlign.Start, size = 36.dp, iconSize = 17.dp)
            IconButton(Lucide.TextAlignCenter, { onChange(style.copy(align = TextAlign.Center)) }, selected = style.align == TextAlign.Center, size = 36.dp, iconSize = 17.dp)
            IconButton(Lucide.TextAlignEnd, { onChange(style.copy(align = TextAlign.End)) }, selected = style.align == TextAlign.End, size = 36.dp, iconSize = 17.dp)
            Spacer(Modifier.width(8.dp))
            IconButton(Lucide.Minus, { onChange(style.copy(fontSize = (style.fontSize - 2f).coerceAtLeast(6f))) }, size = 32.dp, iconSize = 15.dp)
            Text(style.fontSize.roundToInt().toString(), Modifier.width(28.dp), style = M.type.calloutStrong, color = c.textSecondary)
            IconButton(Lucide.Plus, { onChange(style.copy(fontSize = (style.fontSize + 2f).coerceAtMost(144f))) }, size = 32.dp, iconSize = 15.dp)
        }
        Spacer(Modifier.height(8.dp))
        Segmented(FontKind.entries, style.font, { onChange(style.copy(font = it)) }, Modifier.fillMaxWidth()) { o, sel ->
            Text(fontLabel(o), style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (INK_COLORS + Tools.customColors).distinct().take(10).forEach { col ->
                ColorDot(Color(col or 0xFF000000.toInt()), style.argb == col, { onChange(style.copy(argb = col)) }, size = 28.dp)
            }
        }
    }
}
