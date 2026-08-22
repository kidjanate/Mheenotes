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
            val rawTokens = mutableListOf<OcrTextToken>()
            var lineIndex = -1
            iterator.begin()
            do {
                if (lineIndex < 0 || iterator.isAtBeginningOf(RIL_TEXTLINE)) lineIndex++
                val word = iterator.getUTF8Text(RIL_WORD)?.trim().orEmpty()
                val bounds = iterator.getBoundingRect(RIL_WORD)
                if (word.isNotBlank() && bounds != null && !bounds.isEmpty) {
                    rawTokens.add(OcrTextToken(word, Rect(bounds), lineIndex))
                }
            } while (iterator.next(RIL_WORD))
            mergeAdjacentThaiTokens(rawTokens)
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
    if (rawText.isBlank()) return ""
    return rawText.lines().joinToString("\n") { line ->
        normalizeThaiLine(line)
    }.trim()
}

private fun normalizeThaiLine(line: String): String {
    var text = line.trim()
    val thaiCharCount = text.count { it in '\u0E00'..'\u0E7F' }
    if (thaiCharCount < 2) return text

    // 1. Remove spaces before combining marks, vowels (upper/lower/following), tone marks, diacritics, and symbols
    text = text.replace(THAI_COMBINING_BEFORE_SPACE, "")
    // 2. Remove spaces immediately after leading vowels before a Thai consonant
    text = text.replace(THAI_LEADING_VOWEL_AFTER_SPACE, "")

    // 3. Check for inter-Thai character spaces
    val thaiGaps = THAI_INTER_CHARACTER_SPACE.findAll(text).toList()
    if (thaiGaps.isEmpty()) return text

    // Check if the spacing is systematic character-spacing (e.g. "ภ า ษ า ไ ท ย" or "ส วั ส ดี")
    val thaiWords = text.split(Regex("[ \\t]+")).filter { token ->
        token.any { it in '\u0E00'..'\u0E7F' }
    }
    val shortThaiWords = thaiWords.count { token ->
        token.count { it in '\u0E00'..'\u0E7F' } <= 2
    }
    val hasSystematicSpacing = (thaiGaps.size >= 2 && thaiGaps.size.toFloat() / (thaiCharCount - 1).coerceAtLeast(1) >= 0.25f) ||
        (thaiWords.isNotEmpty() && shortThaiWords.toFloat() / thaiWords.size >= 0.5f)

    if (hasSystematicSpacing) {
        text = text.replace(THAI_INTER_CHARACTER_SPACE, "")
    } else {
        text = mergeShortThaiGaps(text)
    }

    return text.replace(Regex("[ \\t]{2,}"), " ").trim()
}

private fun mergeShortThaiGaps(text: String): String {
    val words = text.split(Regex("[ \\t]+"))
    if (words.size <= 1) return text
    val result = StringBuilder()
    for (i in words.indices) {
        val word = words[i]
        if (i == 0) {
            result.append(word)
            continue
        }
        val prevWord = words[i - 1]
        val prevIsThai = prevWord.isNotEmpty() && prevWord.last() in '\u0E00'..'\u0E7F'
        val currIsThai = word.isNotEmpty() && word.first() in '\u0E00'..'\u0E7F'
        val prevThaiLen = prevWord.count { it in '\u0E00'..'\u0E7F' }
        val currThaiLen = word.count { it in '\u0E00'..'\u0E7F' }

        if (prevIsThai && currIsThai && (prevThaiLen <= 2 || currThaiLen <= 2)) {
            result.append(word)
        } else {
            result.append(' ').append(word)
        }
    }
    return result.toString()
}

private fun mergeAdjacentThaiTokens(rawTokens: List<OcrTextToken>): List<OcrTextToken> {
    if (rawTokens.isEmpty()) return emptyList()
    val result = mutableListOf<OcrTextToken>()
    var current: OcrTextToken? = null

    for (token in rawTokens) {
        val prev = current
        if (prev == null) {
            current = token
            continue
        }

        val prevEndsThai = prev.text.isNotEmpty() && prev.text.last() in '\u0E00'..'\u0E7F'
        val currStartsThai = token.text.isNotEmpty() && token.text.first() in '\u0E00'..'\u0E7F'
        val sameLine = prev.lineIndex == token.lineIndex
        val horizontalGap = (token.bounds.left - prev.bounds.right).coerceAtLeast(0)
        val prevHeight = prev.bounds.bottom - prev.bounds.top
        val tokenHeight = token.bounds.bottom - token.bounds.top
        val avgHeight = ((prevHeight + tokenHeight) / 2).coerceAtLeast(1)
        val isCloseGap = horizontalGap <= avgHeight * 0.45f
        val isShortFragment = prev.text.length <= 2 || token.text.length <= 2


        if (sameLine && prevEndsThai && currStartsThai && (isCloseGap || isShortFragment)) {
            val mergedText = normalizeThaiOcrText("${prev.text}${token.text}")
            val mergedBounds = Rect(
                minOf(prev.bounds.left, token.bounds.left),
                minOf(prev.bounds.top, token.bounds.top),
                maxOf(prev.bounds.right, token.bounds.right),
                maxOf(prev.bounds.bottom, token.bounds.bottom),
            )
            current = OcrTextToken(mergedText, mergedBounds, prev.lineIndex)
        } else {
            val normalizedPrevText = normalizeThaiOcrText(prev.text)
            result.add(if (normalizedPrevText == prev.text) prev else prev.copy(text = normalizedPrevText))
            current = token
        }
    }

    current?.let { last ->
        val normalized = normalizeThaiOcrText(last.text)
        result.add(if (normalized == last.text) last else last.copy(text = normalized))
    }

    return result
}

private val THAI_COMBINING_BEFORE_SPACE = Regex("[ \\t]+(?=[\\u0E30-\\u0E3A\\u0E45\\u0E47-\\u0E4E\\u0E46\\u0E2F])")
private val THAI_LEADING_VOWEL_AFTER_SPACE = Regex("(?<=[\\u0E40-\\u0E44])[ \\t]+(?=[\\u0E01-\\u0E2E])")
private val THAI_INTER_CHARACTER_SPACE = Regex("(?<=[\\u0E00-\\u0E7F])[ \\t]+(?=[\\u0E00-\\u0E7F])")

