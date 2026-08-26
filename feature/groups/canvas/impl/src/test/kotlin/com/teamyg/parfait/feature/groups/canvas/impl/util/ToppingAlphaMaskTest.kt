package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingAlphaMaskTest {
    @Test
    fun isOpaqueAt_alphaAboveThreshold_isTrue() {
        // Given 가운데 한 픽셀만 완전 불투명한 3x3 마스크
        val mask = toppingAlphaMaskOf(width = 3, height = 3) { x, y ->
            if (x == 1 && y == 1) 255 else 0
        }

        // Then
        assertTrue(mask.isOpaqueAt(1, 1))
        assertFalse(mask.isOpaqueAt(0, 0))
    }

    @Test
    fun isOpaqueAt_alphaBelowThreshold_isFalse() {
        // Given 임계값 바로 아래로만 채운 마스크 — 다운스케일 잡티를 걸러 내는 자리다
        val mask = toppingAlphaMaskOf(width = 2, height = 2) { _, _ ->
            TOPPING_MASK_ALPHA_THRESHOLD - 1
        }

        // Then
        assertFalse(mask.isOpaqueAt(0, 0))
        assertFalse(mask.hasAnyOpaque)
    }

    @Test
    fun isOpaqueAt_outOfBounds_isFalseNotThrow() {
        // Given 테두리 되밀기 점은 정의상 마스크 밖으로 나간다
        val mask = toppingAlphaMaskOf(width = 2, height = 2) { _, _ -> 255 }

        // Then 예외가 아니라 "불투명 아님"이다
        assertFalse(mask.isOpaqueAt(-1, 0))
        assertFalse(mask.isOpaqueAt(0, -1))
        assertFalse(mask.isOpaqueAt(2, 0))
        assertFalse(mask.isOpaqueAt(0, 2))
    }

    @Test
    fun hasAnyOpaque_allTransparent_isFalse() {
        // Given 불투명 픽셀이 하나도 없는 마스크 — 이런 마스크는 부재로 취급해야 한다
        val mask = toppingAlphaMaskOf(width = 8, height = 8) { _, _ -> 0 }

        // Then
        assertFalse(mask.hasAnyOpaque)
    }

    @Test
    fun bitset_packsMoreThan64Pixels() {
        // Given 64픽셀을 넘겨 LongArray 가 여러 칸이 되는 크기
        val mask = toppingAlphaMaskOf(width = 10, height = 10) { x, y ->
            if (x == 9 && y == 9) 255 else 0
        }

        // Then 마지막 픽셀이 두 번째 Long 칸에 들어가도 제대로 읽힌다
        assertTrue(mask.isOpaqueAt(9, 9))
        assertFalse(mask.isOpaqueAt(8, 9))
    }
}
