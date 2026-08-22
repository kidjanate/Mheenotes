package com.mheenotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ThaiTextRecognizerTest {
    @Test
    fun removesSystematicSpacesBetweenThaiCharacters() {
        assertEquals("ภาษาไทย", normalizeThaiOcrText("ภ า ษ า ไ ท ย"))
    }

    @Test
    fun removesSpacesAroundCombiningMarksAndToneMarks() {
        assertEquals("สวัสดีครับ", normalizeThaiOcrText("ส ว ั ส ด ี ค ร ั บ"))
    }

    @Test
    fun removesSpacesAfterLeadingVowelsAndInsideWords() {
        assertEquals("ไปเรียน", normalizeThaiOcrText("ไ ป เ ร ีย น"))
        assertEquals("การทำงานของเรา", normalizeThaiOcrText("ก าร ท ำ งาน ข อง เรา"))
    }

    @Test
    fun preservesNormalPhraseSpacing() {
        assertEquals("ภาษาไทย ทดสอบ OCR", normalizeThaiOcrText("ภาษาไทย ทดสอบ OCR"))
        assertEquals("สวัสดีครับ วันนี้อากาศดี", normalizeThaiOcrText("สวัสดีครับ วันนี้อากาศดี"))
    }

    @Test
    fun preservesThaiWithNumbersAndPunctuation() {
        assertEquals("บทที่ 1: การทดสอบ OCR", normalizeThaiOcrText("บทที่ 1: การทดสอบ OCR"))
        assertEquals("ก. ภาษาไทย\nข. ภาษาอังกฤษ", normalizeThaiOcrText("ก. ภาษาไทย\nข. ภาษาอังกฤษ"))
    }
}

