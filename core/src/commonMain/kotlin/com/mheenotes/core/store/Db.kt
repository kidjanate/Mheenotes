package com.mheenotes.core.store

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.launch

/**
 * One SQLite connection confined to a single worker. Every read and write goes through [run]
 * or [post] and executes strictly in submission order, so the UI never waits on disk and
 * writes can never reorder.
 */
class Db(path: String, schema: List<String>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private lateinit var conn: SQLiteConnection

    init {
        // Opening happens on the worker too, so constructing a Db never blocks the caller.
        queue.trySend {
            conn = BundledSQLiteDriver().open(path)
            conn.execSQL("PRAGMA journal_mode=WAL")
            conn.execSQL("PRAGMA synchronous=NORMAL")
            for (sql in schema) conn.execSQL(sql)
        }
        scope.launch { for (job in queue) job() }
    }

    /** Runs [block] on the DB worker and returns its result. */
    suspend fun <T> run(block: SQLiteConnection.() -> T): T {
        val result = CompletableDeferred<T>()
        queue.send { result.completeWith(runCatching { conn.block() }) }
        return result.await()
    }

    /** Fire-and-forget write, ordered after everything submitted earlier. */
    fun post(block: SQLiteConnection.() -> Unit) {
        queue.trySend { runCatching { conn.block() }.onFailure { onError(it) } }
    }

    var onError: (Throwable) -> Unit = { it.printStackTrace() }

    /** Waits for every queued write. */
    suspend fun flush() = run { }

    suspend fun close() {
        run { conn.close() }
        queue.close()
        scope.cancel()
    }
}

inline fun <T> SQLiteConnection.transaction(block: SQLiteConnection.() -> T): T {
    execSQL("BEGIN IMMEDIATE")
    try {
        val r = block()
        execSQL("COMMIT")
        return r
    } catch (t: Throwable) {
        execSQL("ROLLBACK")
        throw t
    }
}

inline fun <T> SQLiteConnection.query(sql: String, vararg args: Any?, row: (SQLiteStatement) -> T): List<T> =
    prepare(sql).use { st ->
        st.bindAll(args)
        val out = ArrayList<T>()
        while (st.step()) out += row(st)
        out
    }

fun SQLiteConnection.exec(sql: String, vararg args: Any?) {
    prepare(sql).use { st ->
        st.bindAll(args)
        st.step()
    }
}

fun SQLiteStatement.bindAll(args: Array<out Any?>) {
    args.forEachIndexed { i, v ->
        val idx = i + 1
        when (v) {
            null -> bindNull(idx)
            is String -> bindText(idx, v)
            is Int -> bindLong(idx, v.toLong())
            is Long -> bindLong(idx, v)
            is Float -> bindDouble(idx, v.toDouble())
            is Double -> bindDouble(idx, v)
            is Boolean -> bindLong(idx, if (v) 1 else 0)
            is ByteArray -> bindBlob(idx, v)
            else -> error("Unsupported SQL argument ${v::class}")
        }
    }
}

fun SQLiteStatement.textOrNull(i: Int): String? = if (isNull(i)) null else getText(i)
fun SQLiteStatement.longOrNull(i: Int): Long? = if (isNull(i)) null else getLong(i)
