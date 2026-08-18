package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val OPAQUE_WHITE = 0xFFFFFFFF.toInt()
private const val TRANSPARENT = 0

class SegmentationMaskTest {
    /** 3×3 이미지 전 픽셀을 불투명 흰색으로 채운다 */
    private fun pixels() = IntArray(9) { OPAQUE_WHITE }

    private fun mask(vararg confidences: Float): FloatBuffer = FloatBuffer.wrap(confidences)

    @Test
    fun maskSubjectPixels_oneCenterPixelIsSubject_boundsCoverThatPixelOnly() {
        // Given 가운데 한 칸만 객체인 3×3 마스크
        val pixels = pixels()
        val mask = mask(
            0f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f,
        )

        // When 객체가 아닌 자리를 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 그 한 칸만 감싸는 영역이 나온다. right·bottom 은 마지막 픽셀을 포함하는 exclusive 값이다
        assertEquals(1, bounds?.left)
        assertEquals(1, bounds?.top)
        assertEquals(2, bounds?.right)
        assertEquals(2, bounds?.bottom)
    }

    @Test
    fun maskSubjectPixels_oneCenterPixelIsSubject_erasesEverythingElse() {
        // Given 가운데 한 칸만 객체인 3×3 마스크
        val pixels = pixels()
        val mask = mask(
            0f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f,
        )

        // When 객체가 아닌 자리를 지운다
        maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 객체 픽셀만 원본 색으로 남는다
        assertEquals(OPAQUE_WHITE, pixels[4])
        assertTrue(pixels.filterIndexed { index, _ -> index != 4 }.all { it == TRANSPARENT })
    }

    @Test
    fun maskSubjectPixels_nothingDetected_returnsNull() {
        // Given 아무 데도 객체가 없는 마스크
        val pixels = pixels()
        val mask = mask(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

        // When 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 감쌀 것이 없으므로 영역도 없다
        assertNull(bounds)
        assertTrue(pixels.all { it == TRANSPARENT })
    }

    @Test
    fun maskSubjectPixels_confidenceIsExactlyTheThreshold_isNotSubject() {
        // Given 정확히 임계값인 칸 하나뿐인 마스크
        val pixels = pixels()
        val mask = mask(0f, 0f, 0f, 0f, SUBJECT_CONFIDENCE_THRESHOLD, 0f, 0f, 0f, 0f)

        // When 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 임계값 자체는 객체가 아니다 — 판정이 초과이지 이상이 아니다
        assertNull(bounds)
    }

    @Test
    fun maskSubjectPixels_everyPixelIsSubject_keepsAllPixelsAndCoversTheWholeImage() {
        // Given 전부 객체인 마스크
        val pixels = pixels()
        val mask = mask(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)

        // When 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 이미지 전체가 영역이고 지워진 픽셀이 없다
        assertEquals(0, bounds?.left)
        assertEquals(0, bounds?.top)
        assertEquals(3, bounds?.right)
        assertEquals(3, bounds?.bottom)
        assertTrue(pixels.all { it == OPAQUE_WHITE })
    }
}
