package com.mheenotes.ui.editor

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import com.composables.icons.lucide.Download
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.LocalOverlays
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.ModalActions
import com.mheenotes.ui.design.ModalTitle
import com.mheenotes.ui.design.Segmented
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.TextField
import com.mheenotes.core.doc.parsePageRange
import kotlinx.coroutines.launch

enum class ExportKind { Pdf, Png, Notebook }

enum class PageRange { All, Current, Custom }

@Composable
fun ExportModal(tab: EditorTab, visible: Boolean, onDismiss: () -> Unit) {
    val c = M.colors
    val scope = rememberCoroutineScope()
    val overlays = LocalOverlays.current
    val pages by (tab.doc?.pages ?: return).collectAsState()
    var kind by remember { mutableStateOf(ExportKind.Pdf) }
    var range by remember { mutableStateOf(PageRange.All) }
    var custom by remember(visible) { mutableStateOf("1-${pages.size}") }
    var fileName by remember(visible) { mutableStateOf(com.mheenotes.ui.AppGraph.library.notebook(tab.notebookId)?.title.orEmpty()) }
    val selected: List<Int>? = when {
        kind == ExportKind.Notebook || range == PageRange.All -> pages.indices.toList()
        range == PageRange.Current -> listOf(tab.currentPage.coerceIn(0, pages.lastIndex))
        else -> parsePageRange(custom, pages.size)
    }
    val savedText = stringResource(Res.string.saved_to_files)
    val failedText = stringResource(Res.string.export_failed)
    val exportingText = stringResource(Res.string.exporting)
    val saver = rememberExportSaver { ok -> overlays.toast(if (ok) savedText else failedText) }
    fun run(save: Boolean) {
        val chosen = selected ?: return
        onDismiss()
        tab.canvas?.commitSelection()
        overlays.toast(exportingText)
        scope.launch {
            val result = runCatching { Exporter.export(tab, kind, chosen, fileName.trim()) }.getOrElse { it.printStackTrace(); null }
            when {
                result == null -> overlays.toast(failedText)
                save -> saver.save(result)
                else -> Exporter.share(result)
            }
        }
    }
    Modal(visible, onDismiss, width = 480.dp) {
        ModalTitle(stringResource(Res.string.export))
        Text(stringResource(Res.string.file_name), style = M.type.calloutStrong, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        TextField(fileName, { fileName = it.take(80) }, Modifier.fillMaxWidth(), placeholder = stringResource(Res.string.untitled), clearable = true)
        Spacer(Modifier.height(16.dp))
        Segmented(ExportKind.entries, kind, { kind = it }, Modifier.fillMaxWidth()) { o, sel ->
            Text(
                when (o) { ExportKind.Pdf -> "PDF"; ExportKind.Png -> stringResource(Res.string.image_format); ExportKind.Notebook -> stringResource(Res.string.export_notebook) },
                style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary, maxLines = 1,
            )
        }
        AnimatedVisibility(kind != ExportKind.Notebook) {
            Column {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(Res.string.pages_), style = M.type.calloutStrong, color = c.textSecondary)
                Spacer(Modifier.height(8.dp))
                Segmented(PageRange.entries, range, { range = it }, Modifier.fillMaxWidth()) { o, sel ->
                    Text(
                        when (o) { PageRange.All -> stringResource(Res.string.export_all); PageRange.Current -> stringResource(Res.string.export_current); PageRange.Custom -> stringResource(Res.string.export_range) },
                        style = M.type.calloutStrong, color = if (sel) c.text else c.textSecondary, maxLines = 1,
                    )
                }
                AnimatedVisibility(range == PageRange.Custom) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        TextField(custom, { custom = it }, Modifier.fillMaxWidth(), placeholder = "1-3, 5")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (selected == null) stringResource(Res.string.range_invalid) else pluralStringResource(Res.plurals.pages, selected.size, selected.size),
                            style = M.type.caption, color = if (selected == null) c.danger else c.textTertiary,
                        )
                    }
                }
            }
        }
        ModalActions {
            Button(stringResource(Res.string.cancel), onDismiss)
            Button(stringResource(Res.string.save_to_files), { run(save = true) }, icon = Lucide.Download, enabled = selected != null)
            Button(stringResource(Res.string.share), { run(save = false) }, icon = Lucide.Share2, style = ButtonStyle.Primary, enabled = selected != null)
        }
    }
}

/** Files produced by an export (one, or one per page for images). */
class ExportResult(val paths: List<String>, val mime: String)

/** Writes export files and shares them. Platform-provided. */
expect object Exporter {
    suspend fun export(tab: EditorTab, kind: ExportKind, pages: List<Int>, name: String): ExportResult?
    fun share(result: ExportResult)
}

interface ExportSaver {
    fun save(result: ExportResult)
}

@Composable
expect fun rememberExportSaver(onDone: (Boolean) -> Unit): ExportSaver
