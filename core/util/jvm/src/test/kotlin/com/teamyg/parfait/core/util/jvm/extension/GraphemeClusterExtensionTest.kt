package com.teamyg.parfait.core.util.jvm.extension

import kotlin.test.Test
import kotlin.test.assertEquals

class GraphemeClusterExtensionTest {
    @Test
    fun graphemeClusterCount_asciiText_matchesLength() {
        assertEquals(5, "hello".graphemeClusterCount())
    }

    @Test
    fun graphemeClusterCount_skinToneModifiedEmoji_countsAsOne() {
        assertEquals(1, "🙌🏽".graphemeClusterCount())
    }

    @Test
    fun graphemeClusterCount_zwjFamilyEmoji_countsAsOne() {
        assertEquals(1, "👨‍👩‍👧".graphemeClusterCount())
    }

    @Test
    fun graphemeClusterCount_keycapEmoji_countsAsOne() {
        assertEquals(1, "1️⃣".graphemeClusterCount())
    }

    @Test
    fun graphemeClusterCount_lineBreak_countsAsOneChar() {
        assertEquals(3, "a\nb".graphemeClusterCount())
    }

    @Test
    fun graphemeClusterCount_empty_isZero() {
        assertEquals(0, "".graphemeClusterCount())
    }

    @Test
    fun truncateToGraphemeClusters_withinLimit_returnsUnchanged() {
        assertEquals("hi", "hi".truncateToGraphemeClusters(10))
    }

    @Test
    fun truncateToGraphemeClusters_exactLength_returnsUnchanged() {
        assertEquals("abc", "abc".truncateToGraphemeClusters(3))
    }

    @Test
    fun truncateToGraphemeClusters_cutsAtClusterBoundary_withoutSplittingEmoji() {
        // Given 3클러스터(a, 조합 이모지, b) 중 앞 2개만 요청
        val text = "a🙌🏽b"

        val truncated = text.truncateToGraphemeClusters(2)

        // Then 이모지가 중간에서 깨지지 않고 통째로 포함된다
        assertEquals("a🙌🏽", truncated)
        assertEquals(2, truncated.graphemeClusterCount())
    }
}
