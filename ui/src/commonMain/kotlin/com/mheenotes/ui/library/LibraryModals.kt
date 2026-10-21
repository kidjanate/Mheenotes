package com.mheenotes.ui.library

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.FileText
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.mheenotes.core.model.Folder
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.PAPER_WHITE
import com.mheenotes.core.model.PageSize
import com.mheenotes.core.model.Template
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.ColorDot
import com.mheenotes.ui.design.Divider
import com.mheenotes.ui.design.ListRow
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.ModalActions
import com.mheenotes.ui.design.ModalTitle
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Segmented
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.TextField
import com.mheenotes.ui.design.ThemeMode
import com.mheenotes.ui.design.Toggle
import com.mheenotes.ui.design.pressable
import com.mheenotes.ui.render.drawTemplate

val PAPER_COLORS = listOf(PAPER_WHITE, 0xFFFBF6E9.toInt(), 0xFFEFF3F8.toInt(), 0xFF26272B.toInt())

@Composable
fun NewNotebookModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Template, PageSize, Int) -> Unit,
) {
    var title by remember(visible) { mutableStateOf("") }
    var template by remember { mutableStateOf(Template.Lined) }
    var size by remember { mutableStateOf(PageSize.A4) }
    var paper by remember { mutableStateOf(PAPER_WHITE) }
    Modal(visible, onDismiss, width = 600.dp) {
        ModalTitle(stringResource(Res.string.new_notebook))
        val focus = remember { FocusRequester() }
        TextField(title, { title = it }, Modifier.fillMaxWidth().focusRequester(focus), placeholder = stringResource(Res.string.notebook_name), onSubmit = { onCreate(title, template, size, paper) })
        LaunchedEffect(visible) { if (visible) runCatching { focus.requestFocus() } }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(Res.string.template), style = M.type.calloutStrong, color = M.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Template.entries.forEach { t ->
                TemplateOption(t, paper, t == template, Modifier.weight(1f)) { template = t }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.paper), style = M.type.calloutStrong, color = M.colors.textSecondary)
                Spacer(Modifier.height(10.dp))
                val sizes = listOf(PageSize.A4, PageSize.Letter, PageSize.A5, PageSize.Square)
                Segmented(sizes, size, { size = it }, Modifier.fillMaxWidth()) { o, sel ->
                    Text(
                        when (o) { PageSize.A4 -> stringResource(Res.string.size_a4); PageSize.Letter -> stringResource(Res.string.size_letter); PageSize.A5 -> stringResource(Res.string.size_a5); else -> stringResource(Res.string.size_square) },
                        style = M.type.calloutStrong, color = if (sel) M.colors.text else M.colors.textSecondary, maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(24.dp))
            Column {
                Text(stringResource(Res.string.paper_color), style = M.type.calloutStrong, color = M.colors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PAPER_COLORS.forEach { p -> ColorDot(Color(p), p == paper, { paper = p }) }
                }
            }
        }
        ModalActions {
            Button(stringResource(Res.string.cancel), onDismiss)
            Button(stringResource(Res.string.create), { onCreate(title, template, size, paper) }, style = ButtonStyle.Primary)
        }
    }
}

@Composable
private fun TemplateOption(t: Template, paper: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = M.colors
    val border by animateDpAsState(if (selected) 2.5.dp else 0.75.dp, Motion.snappy(), label = "b")
    Column(modifier.pressable(pressedScale = 0.94f, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(Radii.sm))
                .background(Color(paper))
                .border(border, if (selected) c.accent else c.separator, RoundedCornerShape(Radii.sm)),
        ) {
            val k = size.width / 300f
            scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                drawTemplate(t, 300f, 300f / 0.72f, paper)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            com.mheenotes.ui.editor.templateLabel(t),
            style = M.type.caption, color = if (selected) c.accent else c.textSecondary,
        )
    }
}

@Composable
fun NameModal(
    visible: Boolean,
    title: String,
    placeholder: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(visible, initial) { mutableStateOf(initial) }
    Modal(visible, onDismiss, width = 420.dp) {
        ModalTitle(title)
        val focus = remember { FocusRequester() }
        TextField(value, { value = it }, Modifier.fillMaxWidth().focusRequester(focus), placeholder = placeholder, clearable = true, onSubmit = { onConfirm(value) })
        LaunchedEffect(visible) { if (visible) runCatching { focus.requestFocus() } }
        ModalActions {
            Button(stringResource(Res.string.cancel), onDismiss)
            Button(confirm, { onConfirm(value) }, style = ButtonStyle.Primary)
        }
    }
}

@Composable
fun MoveModal(targets: Set<Id>?, folders: List<Folder>, onDismiss: () -> Unit, onMove: (Id?) -> Unit) {
    val c = M.colors
    Modal(targets != null, onDismiss, width = 420.dp) {
        ModalTitle(stringResource(Res.string.move_to))
        Card(Modifier.fillMaxWidth().heightIn(max = 380.dp), color = c.fill, border = false, shape = RoundedCornerShape(Radii.md)) {
            LazyColumn {
                item { ListRow(stringResource(Res.string.top_level), icon = Lucide.House, onClick = { onMove(null) }) }
                val byParent = folders.groupBy { it.parentId }
                val rows = ArrayList<Pair<Folder, Int>>()
                fun walk(p: Id?, d: Int) { byParent[p].orEmpty().sortedBy { it.name.lowercase() }.forEach { rows += it to d; walk(it.id, d + 1) } }
                walk(null, 0)
                items(rows, key = { it.first.id }) { (f, d) ->
                    Divider(Modifier.padding(start = 48.dp))
                    ListRow(f.name, Modifier.padding(start = (d * 18).dp), icon = Lucide.Folder, onClick = { onMove(f.id) })
                }
            }
        }
        ModalActions { Button(stringResource(Res.string.cancel), onDismiss) }
    }
}

@Composable
fun ThemePicker(modifier: Modifier = Modifier) {
    val c = M.colors
    val st = AppGraph.settings
    Segmented(ThemeMode.entries, st.theme, st::updateTheme, modifier) { o, sel ->
        Text(
            when (o) { ThemeMode.System -> stringResource(Res.string.theme_system); ThemeMode.Light -> stringResource(Res.string.theme_light); ThemeMode.Dark -> stringResource(Res.string.theme_dark) },
            style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary,
        )
    }
}

@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val c = M.colors
    val st = AppGraph.settings
    Segmented(com.mheenotes.ui.AppLanguage.entries, st.language, st::updateLanguage, modifier) { o, sel ->
        Text(
            when (o) {
                com.mheenotes.ui.AppLanguage.System -> stringResource(Res.string.theme_system)
                com.mheenotes.ui.AppLanguage.English -> "English"
                com.mheenotes.ui.AppLanguage.Thai -> "ไทย"
            },
            style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary,
        )
    }
}

@Composable
fun SettingsModal(visible: Boolean, onDismiss: () -> Unit, onTour: () -> Unit = {}) {
    var showLicenses by remember { mutableStateOf(false) }
    val c = M.colors
    val st = AppGraph.settings
    Modal(visible, onDismiss, width = 520.dp) {
        ModalTitle(stringResource(Res.string.settings))
        Text(stringResource(Res.string.appearance), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(10.dp))
        ThemePicker(Modifier.fillMaxWidth())
        Spacer(Modifier.height(22.dp))
        Text(stringResource(Res.string.input), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), color = c.fill, border = false, shape = RoundedCornerShape(Radii.md)) {
            Column {
                ListRow(stringResource(Res.string.finger_draws)) { Toggle(st.fingerDraws, st::updateFingerDraws) }
                Divider(Modifier.padding(start = 16.dp))
                ListRow(stringResource(Res.string.scribble_erase)) { Toggle(st.scribbleErase, st::updateScribble) }
                Divider(Modifier.padding(start = 16.dp))
                ListRow(stringResource(Res.string.draw_hold_shapes)) { Toggle(st.holdToShape, st::updateHoldShape) }
                Divider(Modifier.padding(start = 16.dp))
                ListRow(stringResource(Res.string.low_latency), subtitle = stringResource(Res.string.low_latency_hint)) { Toggle(st.lowLatencyInk, st::updateLowLatency) }
                Divider(Modifier.padding(start = 16.dp))
                ListRow(stringResource(Res.string.pressure)) { Toggle(st.pressure, st::updatePressure) }
                Divider(Modifier.padding(start = 16.dp))
                ListRow(stringResource(Res.string.ask_insert_page)) { Toggle(st.askInsertPage, st::updateAskInsert) }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(Res.string.language), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(10.dp))
        LanguagePicker(Modifier.fillMaxWidth())
        Spacer(Modifier.height(22.dp))
        Text(stringResource(Res.string.performance), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), color = c.fill, border = false, shape = RoundedCornerShape(Radii.md)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(Res.string.render_memory), style = M.type.body)
                Text(stringResource(Res.string.render_memory_hint), style = M.type.callout, color = c.textSecondary)
                Spacer(Modifier.height(12.dp))
                Segmented(com.mheenotes.ui.RenderMemory.entries, st.renderMemory, st::updateRenderMemory, Modifier.fillMaxWidth()) { o, sel ->
                    val name = when (o) {
                        com.mheenotes.ui.RenderMemory.Low -> stringResource(Res.string.render_memory_low)
                        com.mheenotes.ui.RenderMemory.Balanced -> stringResource(Res.string.render_memory_balanced)
                        com.mheenotes.ui.RenderMemory.High -> stringResource(Res.string.render_memory_high)
                    }
                    Text("$name · ${o.megabytes} MB", style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(Res.string.about), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), color = c.fill, border = false, shape = RoundedCornerShape(Radii.md)) {
            Column {
                ListRow(stringResource(Res.string.tour), icon = Lucide.Sparkles, onClick = onTour) {
                    com.mheenotes.ui.design.Icon(Lucide.ChevronRight, tint = c.textTertiary, size = 16.dp)
                }
                Divider(Modifier.padding(start = 48.dp))
                ListRow(stringResource(Res.string.licenses), icon = Lucide.FileText, onClick = { showLicenses = true }) {
                    com.mheenotes.ui.design.Icon(Lucide.ChevronRight, tint = c.textTertiary, size = 16.dp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(Res.string.made_with), Modifier.fillMaxWidth(), style = M.type.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center), color = c.textTertiary)
        ModalActions { Button(stringResource(Res.string.done), onDismiss, style = ButtonStyle.Primary) }
    }
    com.mheenotes.ui.settings.LicensesModal(showLicenses) { showLicenses = false }
}
