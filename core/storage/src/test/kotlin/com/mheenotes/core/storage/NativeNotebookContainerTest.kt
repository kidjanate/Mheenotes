package com.mheenotes.core.storage

import com.mheenotes.core.model.NotebookDocument
import com.mheenotes.core.model.AssetId
import com.mheenotes.core.model.AudioObject
import com.mheenotes.core.model.ImageObject
import com.mheenotes.core.model.ObjectId
import com.mheenotes.core.model.ObjectCrop
import com.mheenotes.core.model.ObjectTransform
import com.mheenotes.core.model.PdfPageSource
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.ShapeObject
import com.mheenotes.core.model.SourceDocument
import com.mheenotes.core.model.SourceDocumentId
import com.mheenotes.core.model.TextObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NativeNotebookContainerTest {
    @Test fun manifestAndContainerRoundTrip() {
        val source = Files.createTempDirectory("mynote-source").toFile()
        val destination = Files.createTempDirectory("mynote-dest").toFile()
        val sourceId = SourceDocumentId("source-pdf")
        val notebook = NotebookDocument.empty("Statistics", now = 42).let { document ->
            document.copy(
                pages = document.pages.map { page ->
                    page.copy(
                        pdfSource = PdfPageSource(sourceId, 3),
                        objects = listOf(
                            ImageObject(
                                ObjectId("image-1"), AssetId("asset-1"),
                                ObjectTransform(1f, 2f, 300f, 200f, 15f), 1,
                                opacity = .8f,
                                crop = ObjectCrop(.1f, .2f, .15f, .05f),
                                locked = true,
                            ),
                            TextObject(
                                ObjectId("text-1"), "Line one\nLine two", 0xFF112233.toInt(), 8_000f,
                                ObjectTransform(4f, 5f, 60_000f, 20_000f), 2,
                            ),
                            ShapeObject(
                                ObjectId("shape-1"), ShapeKind.Ellipse, 0xFF445566.toInt(), 900f,
                                0x22445566, ObjectTransform(7f, 8f, 40_000f, 30_000f), 3,
                            ),
                            AudioObject(
                                ObjectId("audio-1"), AssetId("audio-asset"), 12_345L, "Lecture notes",
                                ObjectTransform(12f, 13f, 12_000f, 12_000f), 4,
                            ),
                        ),
                    )
                },
                sourceDocuments = listOf(
                    SourceDocument(sourceId, "sources/source-pdf.pdf", "application/pdf", "abc", 8),
                ),
            )
        }
        NativeNotebookContainer.saveManifest(notebook, source)
        source.resolve("assets").mkdirs()
        source.resolve("assets/asset-1.image").writeText("embedded image bytes")
        source.resolve("assets/audio-asset.wav").writeText("embedded audio bytes")
        val bytes = ByteArrayOutputStream().also { NativeNotebookContainer.pack(source, it) }.toByteArray()
        val restored = NativeNotebookContainer.unpack(ByteArrayInputStream(bytes), destination)
        assertEquals(notebook, restored)
        assertEquals("embedded image bytes", destination.resolve("assets/asset-1.image").readText())
        assertEquals("embedded audio bytes", destination.resolve("assets/audio-asset.wav").readText())
    }

    @Test fun rejectsFutureVersion() {
        val directory = Files.createTempDirectory("mynote-future").toFile()
        directory.resolve("manifest.properties").writeText(
            "formatVersion=999\nid=x\ntitle=x\ncreatedAt=1\nmodifiedAt=1\npageCount=1\n",
        )
        assertThrows(IllegalArgumentException::class.java) {
            NativeNotebookContainer.loadManifest(directory)
        }
    }

    @Test fun fallsBackToPreviousManifestAfterInterruptedReplacement() {
        val directory = Files.createTempDirectory("mynote-manifest-recovery").toFile()
        val notebook = NotebookDocument.empty("Recovered", now = 123)
        NativeNotebookContainer.saveManifest(notebook, directory)

        val manifest = directory.resolve("manifest.properties")
        val backup = directory.resolve("manifest.properties.backup")
        assertEquals(true, manifest.renameTo(backup))
        manifest.writeText("incomplete=true\n")

        assertEquals(notebook, NativeNotebookContainer.loadManifest(directory))
    }

    @Test fun loadsVersionOneManifestWithoutEditableObjects() {
        val directory = Files.createTempDirectory("mynote-v1").toFile()
        directory.resolve("manifest.properties").writeText(
            """
            formatVersion=1
            id=legacy-note
            title=Legacy
            createdAt=10
            modifiedAt=20
            pageCount=1
            sourceCount=0
            page.0.id=legacy-page
            page.0.width=210000.0
            page.0.height=297000.0
            page.0.rotation=0
            page.0.template=Blank
            page.0.bookmarked=false
            """.trimIndent(),
        )

        val restored = NativeNotebookContainer.loadManifest(directory)
        assertEquals(1, restored.formatVersion)
        assertEquals(emptyList<Any>(), restored.pages.single().objects)
    }
}
