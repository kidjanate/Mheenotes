package com.mheenotes.app.editor

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AutomaticTextRecognizerTest {
    @Test
    fun latinTextPrefersLatinModel() {
        assertPreferred(OcrScript.Latin, "Résumé déjà vu")
        assertPreferred(OcrScript.Latin, "Meeting notes for tomorrow")
        assertPreferred(OcrScript.Latin, "The quick brown fox jumps over the lazy dog")
    }

    @Test
    fun englishTextBeatsThaiHallucination() {
        val englishText = "Project Meeting Agenda 2026"
        val thaiHallucination = "ด บ ย อ ง ะ ก"

        val englishScore = score(OcrScript.Latin, englishText)
        val thaiScore = score(OcrScript.Thai, thaiHallucination)

        assertTrue("English text score ($englishScore) should beat Thai hallucination ($thaiScore)", englishScore > thaiScore)
        assertTrue("Thai hallucination quality should be low", calculateThaiQuality(thaiHallucination) < 0.20)
        assertTrue("English quality should be high", calculateLatinQuality(englishText) > 0.70)
    }

    @Test
    fun chineseTextPrefersCjkModelOverLatin() {
        val text = "中文识别测试"

        assertTrue(score(OcrScript.Chinese, text) > score(OcrScript.Latin, text))
        assertTrue(score(OcrScript.Chinese, text) > score(OcrScript.Japanese, text))
    }

    @Test
    fun japaneseKanaPrefersJapaneseModel() {
        assertPreferred(OcrScript.Japanese, "ノートを読み取る")
    }

    @Test
    fun devanagariTextPrefersDevanagariModel() {
        assertPreferred(OcrScript.Devanagari, "हिन्दी पाठ पहचान")
    }

    @Test
    fun koreanTextPrefersKoreanModel() {
        assertPreferred(OcrScript.Korean, "한국어 텍스트 인식")
    }

    @Test
    fun thaiTextPrefersThaiModel() {
        assertPreferred(OcrScript.Thai, "อ่านข้อความภาษาไทย")
    }

    @Test
    fun thaiCandidateBeatsLatinHallucination() {
        val thaiText = "อ่านข้อความภาษาไทย"
        val latinHallucination = "n 1 o 1 a n n a l u c o"

        val thaiScore = score(OcrScript.Thai, thaiText)
        val latinScore = score(OcrScript.Latin, latinHallucination)

        assertTrue("Thai score ($thaiScore) should beat Latin hallucination ($latinScore)", thaiScore > latinScore)
    }

    @Test
    fun thaiCandidateRequiresActualThaiText() {
        assertTrue(isReliableThaiOcr("อ่านข้อความภาษาไทย", meanConfidence = .72f))
        assertTrue(!isReliableThaiOcr("This is English text", meanConfidence = .95f))
        assertTrue(!isReliableThaiOcr("ภาษาไทย", meanConfidence = .2f))
    }

    @Test
    fun detectsLanguageTagsCorrectly() {
        assertEquals("th", detectLanguageTag(OcrScript.Thai, "อ่านข้อความภาษาไทย"))
        assertEquals("th+en", detectLanguageTag(OcrScript.Thai, "อ่านข้อความภาษาไทย Chapter 1 Overview"))
        assertEquals("ja", detectLanguageTag(OcrScript.Japanese, "ノートを読み取る"))
        assertEquals("ja+en", detectLanguageTag(OcrScript.Japanese, "ノートを読み取る Version 2.0"))
        assertEquals("zh", detectLanguageTag(OcrScript.Chinese, "中文识别测试"))
        assertEquals("zh+en", detectLanguageTag(OcrScript.Chinese, "中文识别测试 Project Alpha"))
        assertEquals("ko", detectLanguageTag(OcrScript.Korean, "한국어 텍스트 인식"))
        assertEquals("hi", detectLanguageTag(OcrScript.Devanagari, "हिन्दी पाठ पहचान"))
        assertEquals("en", detectLanguageTag(OcrScript.Latin, "The quick brown fox jumps over the lazy dog"))
        assertEquals("es", detectLanguageTag(OcrScript.Latin, "El rápido zorro marrón salta sobre el perro perezoso"))
        assertEquals("fr", detectLanguageTag(OcrScript.Latin, "Le renard brun rapide saute par-dessus le chien paresseux"))
        assertEquals("de", detectLanguageTag(OcrScript.Latin, "Der schnelle braune Fuchs springt über den faulen Hund"))
    }

    @Test
    fun languageDisplayNameReturnsHumanReadableNames() {
        val englishResult = AutomaticOcrResult("Hello world", emptyList(), OcrScript.Latin, "en")
        assertEquals("English", englishResult.languageDisplayName(Locale.ENGLISH))

        val thaiResult = AutomaticOcrResult("สวัสดีครับ", emptyList(), OcrScript.Thai, "th")
        assertEquals("Thai", thaiResult.languageDisplayName(Locale.ENGLISH))

        val mixedThaiEnglishResult = AutomaticOcrResult("บทเรียน Chapter 1", emptyList(), OcrScript.Thai, "th+en")
        assertEquals("Thai + English", mixedThaiEnglishResult.languageDisplayName(Locale.ENGLISH))

        val japaneseResult = AutomaticOcrResult("日本語", emptyList(), OcrScript.Japanese, "ja")
        assertEquals("Japanese", japaneseResult.languageDisplayName(Locale.ENGLISH))
    }

    @Test
    fun cleanOcrTextNormalizesSpacingAndNewlines() {
        val rawThai = "ภ า ษ า ไ ท ย\r\n\r\n\r\nท ด ส อ บ   "
        val cleanedThai = cleanOcrText(OcrScript.Thai, rawThai)
        assertEquals("ภาษาไทย\n\nทดสอบ", cleanedThai)

        val rawLatin = "Line 1   \r\n\r\n\r\nLine 2   "
        val cleanedLatin = cleanOcrText(OcrScript.Latin, rawLatin)
        assertEquals("Line 1\n\nLine 2", cleanedLatin)
    }

    @Test
    fun fusesMixedThaiAndLatinTokensCorrectly() {
        val thaiCandidate = AutomaticTextRecognizer.Candidate(
            script = OcrScript.Thai,
            text = "บทที่ 1",
            tokens = listOf(
                OcrTextToken("บทที่", Rect(10, 10, 60, 30), lineIndex = 0),
                OcrTextToken("1", Rect(65, 10, 75, 30), lineIndex = 0),
            ),
            meanConfidence = 0.85f,
            languageTag = "th",
        )

        val latinCandidate = AutomaticTextRecognizer.Candidate(
            script = OcrScript.Latin,
            text = "Introduction to Computer Science",
            tokens = listOf(
                OcrTextToken("Introduction", Rect(80, 10, 170, 30), lineIndex = 0),
                OcrTextToken("to", Rect(175, 10, 190, 30), lineIndex = 0),
                OcrTextToken("Computer", Rect(195, 10, 260, 30), lineIndex = 0),
                OcrTextToken("Science", Rect(265, 10, 320, 30), lineIndex = 0),
            ),
            meanConfidence = 0.90f,
            languageTag = "en",
        )

        val fused = fuseThaiAndLatin(thaiCandidate, latinCandidate)
        assertEquals("บทที่ 1 Introduction to Computer Science", fused.text)
        assertEquals("th+en", fused.languageTag)
        assertEquals(6, fused.tokens.size)
    }

    private fun assertPreferred(expected: OcrScript, text: String) {
        val expectedScore = score(expected, text)
        OcrScript.entries.filterNot { it == expected }.forEach { other ->
            assertTrue("Expected $expected to beat $other for $text", expectedScore > score(other, text))
        }
    }

    private fun score(script: OcrScript, text: String): Double =
        automaticOcrScore(script, text, meanConfidence = .8f)
}


