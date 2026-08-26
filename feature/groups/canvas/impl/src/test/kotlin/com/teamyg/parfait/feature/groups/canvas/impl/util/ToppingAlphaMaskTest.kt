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

    @Test
    fun hasAnyOpaque_withOpaquePixels_isTrue() {
        // Given 불투명 픽셀이 하나 있는 마스크
        val mask = toppingAlphaMaskOf(width = 2, height = 2) { x, y ->
            if (x == 0 && y == 0) 255 else 0
        }

        // Then hasAnyOpaque는 참이어야 한다 — 뒤 태스크의 마스크 부재 판정에 쓰인다
        assertTrue(mask.hasAnyOpaque)
    }

    @Test
    fun isOpaqueAt_nonSquareMask_rowMajorIndexing() {
        // Given 가로 3, 세로 5인 비정사각 마스크로 좌우·상하 비대칭 배치
        // (x=2, y=0)과 (x=0, y=4)에만 불투명
        val mask = toppingAlphaMaskOf(width = 3, height = 5) { x, y ->
            when {
                x == 2 && y == 0 -> 255
                x == 0 && y == 4 -> 255
                else -> 0
            }
        }

        // Then 행 우선(row-major) 인덱싱이 맞아야 각 픽셀을 읽을 수 있다
        assertTrue(mask.isOpaqueAt(2, 0))
        assertTrue(mask.isOpaqueAt(0, 4))
        assertFalse(mask.isOpaqueAt(0, 0))
        assertFalse(mask.isOpaqueAt(2, 4))
    }

    @Test
    fun isOpaqueAt_alphaAtThreshold_isTrue() {
        // Given 임계값과 정확히 같은 알파값
        val mask = toppingAlphaMaskOf(width = 2, height = 2) { _, _ ->
            TOPPING_MASK_ALPHA_THRESHOLD
        }

        // Then 임계값 자체는 불투명으로 치한다
        assertTrue(mask.isOpaqueAt(0, 0))
        assertTrue(mask.isOpaqueAt(1, 1))
    }
}
