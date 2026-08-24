package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val TEST_OPTIONS = AlphaPostProcessOptions(
    downscaleFactor = 1,
    areaOpeningMinPixels = 4,
    erodeEdge = false,
)

private fun confidenceBuffer(values: FloatArray): FloatBuffer = FloatBuffer.wrap(values)

class SegmentationMaskTest {
    @Test
    fun confidenceToAlpha_atOrBelowTheRampFloor_isFullyTransparent() {
        // Given · When · Then
        assertEquals(0, confidenceToAlpha(0f))
        assertEquals(0, confidenceToAlpha(0.35f))
    }

    @Test
    fun confidenceToAlpha_atOrAboveTheRampCeiling_isFullyOpaque() {
        // Given · When · Then
        assertEquals(255, confidenceToAlpha(0.65f))
        assertEquals(255, confidenceToAlpha(1f))
    }

    @Test
    fun confidenceToAlpha_exactlyAtTheOldThreshold_staysBackground() {
        // Given — 종전 상수는 "이 값을 넘는" 신뢰도만 객체로 봤다. 램프도 그 경계를 지켜야 한다.
        // 알파 > 127 은 신뢰도 0.5 가 아니라 0.35 + 128 × 0.3 / 255 ≈ 0.5006 이다

        // When · Then
        assertEquals(127, confidenceToAlpha(0.5f))
        assertEquals(128, confidenceToAlpha(0.5006f))
    }

    @Test
    fun maskSubjectAlpha_confidentBlobWithASpeck_dropsTheSpeck() {
        // Given — 8×8. 왼쪽 위 4×4 는 확실하고 오른쪽 아래 한 점만 튄다
        val values = FloatArray(64)
        for (y in 0 until 4) for (x in 0 until 4) values[y * 8 + x] = 1f
        values[63] = 1f

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertEquals(
            0,
            masked
                ?.alpha
                ?.get(63)
                ?.toInt()
                ?.and(0xFF),
        )
        assertEquals(4, masked?.result?.bounds?.right)
    }

    @Test
    fun maskSubjectAlpha_everyPixelIsConfident_coversTheWholePlate() {
        // Given — 프레임에 걸친 피사체가 테두리를 잃지 않는지 본다
        val values = FloatArray(64) { 1f }

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertEquals(8, masked?.result?.bounds?.right)
        assertEquals(8, masked?.result?.bounds?.bottom)
    }

    @Test
    fun maskSubjectAlpha_nothingIsConfident_returnsNull() {
        // Given
        val values = FloatArray(64) { 0.1f }

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertNull(masked)
    }

    @Test
    fun maskSubjectAlpha_bufferLimitBelowCapacity_stopsAtLimitInsteadOfReadingPastIt() {
        // Given — absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼는다
        val buffer = FloatBuffer.allocate(64).apply { limit(10) }

        // When · Then
        assertFailsWith<IndexOutOfBoundsException> {
            maskSubjectAlpha(buffer, width = 8, height = 8, options = TEST_OPTIONS)
        }
    }
}
