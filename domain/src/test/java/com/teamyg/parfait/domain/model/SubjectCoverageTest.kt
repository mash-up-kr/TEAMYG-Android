package com.teamyg.parfait.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/** 100×100 캔버스는 비율 하한이 5px 이라 절대 하한(2,500px)이 이긴다 */
private const val SMALL_CANVAS_AREA = 100L * 100L

/** 4000×3000 캔버스는 비율 하한이 6,000px 이라 절대 하한을 넘어선다 */
private const val BIG_CANVAS_AREA = 4_000L * 3_000L

class SubjectCoverageTest {
    @Test
    fun floorPixels_smallCanvas_usesTheAbsoluteFloor() {
        // When
        val floor = SubjectCoverage.floorPixels(SMALL_CANVAS_AREA)

        // Then
        assertEquals(2_500L, floor)
    }

    @Test
    fun floorPixels_bigCanvas_usesTheRatioFloor() {
        // When
        val floor = SubjectCoverage.floorPixels(BIG_CANVAS_AREA)

        // Then
        assertEquals(6_000L, floor)
    }

    @Test
    fun isLargeEnough_coverageIsExactlyTheFloor_isTrue() {
        // Given — 알파 합은 픽셀 수 × 255 다
        val alphaSum = 255L * 2_500L

        // Then
        assertTrue(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = SMALL_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_coverageIsOneShortOfTheFloor_isFalse() {
        // Given
        val alphaSum = 255L * 2_500L - 1L

        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = SMALL_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_fullyTransparent_isFalse() {
        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = 0L, canvasArea = SMALL_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_bigCanvas_ratioFloorTakesOverTheAbsoluteFloor() {
        // Given — 절대 하한은 넘지만 비율 하한에는 못 미치는 커버리지
        val alphaSum = 255L * 3_000L

        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = BIG_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_canvasAreaIsZero_isFalse() {
        // Given — 좌표계가 없으면 비율을 잴 수 없다
        val alphaSum = 255L * 10_000L

        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = 0L))
    }

    @Test
    fun isLargeEnough_twelveMegapixelOpaqueSubject_survivesTheIntBoundary() {
        // Given — 4000×3000 전면 불투명. 알파 합 30.6억은 Int 범위를 넘는다
        val canvasArea = BIG_CANVAS_AREA
        val alphaSum = 255L * canvasArea

        // Then
        assertTrue(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = canvasArea))
    }
}
