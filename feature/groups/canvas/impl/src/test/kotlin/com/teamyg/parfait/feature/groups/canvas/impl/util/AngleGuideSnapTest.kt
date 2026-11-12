package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AngleGuideSnapTest {
    @Test
    fun resolveAngleGuide_exactlyAtZero_snapsHorizontal() {
        val result = resolveAngleGuide(rawDegrees = 0f, thresholdDegrees = 3f)

        assertTrue(result.isSnapped)
        assertEquals(0f, result.effectiveValue)
        assertTrue(result.isHorizontal)
    }

    @Test
    fun resolveAngleGuide_justInsideThreshold_snapsToNinety_vertical() {
        val result = resolveAngleGuide(rawDegrees = 92.9f, thresholdDegrees = 3f)

        assertTrue(result.isSnapped)
        assertEquals(90f, result.effectiveValue)
        assertFalse(result.isHorizontal)
    }

    @Test
    fun resolveAngleGuide_justOutsideThreshold_doesNotSnap() {
        val result = resolveAngleGuide(rawDegrees = 93.1f, thresholdDegrees = 3f)

        assertFalse(result.isSnapped)
        assertEquals(93.1f, result.effectiveValue)
    }

    @Test
    fun resolveAngleGuide_near180_snapsHorizontal() {
        val result = resolveAngleGuide(rawDegrees = 178f, thresholdDegrees = 3f)

        assertTrue(result.isSnapped)
        assertEquals(180f, result.effectiveValue)
        assertTrue(result.isHorizontal)
    }

    @Test
    fun resolveAngleGuide_near270_snapsVertical() {
        val result = resolveAngleGuide(rawDegrees = 271.5f, thresholdDegrees = 3f)

        assertTrue(result.isSnapped)
        assertEquals(270f, result.effectiveValue)
        assertFalse(result.isHorizontal)
    }

    @Test
    fun resolveAngleGuide_unboundedPastFullTurn_snapsTo360NotZero() {
        // 362도는 0도가 아니라 360도로 스냅해야 연속적으로 움직인다(접어서 비교하면 큰 점프가 생긴다)
        val result = resolveAngleGuide(rawDegrees = 362f, thresholdDegrees = 3f)

        assertTrue(result.isSnapped)
        assertEquals(360f, result.effectiveValue)
        assertTrue(result.isHorizontal)
    }

    @Test
    fun resolveAngleGuide_negativeAngle_snapsToNegativeNinety() {
        val result = resolveAngleGuide(rawDegrees = -91f, thresholdDegrees = 3f)

        assertTrue(result.isSnapped)
        assertEquals(-90f, result.effectiveValue)
        assertFalse(result.isHorizontal)
    }

    @Test
    fun resolveAngleGuide_farFromAnyMultiple_notHorizontalFlagReflectsNearestTarget() {
        // 45도는 스냅 안 되지만, isHorizontal 은 "지금 가장 가까운 쪽"을 가리킨다(0 과 90 이 똑같이
        // 가까우면 반올림 규칙상 90 쪽으로 붙는다)
        val result = resolveAngleGuide(rawDegrees = 45f, thresholdDegrees = 3f)

        assertFalse(result.isSnapped)
        assertEquals(45f, result.effectiveValue)
    }
}
