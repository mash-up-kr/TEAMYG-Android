package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun maskSubjectPixels_bufferLimitBelowCapacity_stopsAtLimitInsteadOfReadingPastIt() {
        // Given capacity 는 3×3(9) 와 같지만 limit 은 그보다 작은 버퍼 — ML Kit 이 capacity 만큼
        // 할당은 해 놓고 실제로는 그보다 적은 데이터만 채워 돌려주는 경우를 흉내낸다.
        // capacity() 만 보는 낡은 가드는 9 == 9 라 통과시키지만, absolute get(index) 는 limit 을
        // 경계로 삼으므로 limit 을 넘어서는 인덱스는 실제로 읽을 수 없다
        val pixels = pixels()
        val underlying = FloatBuffer.allocate(9)
        underlying.put(floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f))
        underlying.limit(8)
        underlying.position(0)

        // When & Then limit(8) 을 넘어서는 마지막 인덱스(8)를 읽으려 하면 조용히 넘어가거나
        // 엉뚱한 값을 읽는 대신 예외로 멈춘다 — 읽기가 limit 안에서만 유효하다는 뜻이고, 그래서
        // 호출부 가드는 capacity() 가 아니라 remaining() 으로 비교해야 한다
        assertFailsWith<IndexOutOfBoundsException> {
            maskSubjectPixels(pixels, underlying, width = 3, height = 3)
        }
    }
}
