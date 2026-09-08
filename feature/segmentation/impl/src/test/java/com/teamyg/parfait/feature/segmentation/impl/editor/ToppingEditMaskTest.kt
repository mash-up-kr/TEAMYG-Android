package com.teamyg.parfait.feature.segmentation.impl.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SIDE = 4

/** ARGB 에서 알파만 의미가 있다 — 색은 SRC_IN 단계에서 원본으로 덮이므로 0 으로 둔다 */
private fun pixel(alpha: Int): Int = alpha shl 24

class ToppingEditMaskTest {
    private fun pixels(vararg alphas: Int): IntArray = IntArray(SIDE * SIDE) { index -> pixel(alphas[index]) }

    @Test
    fun measureSubject_opaqueBlockInTheMiddle_returnsItsBounds() {
        // Given — (1,1)~(2,2) 네 픽셀만 불투명하다
        val pixels = pixels(
            0, 0, 0, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertFalse(measure.isEmpty)
        assertEquals(1, measure.left)
        assertEquals(1, measure.top)
        assertEquals(2, measure.right)
        assertEquals(2, measure.bottom)
    }

    @Test
    fun measureSubject_opaqueBlockInTheMiddle_sumsTheAlpha() {
        // Given
        val pixels = pixels(
            0, 0, 0, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then — 네 픽셀 × 255
        assertEquals(1_020L, measure.alphaSum)
    }

    @Test
    fun measureSubject_semiTransparentPixels_countTheirOwnAlpha() {
        // Given — 반투명 픽셀은 "있음"이 아니라 알파 값 그대로 합에 들어간다
        val pixels = pixels(
            0, 0, 0, 0,
            0, 10, 20, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertEquals(30L, measure.alphaSum)
        assertEquals(1, measure.left)
        assertEquals(2, measure.right)
        assertEquals(1, measure.top)
        assertEquals(1, measure.bottom)
    }

    @Test
    fun measureSubject_fullyTransparent_isEmpty() {
        // Given
        val pixels = IntArray(SIDE * SIDE)

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertTrue(measure.isEmpty)
        assertEquals(0L, measure.alphaSum)
    }

    @Test
    fun measureSubject_singleOpaqueCorner_boundsAreOnePixelWide() {
        // Given — 오른쪽 아래 한 픽셀만 남았다
        val pixels = pixels(
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 255,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertEquals(3, measure.left)
        assertEquals(3, measure.right)
        assertEquals(3, measure.top)
        assertEquals(3, measure.bottom)
        assertEquals(255L, measure.alphaSum)
    }
}
