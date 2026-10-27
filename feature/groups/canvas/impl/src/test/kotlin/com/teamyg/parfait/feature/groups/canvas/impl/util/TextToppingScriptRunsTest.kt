package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript.CHINESE as C
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript.DEFAULT as D
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript.JAPANESE as J
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScriptRun
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextToppingScriptRunsTest {
    private fun runsOf(text: String) = textToppingScriptRuns(text).map { it.script to text.substring(it.start, it.end) }

    @Test
    fun japaneseSpecExample_allJapanese() {
        val text = "最高デザイナー ソフィ"
        assertEquals(listOf(J to text), runsOf(text))
    }

    @Test
    fun chineseSpecExample_allChinese() {
        val text = "頂級設計師徐輝"
        assertEquals(listOf(C to text), runsOf(text))
    }

    @Test
    fun koreanEnglishDigits_allDefault() {
        val text = "파르페 Parfait 2026"
        assertEquals(listOf(D to text), runsOf(text))
    }

    @Test
    fun kanaPresent_chineseCharacterTriesJapaneseFirst() {
        val text = "学校へ行く"
        assertEquals(listOf(J to text), runsOf(text))
    }

    @Test
    fun noKana_chineseCharacterTriesChineseFirst() {
        assertEquals(listOf(C to "學校 ", J to "学", C to "校"), runsOf("學校 学校"))
    }

    @Test
    fun hangulWithChineseCharacter_chineseCharacterTriesChineseFirst() {
        assertEquals(
            listOf(D to "오늘 ", C to "漢字 ", D to "공부"),
            runsOf("오늘 漢字 공부"),
        )
    }

    @Test
    fun hanMissingFromBoth_keepsFirstPriority() {
        assertEquals(listOf(C to "这"), runsOf("这"))
        assertEquals(listOf(J to "这は"), runsOf("这は"))
    }

    @Test
    fun kanaWithHangul_hangulIsDefault() {
        assertEquals(listOf(J to "パフェ ", D to "파르페"), runsOf("パフェ 파르페"))
    }

    @Test
    fun digitsInsideJapanese_areDefault() {
        assertEquals(listOf(J to "パフェ", D to "2", J to "個"), runsOf("パフェ2個"))
    }

    @Test
    fun leadingNeutral_followsFirstStrongRun() {
        assertEquals(listOf(C to "「最高」"), runsOf("「最高」"))
        assertEquals(listOf(J to "  パフェ"), runsOf("  パフェ"))
    }

    @Test
    fun allNeutral_isDefault() {
        val text = "!? 🎉"
        assertEquals(listOf(D to text), runsOf(text))
    }

    @Test
    fun emojiZwjSequence_staysInOneRun() {
        val text = "パフェ👨‍👩‍👧"
        assertEquals(listOf(J to text), runsOf(text))
    }

    @Test
    fun supplementaryChineseCharacter_indicesAreUtf16() {
        assertEquals(listOf(TextToppingScriptRun(J, 0, 3)), textToppingScriptRuns("𡨚パ"))
        assertEquals(listOf(TextToppingScriptRun(J, 0, 2)), textToppingScriptRuns("𡨚"))
    }

    @Test
    fun kanaPresent_chineseCharacterInBothFonts_isJapanese() {
        val text = "學パ"
        assertEquals(listOf(J to text), runsOf(text))
    }

    @Test
    fun kanaPresent_chineseCharacterOnlyInTw_isChinese() {
        assertEquals(listOf(C to "丟", J to "パ"), runsOf("丟パ"))
    }

    @Test
    fun iterationMarks_followChineseCharacterOrder() {
        assertEquals(listOf(J to "人々は"), runsOf("人々は"))
        assertEquals(listOf(C to "人々"), runsOf("人々"))
    }

    @Test
    fun kanaExtensions_areJapanese() {
        assertEquals(listOf(J to "ｶﾀｶﾅ"), runsOf("ｶﾀｶﾅ"))
        assertEquals(listOf(J to "ㇰ"), runsOf("ㇰ"))
    }

    @Test
    fun fullwidthDigitsAndLatin_areNeutral() {
        assertEquals(listOf(J to "パフェ２個"), runsOf("パフェ２個"))
        assertEquals(listOf(C to "２個"), runsOf("２個"))
        assertEquals(listOf(D to "Ａ"), runsOf("Ａ"))
    }

    @Test
    fun empty_noRuns() {
        assertEquals(emptyList(), textToppingScriptRuns(""))
    }

    @Test
    fun runs_areContiguousAndMerged() {
        val inputs = listOf(
            "最高デザイナー ソフィ", "頂級設計師徐輝", "파르페 Parfait 2026", "学校へ行く", "學校 学校",
            "오늘 漢字 공부", "这", "这は", "パフェ 파르페", "パフェ2個", "「最高」", "  パフェ", "!? 🎉",
            "パフェ👨‍👩‍👧", "𡨚パ", "人々は", "ｶﾀｶﾅ", "パフェ２個",
        )
        for (text in inputs) {
            val runs = textToppingScriptRuns(text)
            assertEquals(0, runs.first().start, text)
            assertEquals(text.length, runs.last().end, text)
            runs.zipWithNext().forEach { (a, b) ->
                assertEquals(a.end, b.start, text)
                assertTrue(a.script != b.script, text)
            }
        }
    }
}
