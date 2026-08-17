package com.mheenotes.core.storage

import com.mheenotes.core.model.NotebookDocument
import com.mheenotes.core.model.NotebookId
import com.mheenotes.core.model.NotebookPage
import com.mheenotes.core.model.AssetId
import com.mheenotes.core.model.ImageObject
import com.mheenotes.core.model.ObjectId
import com.mheenotes.core.model.ObjectCrop
import com.mheenotes.core.model.ObjectTransform
import com.mheenotes.core.model.PageObject
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PageSize
import com.mheenotes.core.model.PageTemplate
import com.mheenotes.core.model.PdfPageSource
import com.mheenotes.core.model.SourceDocument
import com.mheenotes.core.model.SourceDocumentId
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.ShapeObject
import com.mheenotes.core.model.TextObject
import com.mheenotes.core.model.AudioObject
import com.mheenotes.core.model.TextAlignment
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Reliable working-directory + portable ZIP implementation of the .mynote format. */
object NativeNotebookContainer {
    private const val MANIFEST = "manifest.properties"
    private const val MAX_ENTRIES = 20_000
    private const val MAX_ENTRY_BYTES = 512L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 4L * 1024L * 1024L * 1024L

    fun saveManifest(document: NotebookDocument, workingDirectory: File) {
        workingDirectory.mkdirs()
        val target = workingDirectory.resolve(MANIFEST)
        val temporary = workingDirectory.resolve("$MANIFEST.tmp")
        val backup = workingDirectory.resolve("$MANIFEST.backup")
        FileOutputStream(temporary, false).use { stream ->
            val output = BufferedOutputStream(stream)
            ManifestCodec.write(document, output)
            output.flush()
            stream.fd.sync()
        }
        if (target.exists()) {
            if (backup.exists()) check(backup.delete()) { "Unable to replace old manifest backup" }
            check(target.renameTo(backup)) { "Unable to preserve previous notebook manifest" }
        }
        if (!temporary.renameTo(target)) {
            if (backup.exists()) backup.renameTo(target)
            error("Unable to publish notebook manifest")
        }
        backup.delete()
    }

    fun loadManifest(workingDirectory: File): NotebookDocument {
        val target = workingDirectory.resolve(MANIFEST)
        return runCatching {
            target.inputStream().buffered().use(ManifestCodec::read)
        }.getOrElse { primaryFailure ->
            val backup = workingDirectory.resolve("$MANIFEST.backup")
            if (!backup.isFile) throw primaryFailure
            backup.inputStream().buffered().use(ManifestCodec::read)
        }
    }

    fun pack(workingDirectory: File, output: OutputStream) {
        require(workingDirectory.resolve(MANIFEST).isFile) { "Notebook manifest is missing" }
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            workingDirectory.walkTopDown().filter(File::isFile).sortedBy { it.relativeTo(workingDirectory).path }
                .forEach { file ->
                    val relative = file.relativeTo(workingDirectory).invariantSeparatorsPath
                    require(isSafeRelativePath(relative))
                    zip.putNextEntry(ZipEntry(relative).apply { time = 0L })
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    fun unpack(input: InputStream, destination: File): NotebookDocument {
        destination.mkdirs()
        val canonicalRoot = destination.canonicalFile
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                check(++entryCount <= MAX_ENTRIES) { "Notebook has too many entries" }
                check(isSafeRelativePath(entry.name)) { "Unsafe notebook path" }
                val target = destination.resolve(entry.name).canonicalFile
                check(target.path.startsWith(canonicalRoot.path + File.separator)) { "Entry escapes notebook" }
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var entryBytes = 0L
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            check(entryBytes <= MAX_ENTRY_BYTES && totalBytes <= MAX_TOTAL_BYTES) {
                                "Notebook exceeds safe extraction limit"
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return loadManifest(destination)
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(bytes)
                if (count < 0) break
                digest.update(bytes, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && !path.startsWith('\\') &&
            path.split('/', '\\').none { it == ".." || it.isBlank() }
}

private object ManifestCodec {
    fun write(document: NotebookDocument, output: OutputStream) {
        val p = Properties().apply {
            setProperty("formatVersion", document.formatVersion.toString())
            setProperty("id", document.id.value)
            setProperty("title", document.title)
            setProperty("createdAt", document.createdAtEpochMillis.toString())
            setProperty("modifiedAt", document.modifiedAtEpochMillis.toString())
            document.folderPath?.let { setProperty("folderPath", it) }
            setProperty("favorite", document.favorite.toString())
            document.trashedAtEpochMillis?.let { setProperty("trashedAt", it.toString()) }
            setProperty("lastOpenedAt", document.lastOpenedAtEpochMillis.toString())
            setProperty("coverArgb", document.coverArgb.toString())
            setProperty("pageCount", document.pages.size.toString())
            setProperty("sourceCount", document.sourceDocuments.size.toString())
            document.sourceDocuments.forEachIndexed { index, source ->
                val key = "source.$index."
                setProperty(key + "id", source.id.value)
                setProperty(key + "assetPath", source.assetPath)
                setProperty(key + "mediaType", source.mediaType)
                setProperty(key + "sha256", source.sha256)
                setProperty(key + "pageCount", source.pageCount.toString())
            }
            document.pages.forEachIndexed { index, page ->
                val key = "page.$index."
                setProperty(key + "id", page.id.value)
                setProperty(key + "width", page.size.widthMicrons.toString())
                setProperty(key + "height", page.size.heightMicrons.toString())
                setProperty(key + "rotation", page.rotationDegrees.toString())
                setProperty(key + "template", page.template.name)
                setProperty(key + "bookmarked", page.bookmarked.toString())
                setProperty(key + "backgroundArgb", page.backgroundArgb.toString())
                setProperty(key + "templateLineArgb", page.templateLineArgb.toString())
                setProperty(key + "objectCount", page.objects.size.toString())
                page.pdfSource?.let { source ->
                    setProperty(key + "pdfSourceId", source.documentId.value)
                    setProperty(key + "pdfPageIndex", source.pageIndex.toString())
                }
                page.objects.sortedBy(PageObject::zIndex).forEachIndexed { objectIndex, pageObject ->
                    writeObject(key + "object.$objectIndex.", pageObject)
                }
            }
        }
        p.store(output, "Mheenotes portable notebook manifest")
    }

    fun read(input: InputStream): NotebookDocument {
        val p = Properties().apply { load(input) }
        val version = p.required("formatVersion").toInt()
        require(version in 1..NotebookDocument.CURRENT_FORMAT_VERSION) { "Unsupported notebook version $version" }
        val sources = (0 until (p.getProperty("sourceCount")?.toInt() ?: 0)).map { index ->
            val key = "source.$index."
            SourceDocument(
                id = SourceDocumentId(p.required(key + "id")),
                assetPath = p.required(key + "assetPath"),
                mediaType = p.required(key + "mediaType"),
                sha256 = p.required(key + "sha256"),
                pageCount = p.required(key + "pageCount").toInt(),
            )
        }
        val pages = (0 until p.required("pageCount").toInt()).map { index ->
            val key = "page.$index."
            val pdfSourceId = p.getProperty(key + "pdfSourceId")
            val objects = (0 until (p.getProperty(key + "objectCount")?.toInt() ?: 0)).map { objectIndex ->
                p.readObject(key + "object.$objectIndex.")
            }
            NotebookPage(
                id = PageId(p.required(key + "id")),
                size = PageSize(p.required(key + "width").toFloat(), p.required(key + "height").toFloat()),
                rotationDegrees = p.required(key + "rotation").toInt(),
                template = PageTemplate.valueOf(p.required(key + "template")),
                pdfSource = pdfSourceId?.let {
                    PdfPageSource(
                        documentId = SourceDocumentId(it),
                        pageIndex = p.required(key + "pdfPageIndex").toInt(),
                    )
                },
                objects = objects,
                bookmarked = p.required(key + "bookmarked").toBooleanStrict(),
                backgroundArgb = p.getProperty(key + "backgroundArgb")?.toInt() ?: 0xFFFFFEFA.toInt(),
                templateLineArgb = p.getProperty(key + "templateLineArgb")?.toInt() ?: 0x402563EB,
            )
        }
        return NotebookDocument(
            formatVersion = version,
            id = NotebookId(p.required("id")),
            title = p.required("title"),
            createdAtEpochMillis = p.required("createdAt").toLong(),
            modifiedAtEpochMillis = p.required("modifiedAt").toLong(),
            pages = pages,
            sourceDocuments = sources,
            folderPath = p.getProperty("folderPath")?.takeIf(String::isNotBlank),
            favorite = p.getProperty("favorite")?.toBooleanStrict() ?: false,
            trashedAtEpochMillis = p.getProperty("trashedAt")?.toLong(),
            lastOpenedAtEpochMillis = p.getProperty("lastOpenedAt")?.toLong()
                ?: p.required("modifiedAt").toLong(),
            coverArgb = p.getProperty("coverArgb")?.toInt() ?: 0xFF303747.toInt(),
        )
    }

    private fun Properties.writeObject(key: String, value: PageObject) {
        setProperty(key + "id", value.id.value)
        setProperty(key + "zIndex", value.zIndex.toString())
        setProperty(key + "x", value.transform.x.toString())
        setProperty(key + "y", value.transform.y.toString())
        setProperty(key + "width", value.transform.width.toString())
        setProperty(key + "height", value.transform.height.toString())
        setProperty(key + "rotation", value.transform.rotationDegrees.toString())
        when (value) {
            is ImageObject -> {
                setProperty(key + "type", "image")
                setProperty(key + "assetId", value.assetId.value)
                setProperty(key + "opacity", value.opacity.toString())
                setProperty(key + "cropLeft", value.crop.leftFraction.toString())
                setProperty(key + "cropTop", value.crop.topFraction.toString())
                setProperty(key + "cropRight", value.crop.rightFraction.toString())
                setProperty(key + "cropBottom", value.crop.bottomFraction.toString())
                setProperty(key + "locked", value.locked.toString())
            }
            is TextObject -> {
                setProperty(key + "type", "text")
                setProperty(key + "text", value.text)
                setProperty(key + "argb", value.argb.toString())
                setProperty(key + "textSize", value.textSizeMicrons.toString())
                setProperty(key + "bold", value.bold.toString())
                setProperty(key + "italic", value.italic.toString())
                setProperty(key + "underline", value.underline.toString())
                setProperty(key + "alignment", value.alignment.name)
                setProperty(key + "fontFamily", value.fontFamily)
                value.outlineArgb?.let { setProperty(key + "outlineArgb", it.toString()) }
                setProperty(key + "outlineWidth", value.outlineWidthMicrons.toString())
                setProperty(key + "curveDegrees", value.curveDegrees.toString())
            }
            is ShapeObject -> {
                setProperty(key + "type", "shape")
                setProperty(key + "kind", value.kind.name)
                setProperty(key + "strokeArgb", value.strokeArgb.toString())
                setProperty(key + "strokeWidth", value.strokeWidthMicrons.toString())
                value.fillArgb?.let { setProperty(key + "fillArgb", it.toString()) }
            }
            is AudioObject -> {
                setProperty(key + "type", "audio")
                setProperty(key + "assetId", value.assetId.value)
                setProperty(key + "durationMillis", value.durationMillis.toString())
                setProperty(key + "transcript", value.transcript)
            }
        }
    }

    private fun Properties.readObject(key: String): PageObject {
        val transform = ObjectTransform(
            x = required(key + "x").toFloat(),
            y = required(key + "y").toFloat(),
            width = required(key + "width").toFloat(),
            height = required(key + "height").toFloat(),
            rotationDegrees = required(key + "rotation").toFloat(),
        )
        val id = ObjectId(required(key + "id"))
        val zIndex = required(key + "zIndex").toLong()
        return when (required(key + "type")) {
            "image" -> ImageObject(
                id = id,
                assetId = AssetId(required(key + "assetId")),
                transform = transform,
                zIndex = zIndex,
                opacity = getProperty(key + "opacity")?.toFloat() ?: 1f,
                crop = ObjectCrop(
                    leftFraction = getProperty(key + "cropLeft")?.toFloat() ?: 0f,
                    topFraction = getProperty(key + "cropTop")?.toFloat() ?: 0f,
                    rightFraction = getProperty(key + "cropRight")?.toFloat() ?: 0f,
                    bottomFraction = getProperty(key + "cropBottom")?.toFloat() ?: 0f,
                ),
                locked = getProperty(key + "locked")?.toBooleanStrict() ?: false,
            )
            "text" -> TextObject(
                id = id,
                text = required(key + "text"),
                argb = required(key + "argb").toInt(),
                textSizeMicrons = required(key + "textSize").toFloat(),
                bold = getProperty(key + "bold")?.toBooleanStrict() ?: false,
                italic = getProperty(key + "italic")?.toBooleanStrict() ?: false,
                underline = getProperty(key + "underline")?.toBooleanStrict() ?: false,
                alignment = getProperty(key + "alignment")?.let(TextAlignment::valueOf) ?: TextAlignment.Start,
                fontFamily = getProperty(key + "fontFamily") ?: "sans-serif",
                outlineArgb = getProperty(key + "outlineArgb")?.toInt(),
                outlineWidthMicrons = getProperty(key + "outlineWidth")?.toFloat() ?: 0f,
                curveDegrees = getProperty(key + "curveDegrees")?.toFloat() ?: 0f,
                transform = transform,
                zIndex = zIndex,
            )
            "shape" -> ShapeObject(
                id = id,
                kind = ShapeKind.valueOf(required(key + "kind")),
                strokeArgb = required(key + "strokeArgb").toInt(),
                strokeWidthMicrons = required(key + "strokeWidth").toFloat(),
                fillArgb = getProperty(key + "fillArgb")?.toInt(),
                transform = transform,
                zIndex = zIndex,
            )
            "audio" -> AudioObject(
                id = id,
                assetId = AssetId(required(key + "assetId")),
                durationMillis = getProperty(key + "durationMillis")?.toLong() ?: 0L,
                transcript = getProperty(key + "transcript") ?: "",
                transform = transform,
                zIndex = zIndex,
            )
            else -> error("Unknown page object type")
        }
    }

    private fun Properties.required(key: String): String = getProperty(key) ?: error("Missing $key")
}
