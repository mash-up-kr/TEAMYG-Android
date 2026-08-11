package com.teamyg.parfait.core.util.jvm.extension

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharExtensionTest {
    @Test
    fun isKorean_completeSyllable_returnsTrue() {
        // Given 완성형 한글 음절
        val char = '가'

        // When isKorean 호출
        val result = char.isKorean()

        // Then 한글로 판정한다
        assertTrue(result)
    }

    @Test
    fun isKorean_lastCompleteSyllable_returnsTrue() {
        assertTrue('힣'.isKorean())
    }

    @Test
    fun isKorean_standaloneConsonant_returnsTrue() {
        assertTrue('ㄱ'.isKorean())
    }

    @Test
    fun isKorean_standaloneVowel_returnsTrue() {
        assertTrue('ㅏ'.isKorean())
    }

    @Test
    fun isKorean_latinLetter_returnsFalse() {
        assertFalse('a'.isKorean())
        assertFalse('Z'.isKorean())
    }

    @Test
    fun isKorean_digit_returnsFalse() {
        assertFalse('0'.isKorean())
    }

    @Test
    fun isKorean_whitespace_returnsFalse() {
        assertFalse(' '.isKorean())
    }

    @Test
    fun isKorean_symbol_returnsFalse() {
        assertFalse('!'.isKorean())
        assertFalse('_'.isKorean())
    }
}
