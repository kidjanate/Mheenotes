package com.mheenotes.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

internal enum class OcrScript(val displayName: String) {
    Latin("Latin-based language"),
    Chinese("Chinese"),
    Devanagari("Devanagari language"),
    Japanese("Japanese"),
    Korean("Korean"),
    Thai("Thai"),
}

internal data class OcrTextToken(
    val text: String,
    val bounds: Rect,
    val lineIndex: Int,
)

internal data class AutomaticOcrResult(
    val text: String,
    val tokens: List<OcrTextToken>,
    val script: OcrScript,
    val languageTag: String?,
) {
    fun languageDisplayName(locale: Locale = Locale.getDefault()): String {
        val tag = languageTag?.takeUnless { it == "und" }.orEmpty()
        val language = tag.takeIf(String::isNotBlank)
            ?.let(Locale::forLanguageTag)
            ?.getDisplayLanguage(locale)
            ?.takeIf(String::isNotBlank)
        return language ?: script.displayName
    }
}

/** Runs every bundled on-device script model and keeps the most plausible recognized result. */
internal class AutomaticTextRecognizer(context: Context) : AutoCloseable {
    private data class RecognizerSpec(
        val script: OcrScript,
        val recognizer: TextRecognizer,
    )

    private data class Candidate(
        val script: OcrScript,
        val text: String,
        val tokens: List<OcrTextToken>,
        val meanConfidence: Float,
        val languageTag: String?,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val thaiRecognizer = ThaiTextRecognizer(context.applicationContext)

    private val recognizers = listOf(
        RecognizerSpec(
            OcrScript.Latin,
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        ),
        RecognizerSpec(
            OcrScript.Chinese,
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        ),
        RecognizerSpec(
            OcrScript.Devanagari,
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
        ),
        RecognizerSpec(
            OcrScript.Japanese,
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
        ),
        RecognizerSpec(
            OcrScript.Korean,
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        ),
    )

    fun process(bitmap: Bitmap, callback: (Result<AutomaticOcrResult>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val lock = Any()
        val candidates = mutableListOf<Candidate>()
        val failures = mutableListOf<Throwable>()
        var remaining = recognizers.size + 1

        fun finish(candidate: Candidate?, failure: Throwable?) {
            var completed: Result<AutomaticOcrResult>? = null
            synchronized(lock) {
                candidate?.let(candidates::add)
                failure?.let(failures::add)
                remaining--
                if (remaining == 0) {
                    val eligibleCandidates = candidates.filter { value ->
                        value.script != OcrScript.Thai || isReliableThaiOcr(value.text, value.meanConfidence)
                    }
                    val best = eligibleCandidates.maxByOrNull { value ->
                        automaticOcrScore(value.script, value.text, value.meanConfidence)
                    }
                    completed = if (best != null) {
                        Result.success(
                            AutomaticOcrResult(
                                text = best.text,
                                tokens = best.tokens,
                                script = best.script,
                                languageTag = best.languageTag,
                            ),
                        )
                    } else {
                        Result.failure(failures.firstOrNull() ?: IllegalStateException("OCR failed"))
                    }
                }
            }
            completed?.let { result -> mainHandler.post { callback(result) } }
        }

        recognizers.forEach { spec ->
            spec.recognizer.process(image)
                .addOnSuccessListener { text ->
                    val confidences = text.textBlocks.asSequence()
                        .flatMap { it.lines.asSequence() }
                        .map(Text.Line::getConfidence)
                        .filter { it > 0f }
                        .toList()
                    finish(
                        Candidate(
                            script = spec.script,
                            text = text.text,
                            tokens = text.toOcrTokens(),
                            meanConfidence = confidences.takeIf(List<Float>::isNotEmpty)
                                ?.average()?.toFloat() ?: .5f,
                            languageTag = dominantLanguageTag(text),
                        ),
                        null,
                    )
                }
                .addOnFailureListener { error -> finish(null, error) }
        }
        thaiRecognizer.process(bitmap) { recognition ->
            recognition.onSuccess { result ->
                finish(
                    Candidate(
                        script = OcrScript.Thai,
                        text = result.text,
                        tokens = result.tokens,
                        meanConfidence = result.meanConfidence,
                        languageTag = "th",
                    ),
                    null,
                )
            }.onFailure { error -> finish(null, error) }
        }
    }

    override fun close() {
        recognizers.forEach { it.recognizer.close() }
        thaiRecognizer.close()
    }

    private fun dominantLanguageTag(result: Text): String? = result.textBlocks.asSequence()
        .flatMap { it.lines.asSequence() }
        .map(Text.Line::getRecognizedLanguage)
        .filter { it.isNotBlank() && it != "und" }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull(Map.Entry<String, Int>::value)
        ?.key

    private fun Text.toOcrTokens(): List<OcrTextToken> = buildList {
        var lineIndex = 0
        textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val elements = line.elements.mapNotNull { element ->
                    val elementText = element.text.trim().takeIf(String::isNotBlank)
                    val bounds = element.boundingBox
                    if (elementText != null && bounds != null && !bounds.isEmpty) {
                        OcrTextToken(elementText, Rect(bounds), lineIndex)
                    } else null
                }
                if (elements.isNotEmpty()) {
                    addAll(elements)
                } else {
                    val lineText = line.text.trim().takeIf(String::isNotBlank)
                    val bounds = line.boundingBox
                    if (lineText != null && bounds != null && !bounds.isEmpty) {
                        add(OcrTextToken(lineText, Rect(bounds), lineIndex))
                    }
                }
                lineIndex++
            }
        }
    }
}

internal fun isReliableThaiOcr(text: String, meanConfidence: Float): Boolean {
    if (meanConfidence < .35f) return false
    var thaiCharacterCount = 0
    var scriptCharacterCount = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val characterType = Character.getType(codePoint)
        val isTextCharacter = Character.isLetter(codePoint) ||
            characterType == Character.NON_SPACING_MARK.toInt() ||
            characterType == Character.COMBINING_SPACING_MARK.toInt()
        if (isTextCharacter) {
            scriptCharacterCount++
            if (codePoint in 0x0E00..0x0E4E) thaiCharacterCount++
        }
        index += Character.charCount(codePoint)
    }
    val thaiCoverage = if (scriptCharacterCount == 0) 0f else {
        thaiCharacterCount.toFloat() / scriptCharacterCount
    }
    return thaiCharacterCount >= 2 && thaiCoverage >= .35f
}

internal fun automaticOcrScore(script: OcrScript, text: String, meanConfidence: Float): Double {
    var visibleCount = 0
    var letterCount = 0
    var matchingLetterCount = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (!Character.isWhitespace(codePoint)) visibleCount++
        if (Character.isLetter(codePoint)) {
            letterCount++
            if (script.matches(codePoint)) matchingLetterCount++
        }
        index += Character.charCount(codePoint)
    }
    if (visibleCount == 0) return 0.0
    val scriptCoverage = if (letterCount == 0) 0.0 else matchingLetterCount.toDouble() / letterCount
    val confidence = meanConfidence.coerceIn(0f, 1f)
    return visibleCount * (1.0 + scriptCoverage * 3.0 + confidence)
}

private fun OcrScript.matches(codePoint: Int): Boolean = when (this) {
    OcrScript.Latin -> codePoint in 0x0041..0x007A ||
        codePoint in 0x00C0..0x024F ||
        codePoint in 0x1E00..0x1EFF ||
        codePoint in 0xAB30..0xAB6F ||
        codePoint in 0xFF21..0xFF5A
    OcrScript.Chinese -> codePoint.isHan()
    OcrScript.Devanagari -> codePoint in 0x0900..0x097F ||
        codePoint in 0xA8E0..0xA8FF ||
        codePoint in 0x11B00..0x11B5F
    OcrScript.Japanese -> codePoint.isHan() ||
        codePoint in 0x3040..0x30FF ||
        codePoint in 0x31F0..0x31FF ||
        codePoint in 0xFF65..0xFF9F ||
        codePoint in 0x1B000..0x1B16F
    OcrScript.Korean -> codePoint in 0x1100..0x11FF ||
        codePoint in 0x3130..0x318F ||
        codePoint in 0xA960..0xA97F ||
        codePoint in 0xAC00..0xD7FF ||
        codePoint in 0xFFA0..0xFFDC
    OcrScript.Thai -> codePoint in 0x0E00..0x0E7F
}

private fun Int.isHan(): Boolean = this in 0x3400..0x4DBF ||
    this in 0x4E00..0x9FFF ||
    this in 0xF900..0xFAFF ||
    this in 0x20000..0x2EBEF ||
    this in 0x30000..0x3134F
