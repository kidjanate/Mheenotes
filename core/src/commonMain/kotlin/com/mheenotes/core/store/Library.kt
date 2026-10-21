package com.mheenotes.core.store

import com.mheenotes.core.model.Folder
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook
import com.mheenotes.core.model.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class LibraryState(
    val loaded: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val notebooks: List<Notebook> = emptyList(),
)

private val LIBRARY_SCHEMA = listOf(
    """CREATE TABLE IF NOT EXISTS folders(
        id TEXT PRIMARY KEY, name TEXT NOT NULL, parent_id TEXT, created_at INTEGER NOT NULL, trashed_at INTEGER)""",
    """CREATE TABLE IF NOT EXISTS notebooks(
        id TEXT PRIMARY KEY, title TEXT NOT NULL, folder_id TEXT, cover INTEGER NOT NULL,
        created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, opened_at INTEGER NOT NULL,
        favorite INTEGER NOT NULL DEFAULT 0, trashed_at INTEGER, page_count INTEGER NOT NULL DEFAULT 1,
        thumb_version INTEGER NOT NULL DEFAULT 0)""",
    // Completed strokes per local day, for the activity widget.
    "CREATE TABLE IF NOT EXISTS ink_activity(day INTEGER PRIMARY KEY, strokes INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY, value TEXT NOT NULL)",
)

/**
 * The whole library index lives in memory (1,000 notebooks is ~200 KB), so filtering, sorting
 * and search never touch disk. Every mutation updates memory first, then persists in order.
 */
class Library(private val files: AppFiles) {
    private val db = Db(files.libraryDb, LIBRARY_SCHEMA)
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state

    suspend fun load() {
        val (folders, notebooks) = db.run {
            val f = query("SELECT id,name,parent_id,created_at,trashed_at FROM folders") {
                Folder(it.getText(0), it.getText(1), it.textOrNull(2), it.getLong(3), it.longOrNull(4))
            }
            val n = query(
                "SELECT id,title,folder_id,cover,created_at,updated_at,opened_at,favorite,trashed_at,page_count,thumb_version FROM notebooks",
            ) {
                Notebook(
                    it.getText(0), it.getText(1), it.textOrNull(2), it.getLong(3).toInt(), it.getLong(4),
                    it.getLong(5), it.getLong(6), it.getLong(7) == 1L, it.longOrNull(8), it.getLong(9).toInt(), it.getLong(10),
                )
            }
            f to n
        }
        _state.value = LibraryState(true, folders, notebooks)
    }

    fun notebook(id: Id): Notebook? = _state.value.notebooks.firstOrNull { it.id == id }

    fun upsert(n: Notebook) {
        _state.update { s ->
            val i = s.notebooks.indexOfFirst { it.id == n.id }
            s.copy(notebooks = if (i < 0) s.notebooks + n else s.notebooks.toMutableList().also { it[i] = n })
        }
        db.post {
            exec(
                "INSERT OR REPLACE INTO notebooks VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                n.id, n.title, n.folderId, n.coverArgb, n.createdAt, n.updatedAt, n.openedAt, n.favorite,
                n.trashedAt, n.pageCount, n.thumbVersion,
            )
        }
    }

    /** Batch insert used by imports and the performance fixture. */
    fun upsertAll(list: List<Notebook>) {
        _state.update { s ->
            val ids = list.mapTo(HashSet()) { it.id }
            s.copy(notebooks = s.notebooks.filter { it.id !in ids } + list)
        }
        db.post {
            transaction {
                for (n in list) exec(
                    "INSERT OR REPLACE INTO notebooks VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    n.id, n.title, n.folderId, n.coverArgb, n.createdAt, n.updatedAt, n.openedAt, n.favorite,
                    n.trashedAt, n.pageCount, n.thumbVersion,
                )
            }
        }
    }

    fun update(id: Id, change: (Notebook) -> Notebook) {
        notebook(id)?.let { upsert(change(it)) }
    }

    /** Removes the index row. The caller deletes the notebook's files. */
    fun deleteForever(id: Id) {
        _state.update { s -> s.copy(notebooks = s.notebooks.filter { it.id != id }) }
        db.post { exec("DELETE FROM notebooks WHERE id=?", id) }
    }

    fun upsertFolder(f: Folder) {
        _state.update { s ->
            val i = s.folders.indexOfFirst { it.id == f.id }
            s.copy(folders = if (i < 0) s.folders + f else s.folders.toMutableList().also { it[i] = f })
        }
        db.post { exec("INSERT OR REPLACE INTO folders VALUES(?,?,?,?,?)", f.id, f.name, f.parentId, f.createdAt, f.trashedAt) }
    }

    fun deleteFolderForever(id: Id) {
        _state.update { s -> s.copy(folders = s.folders.filter { it.id != id }) }
        db.post { exec("DELETE FROM folders WHERE id=?", id) }
    }

    fun recordStrokes(count: Int, day: Long = nowMillis().toLocalDay()) {
        if (count == 0) return
        db.post {
            exec(
                "INSERT INTO ink_activity(day,strokes) VALUES(?,?) ON CONFLICT(day) DO UPDATE SET strokes=strokes+excluded.strokes",
                day, count,
            )
        }
    }

    /** Stroke counts for the last [days] days, oldest first. */
    suspend fun activity(days: Int, today: Long = nowMillis().toLocalDay()): List<Int> = db.run {
        val counts = query("SELECT day,strokes FROM ink_activity WHERE day>?", today - days) { it.getLong(0) to it.getLong(1).toInt() }.toMap()
        List(days) { i -> counts[today - days + 1 + i] ?: 0 }
    }

    suspend fun setting(key: String): String? = db.run {
        query("SELECT value FROM settings WHERE key=?", key) { it.getText(0) }.firstOrNull()
    }

    fun putSetting(key: String, value: String) = db.post { exec("INSERT OR REPLACE INTO settings VALUES(?,?)", key, value) }

    suspend fun flush() = db.flush()
}

/** Day number in the device's local time zone; set by the platform at startup. */
var localOffsetMillis: Long = 0L

fun Long.toLocalDay(): Long = (this + localOffsetMillis).floorDiv(86_400_000L)
