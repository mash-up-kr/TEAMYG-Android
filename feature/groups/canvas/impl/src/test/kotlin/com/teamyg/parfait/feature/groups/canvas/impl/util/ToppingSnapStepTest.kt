package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingSnapStepTest {
    // ── stepAngleGuide: 회전했을 때만 켜기(0도 토핑 드래그 중 상시 노출 방지) ──

    @Test
    fun stepAngleGuide_atZeroWithoutRotation_staysHidden() {
        // 0도 토핑을 회전 없이 드래그만 하면, resolveAngleGuide(0).isSnapped 가 true여도 가이드는 꺼져 있다
        val step = stepAngleGuide(oldRawDegrees = 0f, oldHasRotated = false, rotationDelta = 0f)

        assertFalse(step.hasRotated)
        assertFalse(step.isVisible)
        assertFalse(step.justSnapped)
        assertEquals(0f, step.effectiveDegrees)
    }

    @Test
    fun stepAngleGuide_firstRotationIntoZone_becomesVisibleAndSnapsOnce() {
        // 회전이 시작되고 0도 ±3도 안이면 가이드가 켜지고 그 프레임에만 justSnapped 다
        val step = stepAngleGuide(oldRawDegrees = 0f, oldHasRotated = false, rotationDelta = 2f)

        assertTrue(step.hasRotated)
        assertTrue(step.isVisible)
        assertTrue(step.justSnapped)
        assertTrue(step.isHorizontal)
        assertEquals(0f, step.effectiveDegrees)
    }

    @Test
    fun stepAngleGuide_stayingInZone_doesNotReSnap() {
        // 이미 켜진 채로 계속 범위 안이면 justSnapped 는 false(햅틱 1회)
        val step = stepAngleGuide(oldRawDegrees = 2f, oldHasRotated = true, rotationDelta = 1f)

        assertTrue(step.isVisible)
        assertFalse(step.justSnapped)
    }

    @Test
    fun stepAngleGuide_rotatingIntoNinety_snapsVerticalOnce() {
        // 85도(범위 밖)에서 3도 더 돌려 88도로 들어오면 90도 세로선으로 스냅되고 한 번 울린다
        val step = stepAngleGuide(oldRawDegrees = 85f, oldHasRotated = true, rotationDelta = 3f)

        assertTrue(step.isVisible)
        assertTrue(step.justSnapped)
        assertFalse(step.isHorizontal)
        assertEquals(90f, step.effectiveDegrees)
    }

    @Test
    fun stepAngleGuide_notInZone_keepsRawDegrees() {
        // 범위 밖이면 스냅값이 아니라 누적 원값을 그대로 쓴다
        val step = stepAngleGuide(oldRawDegrees = 40f, oldHasRotated = true, rotationDelta = 5f)

        assertFalse(step.isVisible)
        assertEquals(45f, step.effectiveDegrees)
    }

    // ── stepCenterGuideDp: 위치 스냅의 진입 엣지 판정 ──

    @Test
    fun stepCenterGuideDp_panningIntoCenter_snapsAndReportsJustSnapped() {
        // 중심이 캔버스 중앙(130dp)에서 6dp 밖이었다가 안으로 들어오면 스냅되고 justSnapped 다
        val step = stepCenterGuideDp(
            oldRawOffsetX = 60.dp,
            oldRawOffsetY = 0.dp,
            panX = 18.dp,
            panY = 0.dp,
            halfWidth = 50.dp,
            halfHeight = 25.dp,
            canvasCenterXDp = 130f,
            canvasCenterYDp = 500f,
        )

        assertTrue(step.guideX.isSnapped)
        assertEquals(130f, step.guideX.effectiveValue)
        assertTrue(step.justSnapped)
        assertEquals(78.dp, step.rawOffsetX)
    }

    @Test
    fun stepCenterGuideDp_stayingSnapped_doesNotReportJustSnapped() {
        // 이미 중앙에 스냅된 채로 더 끌어도 justSnapped 는 false
        val step = stepCenterGuideDp(
            oldRawOffsetX = 80.dp,
            oldRawOffsetY = 0.dp,
            panX = 1.dp,
            panY = 0.dp,
            halfWidth = 50.dp,
            halfHeight = 25.dp,
            canvasCenterXDp = 130f,
            canvasCenterYDp = 500f,
        )

        assertTrue(step.guideX.isSnapped)
        assertFalse(step.justSnapped)
    }
}
