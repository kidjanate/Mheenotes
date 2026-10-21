package com.mheenotes.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.composables.icons.lucide.FileImage
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.mheenotes.ui.design.LocalOverlays
import com.mheenotes.ui.design.MenuItem
import com.mheenotes.ui.design.MenuLabel
import com.mheenotes.ui.design.Popover
import com.mheenotes.ui.i18n.S
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

enum class ExportKind { Pdf, PagePng, Notebook }

@Composable
fun ExportPopover(tab: EditorTab, expanded: Boolean, onDismiss: () -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    val overlays = LocalOverlays.current
    fun run(kind: ExportKind) {
        onDismiss()
        tab.canvas?.commitSelection()
        overlays.toast(s.exporting)
        scope.launch {
            val ok = runCatching { Exporter.exportAndShare(tab, kind) }.getOrElse { it.printStackTrace(); false }
            if (!ok) overlays.toast(s.exportFailed)
        }
    }
    Popover(expanded, onDismiss, align = Alignment.End) {
        MenuLabel(s.export)
        MenuItem(s.exportPdf, icon = Lucide.FileText) { run(ExportKind.Pdf) }
        MenuItem(s.exportPng, icon = Lucide.FileImage) { run(ExportKind.PagePng) }
        MenuItem(s.exportNotebook, icon = Lucide.NotebookPen) { run(ExportKind.Notebook) }
    }
}

/** Writes an export file and opens the system share sheet. Platform-provided. */
expect object Exporter {
    suspend fun exportAndShare(tab: EditorTab, kind: ExportKind): Boolean
}
