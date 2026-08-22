package com.mheenotes.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel.RIL_WORD
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal data class ThaiOcrResult(
    val text: String,
    val tokens: List<OcrTextToken>,
    val meanConfidence: Float,
)

/** Serializes access to one reusable Tesseract instance for offline Thai OCR. */
internal class ThaiTextRecognizer(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Mheenotes-Thai-OCR").apply { isDaemon = true }
    }
    private val closed = AtomicBoolean(false)
    private var engine: TessBaseAPI? = null

    fun process(bitmap: Bitmap, callback: (Result<ThaiOcrResult>) -> Unit) {
        if (closed.get()) {
            callback(Result.failure(IllegalStateException("Thai OCR is closed")))
            return
        }
        try {
            worker.execute {
                callback(runCatching { recognize(bitmap) })
            }
        } catch (error: RejectedExecutionException) {
            callback(Result.failure(error))
        }
    }

    private fun recognize(bitmap: Bitmap): ThaiOcrResult {
        check(!bitmap.isRecycled) { "OCR bitmap was already recycled" }
        val api = engine ?: createEngine().also { engine = it }
        return try {
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            api.setImage(bitmap)
            val text = normalizeThaiOcrText(api.getUTF8Text().orEmpty())
            ThaiOcrResult(
                text = text,
                tokens = api.readWordTokens(),
                meanConfidence = (api.meanConfidence().coerceIn(0, 100) / 100f),
            )
        } finally {
            runCatching { api.clear() }
        }
    }

    private fun createEngine(): TessBaseAPI {
        val dataRoot = ensureThaiModel()
        val api = TessBaseAPI()
        return try {
            check(api.init(dataRoot.absolutePath, THAI_LANGUAGE, TessBaseAPI.OEM_LSTM_ONLY)) {
                "The bundled Thai OCR model could not be initialized"
            }
            api
        } catch (error: Throwable) {
            api.recycle()
            throw error
        }
    }

    private fun ensureThaiModel(): File {
        val dataRoot = File(appContext.noBackupFilesDir, "ocr/tesseract")
        val tessDataDirectory = File(dataRoot, "tessdata")
        val destination = File(tessDataDirectory, THAI_MODEL_FILE)
        if (destination.isFile && destination.sha256() == THAI_MODEL_SHA256) return dataRoot

        check(tessDataDirectory.exists() || tessDataDirectory.mkdirs()) {
            "Thai OCR storage could not be created"
        }
        val temporary = File(tessDataDirectory, "$THAI_MODEL_FILE.tmp")
        appContext.assets.open("tessdata/$THAI_MODEL_FILE").use { input ->
            temporary.outputStream().buffered().use(input::copyTo)
        }
        check(temporary.sha256() == THAI_MODEL_SHA256) { "The bundled Thai OCR model is corrupted" }
        if (destination.exists()) check(destination.delete()) { "The old Thai OCR model could not be replaced" }
        check(temporary.renameTo(destination)) { "The Thai OCR model could not be installed" }
        return dataRoot
    }

    private fun TessBaseAPI.readWordTokens(): List<OcrTextToken> {
        val iterator = resultIterator ?: return emptyList()
        return try {
            buildList {
                var lineIndex = -1
                iterator.begin()
                do {
                    if (lineIndex < 0 || iterator.isAtBeginningOf(RIL_TEXTLINE)) lineIndex++
                    val word = iterator.getUTF8Text(RIL_WORD)?.trim().orEmpty()
                    val bounds = iterator.getBoundingRect(RIL_WORD)
                    if (word.isNotBlank() && bounds != null && !bounds.isEmpty) {
                        add(OcrTextToken(word, Rect(bounds), lineIndex))
                    }
                } while (iterator.next(RIL_WORD))
            }
        } finally {
            iterator.delete()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        worker.execute {
            engine?.recycle()
            engine = null
        }
        worker.shutdown()
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte ->
            String.format(Locale.ROOT, "%02x", byte.toInt() and 0xFF)
        }
    }

    private companion object {
        const val THAI_LANGUAGE = "tha"
        const val THAI_MODEL_FILE = "tha.traineddata"
        const val THAI_MODEL_SHA256 = "294227cc2d1292b0acb28d61d4115c88252b96d466ca90b417cf4cf0c67bf07c"
    }
}

internal fun normalizeThaiOcrText(rawText: String): String {
    val text = rawText.trim()
    val thaiCharacterCount = text.count { it in '\u0E00'..'\u0E7F' }
    if (thaiCharacterCount < 3) return text
    val interCharacterSpaces = THAI_INTER_CHARACTER_SPACE.findAll(text).count()
    val suspiciousSpacingRatio = interCharacterSpaces.toFloat() / (thaiCharacterCount - 1).coerceAtLeast(1)
    return if (interCharacterSpaces >= 2 && suspiciousSpacingRatio >= .3f) {
        text.replace(THAI_INTER_CHARACTER_SPACE, "")
    } else text
}

private val THAI_INTER_CHARACTER_SPACE = Regex("(?<=[\\u0E00-\\u0E7F])[ \\t]+(?=[\\u0E00-\\u0E7F])")
