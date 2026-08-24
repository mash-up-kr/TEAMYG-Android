package com.teamyg.parfait.data.repository.image

import kotlin.test.Test
import kotlin.test.assertEquals

private const val OPAQUE_WHITE = 0xFFFFFFFF.toInt()
private const val TRANSPARENT = 0

class AlphaCoverageTest {
    @Test
    fun sumAlpha_everyPixelIsOpaque_sumsTo255PerPixel() {
        // Given
        val pixels = IntArray(4) { OPAQUE_WHITE }

        // When
        val sum = sumAlpha(pixels)

        // Then
        assertEquals(4L * 255, sum)
    }

    @Test
    fun sumAlpha_partialAlpha_countsTheActualValue() {
        // Given — 알파 128·64 와 투명 둘
        val pixels = intArrayOf(0x80FFFFFF.toInt(), 0x40FFFFFF, TRANSPARENT, TRANSPARENT)

        // When
        val sum = sumAlpha(pixels)

        // Then
        assertEquals(192L, sum)
    }

    @Test
    fun sumAlpha_wouldOverflowInt_staysCorrectInLong() {
        // Given — Int.MAX_VALUE 를 넘는 합. 12MP 전면 불투명 후보가 이 구간이다
        val pixels = IntArray(10_000_000) { OPAQUE_WHITE }

        // When
        val sum = sumAlpha(pixels)

        // Then
        assertEquals(10_000_000L * 255, sum)
    }
}
