package com.mheenotes.core.store

import com.mheenotes.core.model.Asset
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Item
import com.mheenotes.core.model.Page
import com.mheenotes.core.model.PdfRef
import com.mheenotes.core.model.Template
import com.mheenotes.core.model.nowMillis

private val NOTEBOOK_SCHEMA = listOf(
    "CREATE TABLE IF NOT EXISTS meta(key TEXT PRIMARY KEY, value TEXT NOT NULL)",
    """CREATE TABLE IF NOT EXISTS pages(
        id TEXT PRIMARY KEY, ord REAL NOT NULL, width REAL NOT NULL, height REAL NOT NULL,
        template INTEGER NOT NULL, background INTEGER NOT NULL, pdf_asset TEXT, pdf_index INTEGER,
        deleted INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)""",
    // Deleted rows stay as tombstones (data cleared) so a future sync can propagate deletions.
    """CREATE TABLE IF NOT EXISTS items(
        id TEXT PRIMARY KEY, page_id TEXT NOT NULL, seq INTEGER NOT NULL, kind INTEGER NOT NULL,
        data BLOB NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)""",
    "CREATE INDEX IF NOT EXISTS items_page ON items(page_id, deleted)",
    "CREATE TABLE IF NOT EXISTS assets(id TEXT PRIMARY KEY, mime TEXT NOT NULL, file TEXT NOT NULL)",
)

private val NOTEBOOK_MIGRATIONS = listOf(
    "ALTER TABLE pages ADD COLUMN bookmarked INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE pages ADD COLUMN line_argb INTEGER",
    "ALTER TABLE pages ADD COLUMN rotation INTEGER NOT NULL DEFAULT 0",
)

/** Persistence for one notebook: `notebooks/<id>/notebook.db` plus its `assets/` folder. */
class NotebookStore(val notebookId: Id, val files: AppFiles) {
    init {
        AppFiles.mkdirs(files.assetsDir(notebookId))
    }

    private val db = Db(files.notebookDb(notebookId), NOTEBOOK_SCHEMA, NOTEBOOK_MIGRATIONS)

    suspend fun pages(): List<Page> = db.run {
        query("SELECT id,ord,width,height,template,background,pdf_asset,pdf_index,bookmarked,line_argb,rotation FROM pages WHERE deleted=0 ORDER BY ord") {
            Page(
                it.getText(0), it.getDouble(1), it.getDouble(2).toFloat(), it.getDouble(3).toFloat(),
                Template.entries.getOrElse(it.getLong(4).toInt()) { Template.Blank }, it.getLong(5).toInt(),
                it.textOrNull(6)?.let { a -> PdfRef(a, it.getLong(7).toInt()) },
                bookmarked = it.getLong(8) == 1L,
                lineArgb = it.longOrNull(9)?.toInt(),
                rotation = it.getLong(10).toInt(),
            )
        }
    }

    suspend fun items(pageId: Id): List<Item> = db.run {
        query("SELECT id,seq,kind,data FROM items WHERE page_id=? AND deleted=0", pageId) {
            ItemCodec.decode(it.getText(0), it.getLong(1), it.getLong(2).toInt(), it.getBlob(3))
        }
    }

    suspend fun maxSeq(): Long = db.run { query("SELECT IFNULL(MAX(seq),0) FROM items") { it.getLong(0) }.first() }

    suspend fun assets(): List<Asset> = db.run {
        query("SELECT id,mime,file FROM assets") { Asset(it.getText(0), it.getText(1), it.getText(2)) }
    }

    fun putAsset(asset: Asset) = db.post { exec("INSERT OR REPLACE INTO assets VALUES(?,?,?)", asset.id, asset.mime, asset.fileName) }

    fun assetPath(asset: Asset) = files.asset(notebookId, asset.fileName)

    fun writeItems(pageId: Id, removed: Collection<Id>, added: Collection<Item>) {
        if (removed.isEmpty() && added.isEmpty()) return
        val now = nowMillis()
        // Encoding happens on the worker, off the input thread.
        db.post {
            transaction {
                for (id in removed) exec("UPDATE items SET deleted=1, data=X'', updated_at=? WHERE id=?", now, id)
                for (item in added) exec(
                    "INSERT OR REPLACE INTO items VALUES(?,?,?,?,?,0,?)",
                    item.id, pageId, item.seq, ItemCodec.kind(item), ItemCodec.encode(item), now,
                )
            }
        }
    }

    /** Bulk insert for imports and fixtures; one transaction for everything. */
    fun writeBulk(pages: List<Page>, items: Map<Id, List<Item>>) {
        val now = nowMillis()
        db.post {
            transaction {
                for (p in pages) upsertPage(p, now)
                for ((pageId, list) in items) for (item in list) exec(
                    "INSERT OR REPLACE INTO items VALUES(?,?,?,?,?,0,?)",
                    item.id, pageId, item.seq, ItemCodec.kind(item), ItemCodec.encode(item), now,
                )
            }
        }
    }

    fun writePages(upserted: Collection<Page>, deleted: Collection<Id>) {
        if (upserted.isEmpty() && deleted.isEmpty()) return
        val now = nowMillis()
        db.post {
            transaction {
                for (p in upserted) upsertPage(p, now)
                for (id in deleted) exec("UPDATE pages SET deleted=1, updated_at=? WHERE id=?", now, id)
            }
        }
    }

    private fun androidx.sqlite.SQLiteConnection.upsertPage(p: Page, now: Long) = exec(
        "INSERT OR REPLACE INTO pages(id,ord,width,height,template,background,pdf_asset,pdf_index,deleted,updated_at,bookmarked,line_argb,rotation) VALUES(?,?,?,?,?,?,?,?,0,?,?,?,?)",
        p.id, p.ord, p.width, p.height, p.template.ordinal, p.backgroundArgb, p.pdf?.assetId, p.pdf?.pageIndex, now,
        p.bookmarked, p.lineArgb, p.rotation,
    )

    fun putMeta(key: String, value: String) = db.post { exec("INSERT OR REPLACE INTO meta VALUES(?,?)", key, value) }

    suspend fun meta(): Map<String, String> = db.run { query("SELECT key,value FROM meta") { it.getText(0) to it.getText(1) }.toMap() }

    /** Folds the WAL into the main file so the directory can be zipped as a self-contained file. */
    suspend fun checkpoint() = db.run { query("PRAGMA wal_checkpoint(TRUNCATE)") { } ; Unit }

    suspend fun flush() = db.flush()

    suspend fun close() = db.close()
}
