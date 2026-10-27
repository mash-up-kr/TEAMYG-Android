package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript.CHINESE as C
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript.DEFAULT as D
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript.JAPANESE as J
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextToppingScriptRunsTest {
    private fun runsOf(text: String) =
        textToppingScriptRuns(text).map { it.script to text.substring(it.start, it.end) }

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
    fun kanaPresent_hanTriesJapaneseFirst() {
        val text = "学校へ行く"
        assertEquals(listOf(J to text), runsOf(text))
    }

    @Test
    fun noKana_hanTriesChineseFirst() {
        assertEquals(listOf(C to "學校 ", J to "学", C to "校"), runsOf("學校 学校"))
    }

    @Test
    fun hangulWithHan_hanTriesChineseFirst() {
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
    fun supplementaryHan_indicesAreUtf16() {
        assertEquals(
            listOf(TextToppingScriptRun(J, 0, 3)),
            textToppingScriptRuns("𩸚パ"),
        )
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
            "パフェ👨‍👩‍👧", "𩸚パ",
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
