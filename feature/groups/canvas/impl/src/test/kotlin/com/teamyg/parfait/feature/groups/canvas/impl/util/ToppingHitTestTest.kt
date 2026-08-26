package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 왼쪽 절반만 불투명한 4x4 마스크. 좌우 비대칭이라 좌표 부호 실수가 드러난다. */
private fun leftHalfMask(): ToppingAlphaMask =
    toppingAlphaMaskOf(width = 4, height = 4) { x, _ -> if (x < 2) 255 else 0 }

private fun target(
    rotationDegrees: Float = 0f,
    borderWidthPx: Float = 0f,
    mask: ToppingAlphaMask? = leftHalfMask(),
): ToppingHitTarget = ToppingHitTarget(
    centerXPx = 100f,
    centerYPx = 100f,
    imageWidthPx = 40f,
    imageHeightPx = 40f,
    rotationDegrees = rotationDegrees,
    borderWidthPx = borderWidthPx,
    mask = mask,
)

class ToppingHitTestTest {
    @Test
    fun containsPoint_opaqueSide_isHit() {
        // Given·When 왼쪽 절반(불투명)의 한가운데
        // Then
        assertTrue(target().containsPoint(90f, 100f))
    }

    @Test
    fun containsPoint_transparentSide_isMiss() {
        // Given·When 오른쪽 절반(투명)의 한가운데 — 그림 사각형 안이지만 안 눌려야 한다
        // Then
        assertFalse(target().containsPoint(110f, 100f))
    }

    @Test
    fun containsPoint_outsideImageRect_isMiss() {
        // Given·When 그림 사각형 밖
        // Then
        assertFalse(target().containsPoint(100f, 130f))
    }

    @Test
    fun containsPoint_rotated180_opaqueSideMovesToRight() {
        // Given 180도 돌리면 불투명한 왼쪽 절반이 오른쪽으로 온다
        val rotated = target(rotationDegrees = 180f)

        // Then
        assertTrue(rotated.containsPoint(110f, 100f))
        assertFalse(rotated.containsPoint(90f, 100f))
    }

    @Test
    fun containsPoint_withBorder_extendsBeyondSilhouette() {
        // Given 테두리 8px 인 토핑. 투명한 오른쪽이지만 불투명 경계에서 8px 안쪽이다
        val bordered = target(borderWidthPx = 8f)

        // Then 테두리가 있으면 히트, 없으면 미스다
        assertTrue(bordered.containsPoint(104f, 100f))
        assertFalse(target(borderWidthPx = 0f).containsPoint(104f, 100f))
    }

    @Test
    fun containsPoint_withBorder_farTransparentStillMiss() {
        // Given 테두리 두께보다 훨씬 멀리 떨어진 투명한 자리
        val bordered = target(borderWidthPx = 4f)

        // Then
        assertFalse(bordered.containsPoint(118f, 100f))
    }

    @Test
    fun containsPoint_nullMask_fallsBackToRectangle() {
        // Given 마스크가 아직 없는 토핑
        val noMask = target(mask = null)

        // Then 그림 사각형 안이면 투명한 자리여도 히트다 — 현행 판정과 같다
        assertTrue(noMask.containsPoint(110f, 100f))
        assertFalse(noMask.containsPoint(100f, 130f))
    }

    @Test
    fun containsPoint_emptyMask_fallsBackToRectangle() {
        // Given 불투명 픽셀이 하나도 없는 마스크 — 부재로 봐야 영영 안 눌리는 일이 없다
        val empty = target(mask = toppingAlphaMaskOf(4, 4) { _, _ -> 0 })

        // Then
        assertTrue(empty.containsPoint(110f, 100f))
    }
}
