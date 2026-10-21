package com.mheenotes.core.store

import com.mheenotes.core.model.Id
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** File layout under the app's private root directory. */
class AppFiles(val root: String) {
    val libraryDb: String get() = "$root/library.db"
    fun notebookDir(id: Id) = "$root/notebooks/$id"
    fun notebookDb(id: Id) = "${notebookDir(id)}/notebook.db"
    fun assetsDir(id: Id) = "${notebookDir(id)}/assets"
    fun asset(notebookId: Id, fileName: String) = "${assetsDir(notebookId)}/$fileName"
    fun thumb(id: Id) = "$root/thumbs/$id.png"
    val thumbsDir: String get() = "$root/thumbs"
    val tempDir: String get() = "$root/tmp"

    init {
        mkdirs("$root/notebooks"); mkdirs(thumbsDir); mkdirs(tempDir)
    }

    companion object {
        fun mkdirs(path: String) = SystemFileSystem.createDirectories(Path(path))

        fun exists(path: String) = SystemFileSystem.exists(Path(path))

        fun deleteRecursively(path: String) {
            val p = Path(path)
            if (!SystemFileSystem.exists(p)) return
            if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true) {
                for (child in SystemFileSystem.list(p)) deleteRecursively(child.toString())
            }
            SystemFileSystem.delete(p, mustExist = false)
        }
    }
}
