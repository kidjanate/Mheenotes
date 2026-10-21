package com.mheenotes.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.FilePlus
import com.composables.icons.lucide.FileUp
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.FolderInput
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.LibraryBig
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PanelLeft
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.mheenotes.core.model.Folder
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.AppState
import com.mheenotes.ui.LibrarySort
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.ConfirmModal
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.LocalOverlays
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.MenuDivider
import com.mheenotes.ui.design.MenuItem
import com.mheenotes.ui.design.MenuLabel
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.PlatformBackHandler
import com.mheenotes.ui.design.Popover
import com.mheenotes.ui.design.PopoverSide
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.design.TextField
import com.mheenotes.ui.design.pressable
import com.mheenotes.ui.i18n.S
import com.mheenotes.ui.platform.rememberImporter

sealed interface Section {
    data object All : Section
    data object Favorites : Section
    data object Recents : Section
    data object Trash : Section
    data class InFolder(val id: Id) : Section
}

/** Screen-local UI state; survives returning from the editor because it lives in AppState. */
class LibraryUi {
    var section by mutableStateOf<Section>(Section.All)
    var query by mutableStateOf("")
    var selecting by mutableStateOf(false)
    var selection by mutableStateOf(emptySet<Id>())
    var sidebar by mutableStateOf(true)

    fun toggle(id: Id) { selection = if (id in selection) selection - id else selection + id }
    fun endSelection() { selecting = false; selection = emptySet() }

    companion object { val shared = LibraryUi() }
}

@Composable
fun LibraryScreen(app: AppState) {
    val ui = remember { LibraryUi.shared }
    val lib by AppGraph.library.state.collectAsState()
    val settings = AppGraph.settings
    val s = S

    val visible by remember(lib, ui.section, ui.query, settings.librarySort) {
        derivedStateOf { filterNotebooks(lib.notebooks, ui.section, ui.query, settings.librarySort) }
    }
    val childFolders = remember(lib.folders, ui.section, ui.query) {
        if (ui.query.isNotBlank()) emptyList()
        else when (val sec = ui.section) {
            Section.All -> lib.folders.filter { it.parentId == null }
            is Section.InFolder -> lib.folders.filter { it.parentId == sec.id }
            else -> emptyList()
        }.sortedBy { it.name.lowercase() }
    }
    val counts = remember(lib.notebooks) {
        lib.notebooks.filter { it.trashedAt == null }.groupingBy { it.folderId }.eachCount()
    }

    PlatformBackHandler(ui.selecting) { ui.endSelection() }
    PlatformBackHandler(!ui.selecting && ui.section is Section.InFolder) {
        val f = lib.folders.firstOrNull { it.id == (ui.section as Section.InFolder).id }
        ui.section = f?.parentId?.let { Section.InFolder(it) } ?: Section.All
    }

    var showNew by remember { mutableStateOf(false) }
    var newFolderFor by remember { mutableStateOf<Section?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Notebook?>(null) }
    var renameFolder by remember { mutableStateOf<Folder?>(null) }
    var moveTargets by remember { mutableStateOf<Set<Id>?>(null) }
    var confirmDelete by remember { mutableStateOf<Set<Id>?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmFolder by remember { mutableStateOf<Folder?>(null) }
    val overlays = LocalOverlays.current
    val importer = rememberImporter { count ->
        if (count < 0) overlays.toast(s.importFailed) else if (count > 0) overlays.toast(s.imported(count))
    }
    val currentFolderId = (ui.section as? Section.InFolder)?.id

    Row(Modifier.fillMaxSize().background(M.colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        AnimatedVisibility(
            ui.sidebar,
            enter = expandHorizontally(Motion.standard()) + fadeIn(Motion.fade()),
            exit = shrinkHorizontally(Motion.standard()) + fadeOut(Motion.fade()),
        ) {
            Sidebar(
                ui = ui,
                folders = lib.folders,
                counts = counts,
                trashCount = lib.notebooks.count { it.trashedAt != null },
                onNewFolder = { newFolderFor = Section.All },
                onSettings = { showSettings = true },
                onRenameFolder = { renameFolder = it },
                onDeleteFolder = { confirmFolder = it },
            )
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                Header(
                    ui = ui,
                    title = sectionTitle(ui.section, lib.folders),
                    breadcrumbs = breadcrumbs(ui.section, lib.folders),
                    count = visible.size,
                    onNew = { showNew = true },
                    onNewFolder = { newFolderFor = ui.section },
                    onImportPdf = { importer.pdf(currentFolderId) },
                    onImportFile = { importer.notebook(currentFolderId) },
                    onEmptyTrash = { confirmEmpty = true },
                )
                val trash = ui.section == Section.Trash
                if (visible.isEmpty() && childFolders.isEmpty()) {
                    EmptyState(ui, trash, onNew = { showNew = true })
                } else {
                    NotebookCollection(
                        ui = ui,
                        app = app,
                        notebooks = visible,
                        folders = childFolders,
                        counts = counts,
                        trash = trash,
                        grid = settings.libraryGrid,
                        onRename = { renameTarget = it },
                        onMove = { moveTargets = setOf(it.id) },
                        onDeleteForever = { confirmDelete = setOf(it.id) },
                        onRenameFolder = { renameFolder = it },
                        onDeleteFolder = { confirmFolder = it },
                        onExport = { importer.share(it.id) },
                    )
                }
            }
            SelectionBar(
                ui = ui,
                trash = ui.section == Section.Trash,
                allIds = visible.map { it.id },
                onMove = { moveTargets = ui.selection },
                onDeleteForever = { confirmDelete = ui.selection },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    NewNotebookModal(showNew, onDismiss = { showNew = false }) { title, template, size, paper ->
        val n = AppGraph.createNotebook(title.ifBlank { s.untitled }, currentFolderId, template, size, paper)
        showNew = false
        app.open(n.id)
    }
    NameModal(
        visible = newFolderFor != null,
        title = s.newFolder,
        placeholder = s.folderName,
        initial = "",
        confirm = s.create,
        onDismiss = { newFolderFor = null },
    ) { name ->
        val parent = (newFolderFor as? Section.InFolder)?.id
        AppGraph.createFolder(name.ifBlank { s.newFolder }, parent)
        newFolderFor = null
    }
    NameModal(renameTarget != null, s.rename, s.notebookName, renameTarget?.title ?: "", s.save, { renameTarget = null }) {
        renameTarget?.let { n -> AppGraph.rename(n.id, it) }
        renameTarget = null
    }
    NameModal(renameFolder != null, s.rename, s.folderName, renameFolder?.name ?: "", s.save, { renameFolder = null }) {
        renameFolder?.let { f -> if (it.isNotBlank()) AppGraph.renameFolder(f.id, it) }
        renameFolder = null
    }
    MoveModal(moveTargets, lib.folders, onDismiss = { moveTargets = null }) { folder ->
        AppGraph.move(moveTargets.orEmpty(), folder)
        moveTargets = null
        ui.endSelection()
    }
    ConfirmModal(
        confirmDelete != null, s.deleteForeverConfirm, s.cannotUndo, s.deleteForever, s.cancel, danger = true,
        onConfirm = { confirmDelete?.forEach(AppGraph::deleteForever); ui.endSelection() },
        onDismiss = { confirmDelete = null },
    )
    ConfirmModal(
        confirmEmpty, s.emptyTrashConfirm, s.cannotUndo, s.emptyTrash, s.cancel, danger = true,
        onConfirm = { lib.notebooks.filter { it.trashedAt != null }.forEach { AppGraph.deleteForever(it.id) } },
        onDismiss = { confirmEmpty = false },
    )
    ConfirmModal(
        confirmFolder != null, s.deleteFolderConfirm, s.deleteFolderBody, s.delete, s.cancel, danger = true,
        onConfirm = {
            confirmFolder?.let { f ->
                if ((ui.section as? Section.InFolder)?.id == f.id) ui.section = Section.All
                AppGraph.deleteFolder(f.id)
            }
        },
        onDismiss = { confirmFolder = null },
    )
    SettingsModal(showSettings) { showSettings = false }
}

fun filterNotebooks(all: List<Notebook>, section: Section, query: String, sort: LibrarySort): List<Notebook> {
    val q = query.trim().lowercase()
    val base = all.asSequence().filter { n ->
        when (section) {
            Section.Trash -> n.trashedAt != null
            else -> n.trashedAt == null && when (section) {
                Section.All -> q.isNotEmpty() || n.folderId == null
                Section.Favorites -> n.favorite
                Section.Recents -> true
                is Section.InFolder -> q.isNotEmpty() || n.folderId == section.id
                Section.Trash -> false
            }
        }
    }.filter { q.isEmpty() || it.title.lowercase().contains(q) }
    val sorted = when {
        section == Section.Trash -> base.sortedByDescending { it.trashedAt }
        section == Section.Recents -> base.sortedByDescending { it.openedAt }.take(60)
        else -> when (sort) {
            LibrarySort.Edited -> base.sortedByDescending { it.updatedAt }
            LibrarySort.Opened -> base.sortedByDescending { it.openedAt }
            LibrarySort.Title -> base.sortedBy { it.title.lowercase() }
            LibrarySort.Created -> base.sortedByDescending { it.createdAt }
        }
    }
    return sorted.toList()
}

@Composable
private fun sectionTitle(section: Section, folders: List<Folder>): String = when (section) {
    Section.All -> S.allNotes
    Section.Favorites -> S.favorites
    Section.Recents -> S.recents
    Section.Trash -> S.trash
    is Section.InFolder -> folders.firstOrNull { it.id == section.id }?.name ?: S.folders
}

private fun breadcrumbs(section: Section, folders: List<Folder>): List<Folder> {
    if (section !is Section.InFolder) return emptyList()
    val out = ArrayList<Folder>()
    var cur = folders.firstOrNull { it.id == section.id }?.parentId
    while (cur != null) {
        val f = folders.firstOrNull { it.id == cur } ?: break
        out.add(0, f)
        cur = f.parentId
    }
    return out
}

// ---- Sidebar -------------------------------------------------------------------------------

@Composable
private fun Sidebar(
    ui: LibraryUi,
    folders: List<Folder>,
    counts: Map<Id?, Int>,
    trashCount: Int,
    onNewFolder: () -> Unit,
    onSettings: () -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
) {
    val c = M.colors
    val s = S
    Column(Modifier.width(264.dp).fillMaxHeight().padding(start = 12.dp, top = 12.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(Lucide.LibraryBig, tint = c.onAccent, size = 17.dp)
            }
            Spacer(Modifier.width(10.dp))
            Text(s.appName, Modifier.weight(1f), style = M.type.headline)
            IconButton(Lucide.Settings, onSettings, size = 36.dp, iconSize = 19.dp, tint = c.textSecondary)
        }
        SidebarItem(Lucide.LibraryBig, s.allNotes, ui.section == Section.All) { ui.section = Section.All; ui.endSelection() }
        SidebarItem(Lucide.Clock, s.recents, ui.section == Section.Recents) { ui.section = Section.Recents; ui.endSelection() }
        SidebarItem(Lucide.Star, s.favorites, ui.section == Section.Favorites) { ui.section = Section.Favorites; ui.endSelection() }
        SidebarItem(Lucide.Trash2, s.trash, ui.section == Section.Trash, badge = trashCount.takeIf { it > 0 }) { ui.section = Section.Trash; ui.endSelection() }

        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.folders.uppercase(), Modifier.weight(1f), style = M.type.overline, color = c.textTertiary)
            IconButton(Lucide.FolderPlus, onNewFolder, size = 30.dp, iconSize = 16.dp, tint = c.textSecondary)
        }
        val expanded = remember { mutableStateOf(emptySet<Id>()) }
        LazyColumn(Modifier.weight(1f)) {
            val rows = flattenFolders(folders, expanded.value)
            items(rows, key = { it.first.id }) { (f, depth) ->
                val hasChildren = folders.any { it.parentId == f.id }
                FolderRow(
                    folder = f,
                    depth = depth,
                    count = counts[f.id] ?: 0,
                    selected = ui.section == Section.InFolder(f.id),
                    expanded = f.id in expanded.value,
                    hasChildren = hasChildren,
                    onToggle = { expanded.value = if (f.id in expanded.value) expanded.value - f.id else expanded.value + f.id },
                    onClick = { ui.section = Section.InFolder(f.id); ui.endSelection() },
                    onRename = { onRenameFolder(f) },
                    onDelete = { onDeleteFolder(f) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private fun flattenFolders(all: List<Folder>, expanded: Set<Id>): List<Pair<Folder, Int>> {
    val byParent = all.filter { it.trashedAt == null }.groupBy { it.parentId }
    val out = ArrayList<Pair<Folder, Int>>()
    fun walk(parent: Id?, depth: Int) {
        for (f in byParent[parent].orEmpty().sortedBy { it.name.lowercase() }) {
            out += f to depth
            if (f.id in expanded) walk(f.id, depth + 1)
        }
    }
    walk(null, 0)
    return out
}

@Composable
private fun SidebarItem(icon: ImageVector, label: String, selected: Boolean, badge: Int? = null, onClick: () -> Unit) {
    val c = M.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .height(40.dp)
            .clip(RoundedCornerShape(Radii.sm))
            .background(if (selected) c.fillStrong else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, tint = if (selected) c.accent else c.textSecondary, size = 18.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = if (selected) M.type.bodyStrong else M.type.body, maxLines = 1)
        if (badge != null) Text(badge.toString(), style = M.type.caption, color = c.textTertiary)
    }
}

@Composable
private fun FolderRow(
    folder: Folder,
    depth: Int,
    count: Int,
    selected: Boolean,
    expanded: Boolean,
    hasChildren: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = M.colors
    val s = S
    var menu by remember { mutableStateOf(false) }
    val rot by androidx.compose.animation.core.animateFloatAsState(if (expanded) 90f else 0f, Motion.standard(), label = "chev")
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .pressable(pressedScale = 0.97f, onLongClick = { menu = true }, onClick = onClick)
                .height(38.dp)
                .clip(RoundedCornerShape(Radii.sm))
                .background(if (selected) c.fillStrong else androidx.compose.ui.graphics.Color.Transparent)
                .padding(start = (4 + depth * 16).dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp).pressable(enabled = hasChildren, onClick = onToggle), contentAlignment = Alignment.Center) {
                if (hasChildren) Icon(Lucide.ChevronRight, Modifier.rotate(rot), tint = c.textTertiary, size = 14.dp)
            }
            Icon(if (selected) Lucide.FolderOpen else Lucide.Folder, tint = if (selected) c.accent else c.textSecondary, size = 17.dp)
            Spacer(Modifier.width(10.dp))
            Text(folder.name, Modifier.weight(1f), style = M.type.body, maxLines = 1)
            if (count > 0) Text(count.toString(), style = M.type.caption, color = c.textTertiary)
        }
        Popover(menu, { menu = false }, side = PopoverSide.End) {
            MenuItem(s.rename, icon = Lucide.PenLine) { menu = false; onRename() }
            MenuItem(s.delete, icon = Lucide.Trash2, danger = true) { menu = false; onDelete() }
        }
    }
}

// ---- Header --------------------------------------------------------------------------------

@Composable
private fun Header(
    ui: LibraryUi,
    title: String,
    breadcrumbs: List<Folder>,
    count: Int,
    onNew: () -> Unit,
    onNewFolder: () -> Unit,
    onImportPdf: () -> Unit,
    onImportFile: () -> Unit,
    onEmptyTrash: () -> Unit,
) {
    val c = M.colors
    val s = S
    val settings = AppGraph.settings
    val trash = ui.section == Section.Trash
    Column(Modifier.fillMaxWidth().padding(start = 28.dp, end = 24.dp, top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(Lucide.PanelLeft, { ui.sidebar = !ui.sidebar }, tint = c.textSecondary)
            Spacer(Modifier.weight(1f))
            TextField(
                ui.query, { ui.query = it }, Modifier.width(260.dp),
                placeholder = s.searchNotes, leading = Lucide.Search, clearable = true,
            )
            Spacer(Modifier.width(8.dp))
            var sortMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(Lucide.ArrowUpDown, { sortMenu = true }, tint = c.textSecondary)
                Popover(sortMenu, { sortMenu = false }, align = Alignment.End) {
                    MenuLabel(s.sortBy)
                    LibrarySort.entries.forEach { o ->
                        val label = when (o) {
                            LibrarySort.Edited -> s.sortRecent
                            LibrarySort.Opened -> s.sortOpened
                            LibrarySort.Title -> s.sortTitle
                            LibrarySort.Created -> s.sortCreated
                        }
                        MenuItem(label, icon = if (settings.librarySort == o) Lucide.Check else null) { settings.updateSort(o); sortMenu = false }
                    }
                    MenuDivider()
                    MenuItem(s.viewGrid, icon = Lucide.LayoutGrid, trailing = if (settings.libraryGrid) "✓" else null) { settings.updateGrid(true); sortMenu = false }
                    MenuItem(s.viewList, icon = Lucide.LayoutList, trailing = if (!settings.libraryGrid) "✓" else null) { settings.updateGrid(false); sortMenu = false }
                }
            }
            Spacer(Modifier.width(4.dp))
            if (trash) {
                Button(s.emptyTrash, onEmptyTrash, style = ButtonStyle.Danger, compact = true)
            } else {
                Button(if (ui.selecting) s.done else s.select, { if (ui.selecting) ui.endSelection() else ui.selecting = true }, style = ButtonStyle.Ghost, compact = true)
                Spacer(Modifier.width(8.dp))
                var newMenu by remember { mutableStateOf(false) }
                Box {
                    Button(s.new, { newMenu = true }, icon = Lucide.Plus, style = ButtonStyle.Primary, compact = true)
                    Popover(newMenu, { newMenu = false }, align = Alignment.End) {
                        MenuItem(s.newNotebook, icon = Lucide.FilePlus) { newMenu = false; onNew() }
                        MenuItem(s.newFolder, icon = Lucide.FolderPlus) { newMenu = false; onNewFolder() }
                        MenuDivider()
                        MenuItem(s.importPdf, icon = Lucide.FileUp) { newMenu = false; onImportPdf() }
                        MenuItem(s.importNotebook, icon = Lucide.FolderInput) { newMenu = false; onImportFile() }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (breadcrumbs.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                breadcrumbs.forEach { f ->
                    Text(f.name, Modifier.pressable { ui.section = Section.InFolder(f.id) }, style = M.type.callout, color = c.textSecondary)
                    Icon(Lucide.ChevronRight, tint = c.textTertiary, size = 13.dp)
                }
            }
            Spacer(Modifier.height(2.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, Modifier.weight(1f, fill = false).animateContentSize(), style = M.type.display, maxLines = 1)
            Spacer(Modifier.width(12.dp))
            Text(s.notebooksCount(count), Modifier.padding(bottom = 5.dp), style = M.type.callout, color = c.textTertiary)
        }
        Spacer(Modifier.height(14.dp))
    }
}

// ---- Collection ----------------------------------------------------------------------------

@Composable
private fun NotebookCollection(
    ui: LibraryUi,
    app: AppState,
    notebooks: List<Notebook>,
    folders: List<Folder>,
    counts: Map<Id?, Int>,
    trash: Boolean,
    grid: Boolean,
    onRename: (Notebook) -> Unit,
    onMove: (Notebook) -> Unit,
    onDeleteForever: (Notebook) -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    onExport: (Notebook) -> Unit,
) {
    val state = rememberLazyGridState()
    LaunchedEffect(ui.section) { state.scrollToItem(0) }
    val actions = NotebookActions(
        open = { n -> if (ui.selecting) ui.toggle(n.id) else if (!trash) app.open(n.id) },
        rename = onRename, move = onMove, deleteForever = onDeleteForever, export = onExport,
    )
    LazyVerticalGrid(
        columns = if (grid) GridCells.Adaptive(176.dp) else GridCells.Fixed(1),
        state = state,
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(if (grid) 22.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(if (grid) 26.dp else 0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (folders.isNotEmpty()) {
            items(folders, key = { "f" + it.id }, span = { if (grid) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) { f ->
                FolderTile(
                    f, counts[f.id] ?: 0, grid,
                    onOpen = { ui.section = Section.InFolder(f.id) },
                    onRename = { onRenameFolder(f) },
                    onDelete = { onDeleteFolder(f) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "divider", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(0.dp)) }
        }
        items(notebooks, key = { it.id }) { n ->
            if (grid) {
                NotebookCard(n, trash, ui.selecting, n.id in ui.selection, actions, Modifier.animateItem())
            } else {
                NotebookRow(n, trash, ui.selecting, n.id in ui.selection, actions, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun EmptyState(ui: LibraryUi, trash: Boolean, onNew: () -> Unit) {
    val c = M.colors
    val s = S
    Column(Modifier.fillMaxSize().padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(c.fill), contentAlignment = Alignment.Center) {
            Icon(if (trash) Lucide.Trash2 else if (ui.query.isNotBlank()) Lucide.Search else Lucide.FilePlus, tint = c.textTertiary, size = 34.dp)
        }
        Spacer(Modifier.height(18.dp))
        val (title, body) = when {
            trash -> s.trashEmptyTitle to s.trashEmptyBody
            ui.query.isNotBlank() -> s.noResults to null
            ui.section is Section.InFolder -> s.folderEmpty to s.noNotesBody
            else -> s.noNotesTitle to s.noNotesBody
        }
        Text(title, style = M.type.headline)
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(body, style = M.type.callout, color = c.textSecondary)
        }
        if (!trash && ui.query.isBlank()) {
            Spacer(Modifier.height(22.dp))
            Button(s.newNotebook, onNew, icon = Lucide.Plus, style = ButtonStyle.Primary)
        }
    }
}

@Composable
private fun SelectionBar(
    ui: LibraryUi,
    trash: Boolean,
    allIds: List<Id>,
    onMove: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier,
) {
    val s = S
    val c = M.colors
    AnimatedVisibility(
        ui.selecting || (trash && ui.selection.isNotEmpty()),
        modifier.padding(bottom = 28.dp),
        enter = slideInVertically(Motion.bouncy()) { it * 2 } + fadeIn(Motion.fade()),
        exit = slideOutVertically(Motion.standard()) { it * 2 } + fadeOut(Motion.fade()),
    ) {
        val none = ui.selection.isEmpty()
        Card(shape = RoundedCornerShape(Radii.xl), color = c.elevated, elevation = 18.dp) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.selected(ui.selection.size), Modifier.padding(horizontal = 10.dp), style = M.type.calloutStrong, color = c.textSecondary)
                Button(s.selectAll, { ui.selection = if (ui.selection.size == allIds.size) emptySet() else allIds.toSet() }, style = ButtonStyle.Ghost, compact = true)
                if (trash) {
                    Button(s.restore, { AppGraph.restore(ui.selection); ui.endSelection() }, enabled = !none, compact = true)
                    Button(s.deleteForever, onDeleteForever, style = ButtonStyle.Danger, enabled = !none, compact = true)
                } else {
                    Button(s.moveTo, onMove, icon = Lucide.FolderInput, enabled = !none, compact = true)
                    Button(s.favorite, { AppGraph.setFavorite(ui.selection, true); ui.endSelection() }, icon = Lucide.Star, enabled = !none, compact = true)
                    Button(s.moveToTrash, { AppGraph.trash(ui.selection); ui.endSelection() }, icon = Lucide.Trash2, style = ButtonStyle.Danger, enabled = !none, compact = true)
                }
                IconButton(Lucide.X, { ui.endSelection() }, size = 34.dp, iconSize = 17.dp, tint = c.textSecondary)
            }
        }
    }
}
