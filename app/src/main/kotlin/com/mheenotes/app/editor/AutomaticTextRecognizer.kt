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
    Latin("English"),
    Chinese("Chinese"),
    Devanagari("Hindi"),
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
        val tag = languageTag?.takeUnless { it == "und" || it.isBlank() }
            ?: detectLanguageTag(script, text)

        if (tag.contains('+')) {
            val parts = tag.split('+')
            val names = parts.mapNotNull { part ->
                Locale.forLanguageTag(part).getDisplayLanguage(locale).takeIf(String::isNotBlank)
            }
            if (names.isNotEmpty()) {
                return names.joinToString(" + ")
            }
        }

        val language = Locale.forLanguageTag(tag).getDisplayLanguage(locale).takeIf(String::isNotBlank)
        return language ?: script.displayName
    }
}

/** Runs bundled on-device script models, fuses multi-language content, and keeps the best result. */
internal class AutomaticTextRecognizer(context: Context) : AutoCloseable {
    private data class RecognizerSpec(
        val script: OcrScript,
        val recognizer: TextRecognizer,
    )

    internal data class Candidate(
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
                    val latinCandidate = candidates.firstOrNull { it.script == OcrScript.Latin }
                    val thaiCandidate = candidates.firstOrNull { it.script == OcrScript.Thai }

                    val latinQuality = latinCandidate?.let { calculateLatinQuality(it.text) } ?: 0.0
                    val thaiQuality = thaiCandidate?.let { calculateThaiQuality(it.text) } ?: 0.0

                    val best: Candidate? = if (thaiCandidate != null && latinCandidate != null &&
                        hasSubstantialThai(thaiCandidate.text) && hasSubstantialLatin(latinCandidate.text)
                    ) {
                        fuseThaiAndLatin(thaiCandidate, latinCandidate)
                    } else {
                        val eligibleCandidates = candidates.filter { value ->
                            when (value.script) {
                                OcrScript.Thai -> thaiQuality >= 0.25 && isReliableThaiOcr(value.text, value.meanConfidence)
                                OcrScript.Latin -> true
                                else -> true
                            }
                        }
                        eligibleCandidates.maxByOrNull { value ->
                            automaticOcrScore(value.script, value.text, value.meanConfidence)
                        }
                    }



                    completed = if (best != null) {
                        val detectedTag = best.languageTag ?: detectLanguageTag(best.script, best.text)
                        val cleanedText = cleanOcrText(best.script, best.text)
                        Result.success(
                            AutomaticOcrResult(
                                text = cleanedText,
                                tokens = best.tokens,
                                script = best.script,
                                languageTag = detectedTag,
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
                            languageTag = dominantLanguageTag(text) ?: detectLanguageTag(spec.script, text.text),
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

internal fun cleanOcrText(script: OcrScript, rawText: String): String {
    if (rawText.isBlank()) return ""
    var text = rawText.replace("\r\n", "\n").replace('\r', '\n')
    if (script == OcrScript.Thai || text.any { it.isThai() }) {
        text = normalizeThaiOcrText(text)
    }
    return text.lines()
        .map { it.trimEnd() }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

internal fun detectLanguageTag(script: OcrScript, text: String): String = when (script) {
    OcrScript.Thai -> {
        if (hasSubstantialLatin(text)) "th+en" else "th"
    }
    OcrScript.Japanese -> {
        if (hasSubstantialLatin(text)) "ja+en" else "ja"
    }
    OcrScript.Korean -> {
        if (hasSubstantialLatin(text)) "ko+en" else "ko"
    }
    OcrScript.Chinese -> {
        if (hasSubstantialLatin(text)) "zh+en" else "zh"
    }
    OcrScript.Devanagari -> {
        if (hasSubstantialLatin(text)) "hi+en" else "hi"
    }
    OcrScript.Latin -> detectLatinLanguageTag(text)
}

internal fun hasSubstantialThai(text: String): Boolean {
    val thaiCount = text.count { it.isThai() }
    return thaiCount >= 3 && calculateThaiQuality(text) >= 0.25
}

internal fun hasSubstantialLatin(text: String): Boolean {
    val latinWords = text.split(Regex("[^a-zA-Z\\u00C0-\\u024F]+")).filter { it.length >= 2 }
    return latinWords.isNotEmpty() && calculateLatinQuality(text) >= 0.25
}

internal fun calculateLatinQuality(text: String): Double {
    if (text.isBlank()) return 0.0
    val rawWords = text.split(Regex("[^a-zA-Z\\u00C0-\\u024F]+")).filter { it.isNotBlank() }
    if (rawWords.isEmpty()) return 0.0

    var totalLatinChars = 0
    var validWordChars = 0
    var englishStopwordHits = 0

    for (word in rawWords) {
        totalLatinChars += word.length
        val lower = word.lowercase(Locale.ROOT)
        val hasVowel = lower.any { it in "aeiouy\u00E0-\u00FF" }
        if (word.length >= 2 && hasVowel) {
            validWordChars += word.length
        }
        if (lower in ALL_LATIN_STOPWORDS) {
            englishStopwordHits++
        }
    }

    if (totalLatinChars == 0) return 0.0
    val validWordRatio = validWordChars.toDouble() / totalLatinChars
    val stopwordBonus = (englishStopwordHits * 0.15).coerceAtMost(0.4)
    return (validWordRatio * 0.8 + stopwordBonus).coerceIn(0.0, 1.0)

}

internal fun calculateThaiQuality(text: String): Double {
    if (text.isBlank()) return 0.0
    var thaiChars = 0
    var thaiVowelsAndTones = 0

    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (codePoint.isThai()) {
            thaiChars++
            if (codePoint.isThaiVowelOrTone()) {
                thaiVowelsAndTones++
            }
        }
        index += Character.charCount(codePoint)
    }

    if (thaiChars < 2) return 0.0

    val vowelToneRatio = thaiVowelsAndTones.toDouble() / thaiChars

    var thaiWordHits = 0
    for (word in COMMON_THAI_WORDS) {
        if (text.contains(word)) thaiWordHits++
    }

    val baseScore = when {
        vowelToneRatio >= 0.25 -> 0.4
        vowelToneRatio >= 0.15 -> 0.15
        else -> 0.05
    }

    val wordBonus = (thaiWordHits * 0.25).coerceAtMost(0.6)
    return (baseScore + wordBonus).coerceIn(0.0, 1.0)
}

internal fun fuseThaiAndLatin(
    thaiCandidate: AutomaticTextRecognizer.Candidate,
    latinCandidate: AutomaticTextRecognizer.Candidate,
): AutomaticTextRecognizer.Candidate {
    val thaiTokens = thaiCandidate.tokens.map { token ->
        if (token.text.any { it.isThai() }) {
            val norm = normalizeThaiOcrText(token.text)
            if (norm == token.text) token else token.copy(text = norm)
        } else {
            token
        }
    }

    val latinTokens = latinCandidate.tokens.filter { isGenuineLatinToken(it.text) }

    val combinedTokens = mutableListOf<OcrTextToken>()
    combinedTokens.addAll(thaiTokens)

    for (latinTok in latinTokens) {
        val overlappingThai = thaiTokens.firstOrNull { thaiTok ->
            tokensOverlap(thaiTok.bounds, latinTok.bounds)
        }
        if (overlappingThai == null) {
            combinedTokens.add(latinTok)
        } else if (!overlappingThai.text.any { it.isThai() }) {
            combinedTokens.remove(overlappingThai)
            combinedTokens.add(latinTok)
        }
    }

    if (combinedTokens.isEmpty()) {
        return thaiCandidate
    }

    val tokensByLine = combinedTokens.groupBy { it.lineIndex }.toSortedMap()
    val indexedTokens = mutableListOf<OcrTextToken>()
    val textLines = mutableListOf<String>()

    var currentLineIndex = 0
    for ((_, rawLineTokens) in tokensByLine) {
        val lineTokens = rawLineTokens.sortedBy { it.bounds.left }
        val lineBuilder = StringBuilder()
        var prevToken: OcrTextToken? = null

        lineTokens.forEach { token ->
            val updatedToken = token.copy(lineIndex = currentLineIndex)
            indexedTokens.add(updatedToken)

            val prev = prevToken
            if (prev != null) {
                val prevEndsThai = prev.text.isNotEmpty() && prev.text.last().isThai()
                val currStartsThai = token.text.isNotEmpty() && token.text.first().isThai()
                val gap = (token.bounds.left - prev.bounds.right).coerceAtLeast(0)
                val prevHeight = prev.bounds.bottom - prev.bounds.top
                val tokenHeight = token.bounds.bottom - token.bounds.top
                val avgHeight = ((prevHeight + tokenHeight) / 2).coerceAtLeast(1)

                if (prevEndsThai && currStartsThai) {
                    if (gap > avgHeight * 0.45f) {
                        lineBuilder.append(' ')
                    }
                } else {
                    lineBuilder.append(' ')
                }
            }
            lineBuilder.append(token.text)
            prevToken = token
        }
        textLines.add(lineBuilder.toString().trim())
        currentLineIndex++
    }

    val fusedText = textLines.joinToString("\n").trim()
    val avgConfidence = (thaiCandidate.meanConfidence + latinCandidate.meanConfidence) / 2f

    return AutomaticTextRecognizer.Candidate(
        script = OcrScript.Thai,
        text = fusedText,
        tokens = indexedTokens,
        meanConfidence = avgConfidence,
        languageTag = "th+en",
    )
}


private fun tokensOverlap(a: Rect, b: Rect): Boolean {
    val intersectionLeft = maxOf(a.left, b.left)
    val intersectionTop = maxOf(a.top, b.top)
    val intersectionRight = minOf(a.right, b.right)
    val intersectionBottom = minOf(a.bottom, b.bottom)

    if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) return false
    val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
    val aArea = (a.right - a.left) * (a.bottom - a.top)
    val bArea = (b.right - b.left) * (b.bottom - b.top)
    val minArea = minOf(aArea, bArea).coerceAtLeast(1)
    return intersectionArea.toFloat() / minArea > 0.40f
}


private fun isGenuineLatinToken(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.all { it.isDigit() || it in ":.,;!?()[]{}'\"/\\-_+=#%&@$" }) return true

    val latinLetters = trimmed.filter { it.isLetter() && it.code.isLatin() }
    if (latinLetters.isEmpty()) return false

    val lower = latinLetters.lowercase(Locale.ROOT)
    if (lower == "a" || lower == "i") return true
    if (lower.length >= 2 && lower.any { it in "aeiouy\u00E0-\u00FF" }) return true
    if (lower.length in 2..4) return true

    return false
}

private fun detectLatinLanguageTag(text: String): String {
    val words = text.lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}]+"))
        .filter { it.length >= 2 }
    if (words.isEmpty()) return "en"

    if (text.any { it in "đươăêôâấầẩẫậắằẳẵặếềểễệốồổỗộứừửữự" }) return "vi"

    var enHits = 0
    var esHits = 0
    var frHits = 0
    var deHits = 0
    var itHits = 0
    var ptHits = 0
    var idHits = 0

    for (word in words) {
        if (word in ENGLISH_STOPWORDS) enHits++
        if (word in SPANISH_STOPWORDS) esHits++
        if (word in FRENCH_STOPWORDS) frHits++
        if (word in GERMAN_STOPWORDS) deHits++
        if (word in ITALIAN_STOPWORDS) itHits++
        if (word in PORTUGUESE_STOPWORDS) ptHits++
        if (word in INDONESIAN_STOPWORDS) idHits++
    }

    val scores = listOf(
        "en" to enHits,
        "es" to esHits,
        "fr" to frHits,
        "de" to deHits,
        "it" to itHits,
        "pt" to ptHits,
        "id" to idHits,
    )
    val best = scores.maxByOrNull { it.second }
    return if (best != null && best.second > 0) best.first else "en"
}

private val ALL_LATIN_STOPWORDS = setOf(
    "the", "and", "of", "to", "in", "is", "that", "for", "with", "on", "as", "are",
    "this", "by", "from", "at", "or", "be", "an", "was", "have", "not", "but", "you",
    "all", "can", "your", "we", "our", "will", "they", "has", "more", "if",
    "about", "which", "when", "there", "note", "notes", "page", "text", "file", "user",
    "data", "system", "app", "view", "edit", "open", "save", "name", "date", "time",
    "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por",
    "un", "para", "con", "no", "una", "su", "al", "lo", "como", "mas", "pero",
    "der", "die", "und", "in", "den", "von", "zu", "das", "mit", "sich", "des",
)

private val ENGLISH_STOPWORDS = setOf(
    "the", "and", "of", "to", "in", "is", "that", "for", "with", "on", "as", "are",
    "this", "by", "from", "at", "or", "be", "an", "was", "have", "not", "but", "you",
    "all", "can", "your", "we", "our", "will", "they", "has", "more", "if",
    "about", "which", "when", "there", "note", "notes", "page", "text",
)

private val SPANISH_STOPWORDS = setOf(
    "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por",
    "un", "para", "con", "no", "una", "su", "al", "lo", "como", "mas", "pero",
    "sus", "le", "ya", "o", "fue", "este", "ha", "esta", "son",
)

private val FRENCH_STOPWORDS = setOf(
    "de", "la", "le", "et", "les", "des", "en", "un", "du", "une", "est", "pour",
    "dans", "qui", "au", "sur", "pas", "plus", "par", "ce", "avec", "sont",
    "ne", "se", "que", "ou", "aux", "cette", "nous", "vous",
)

private val GERMAN_STOPWORDS = setOf(
    "der", "die", "und", "in", "den", "von", "zu", "das", "mit", "sich", "des",
    "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine", "als", "auch",
    "es", "an", "werden", "aus", "er", "hat", "dass", "sie", "nach", "wir",
)

private val ITALIAN_STOPWORDS = setOf(
    "di", "e", "il", "la", "che", "in", "per", "un", "del", "da", "una", "non",
    "si", "dei", "sono", "ed", "con", "le", "degli", "alla", "nel", "come",
    "delle", "piu", "gli", "ma", "anche", "questo", "della",
)

private val PORTUGUESE_STOPWORDS = setOf(
    "de", "a", "o", "que", "e", "do", "da", "em", "um", "para", "com", "nao",
    "uma", "os", "no", "se", "na", "por", "mais", "as", "dos", "como", "mas",
    "foi", "ao", "ele", "das", "tem", "seu", "sua", "pelo",
)

private val INDONESIAN_STOPWORDS = setOf(
    "dan", "yang", "di", "ini", "dari", "dalam", "untuk", "tidak", "dengan",
    "ke", "akan", "pada", "juga", "saya", "adalah", "itu", "bahwa", "oleh",
    "dapat", "mereka", "ada", "sudah", "bisa",
)

private val COMMON_THAI_WORDS = listOf(
    "การ", "ความ", "ที่", "ใน", "และ", "เป็น", "มี", "ได้", "ให้", "จะ",
    "ของ", "กับ", "โดย", "ว่า", "จาก", "นี้", "ผู้", "วัน", "บท", "ข้อ",
    "สวัสดี", "ครับ", "ค่ะ", "ภาษา", "ไทย", "เรียน", "ทดสอบ", "ข้อมูล",
    "งาน", "ทำ", "ไป", "มา", "ไม่", "เรื่อง", "หน้า", "หมาย", "ตอน",
)

internal fun isReliableThaiOcr(text: String, meanConfidence: Float): Boolean {
    if (meanConfidence < .25f) return false
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
            if (codePoint.isThai()) thaiCharacterCount++
        }
        index += Character.charCount(codePoint)
    }
    val thaiCoverage = if (scriptCharacterCount == 0) 0f else {
        thaiCharacterCount.toFloat() / scriptCharacterCount
    }
    return thaiCharacterCount >= 2 && thaiCoverage >= .25f
}

internal fun automaticOcrScore(script: OcrScript, text: String, meanConfidence: Float): Double {
    if (text.isBlank()) return 0.0

    var visibleCount = 0
    var letterCount = 0
    var scriptMatchingLetters = 0
    var kanaCount = 0
    var hanCount = 0
    var hangulCount = 0
    var devanagariCount = 0
    var thaiCount = 0
    var latinCount = 0

    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (!Character.isWhitespace(codePoint)) visibleCount++
        val characterType = Character.getType(codePoint)
        val isTextCharacter = Character.isLetter(codePoint) ||
            characterType == Character.NON_SPACING_MARK.toInt() ||
            characterType == Character.COMBINING_SPACING_MARK.toInt()
        if (isTextCharacter) {
            letterCount++
            if (script.matches(codePoint)) scriptMatchingLetters++
            if (codePoint.isKana()) kanaCount++
            if (codePoint.isHan()) hanCount++
            if (codePoint.isHangul()) hangulCount++
            if (codePoint.isDevanagari()) devanagariCount++
            if (codePoint.isThai()) thaiCount++
            if (codePoint.isLatin()) latinCount++
        }
        index += Character.charCount(codePoint)
    }

    if (visibleCount == 0 || letterCount == 0) return 0.0

    val confidence = meanConfidence.coerceIn(0f, 1f)

    return when (script) {
        OcrScript.Thai -> {
            val quality = calculateThaiQuality(text)
            if (thaiCount < 2 || quality < 0.20) 0.0
            else {
                val thaiRatio = thaiCount.toDouble() / letterCount
                thaiCount * (2.0 + quality * 4.0) * (1.0 + thaiRatio * 2.0 + confidence)
            }
        }
        OcrScript.Korean -> {
            if (hangulCount < 2) 0.0
            else {
                val hangulRatio = hangulCount.toDouble() / letterCount
                hangulCount * 8.0 * (1.0 + hangulRatio * 2.0 + confidence)
            }
        }
        OcrScript.Japanese -> {
            if (kanaCount >= 1) {
                val jpLetters = kanaCount + hanCount
                val jpRatio = jpLetters.toDouble() / letterCount
                (kanaCount * 6.0 + hanCount * 3.0) * (1.0 + jpRatio * 2.0 + confidence)
            } else if (hanCount >= 2) {
                hanCount * 2.0 * (1.0 + confidence)
            } else 0.0
        }
        OcrScript.Chinese -> {
            if (hanCount < 2 || kanaCount >= 1) 0.0
            else {
                val hanRatio = hanCount.toDouble() / letterCount
                hanCount * 6.0 * (1.0 + hanRatio * 2.0 + confidence)
            }
        }
        OcrScript.Devanagari -> {
            if (devanagariCount < 2) 0.0
            else {
                val devRatio = devanagariCount.toDouble() / letterCount
                devanagariCount * 8.0 * (1.0 + devRatio * 2.0 + confidence)
            }
        }
        OcrScript.Latin -> {
            if (latinCount == 0) 0.0
            else {
                val latinRatio = latinCount.toDouble() / letterCount
                val quality = calculateLatinQuality(text)
                latinCount * (1.0 + quality * 4.0) * (1.0 + latinRatio * 2.0 + confidence)
            }
        }
    }
}

private fun OcrScript.matches(codePoint: Int): Boolean = when (this) {
    OcrScript.Latin -> codePoint.isLatin()
    OcrScript.Chinese -> codePoint.isHan()
    OcrScript.Devanagari -> codePoint.isDevanagari()
    OcrScript.Japanese -> codePoint.isHan() || codePoint.isKana()
    OcrScript.Korean -> codePoint.isHangul()
    OcrScript.Thai -> codePoint.isThai()
}

private fun Int.isKana(): Boolean = this in 0x3040..0x30FF ||
    this in 0x31F0..0x31FF ||
    this in 0xFF65..0xFF9F ||
    this in 0x1B000..0x1B16F

private fun Int.isHangul(): Boolean = this in 0xAC00..0xD7AF ||
    this in 0x1100..0x11FF ||
    this in 0x3130..0x318F ||
    this in 0xA960..0xA97F ||
    this in 0xFFA0..0xFFDC

private fun Int.isDevanagari(): Boolean = this in 0x0900..0x097F ||
    this in 0xA8E0..0xA8FF ||
    this in 0x11B00..0x11B5F

private fun Int.isThai(): Boolean = this in 0x0E00..0x0E7F

private fun Char.isThai(): Boolean = this in '\u0E00'..'\u0E7F'

private fun Int.isThaiVowelOrTone(): Boolean =
    this in 0x0E30..0x0E3A || this in 0x0E40..0x0E4E

private fun Int.isLatin(): Boolean = this in 0x0041..0x005A ||
    this in 0x0061..0x007A ||
    this in 0x00C0..0x024F ||
    this in 0x1E00..0x1EFF ||
    this in 0xAB30..0xAB6F ||
    this in 0xFF21..0xFF5A

private fun Int.isHan(): Boolean = this in 0x3400..0x4DBF ||
    this in 0x4E00..0x9FFF ||
    this in 0xF900..0xFAFF ||
    this in 0x20000..0x2EBEF ||
    this in 0x30000..0x3134F


