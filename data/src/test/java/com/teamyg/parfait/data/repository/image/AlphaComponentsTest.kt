package com.teamyg.parfait.data.repository.image

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** `#` 는 불투명, `.` 은 투명. 한 줄이 한 행이다 */
private fun alphaOf(vararg rows: String): ByteArray {
    val flat = rows.joinToString(separator = "")
    return ByteArray(flat.length) { if (flat[it] == '#') 255.toByte() else 0 }
}

class AlphaComponentsTest {
    @Test
    fun downscaleMask_factorFour_orsEachBlock() {
        // Given — 8×8 에서 왼쪽 위 블록에 한 점만 있다
        val alpha = alphaOf(
            "#.......",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
        )

        // When
        val mask = downscaleMask(alpha, width = 8, height = 8, factor = 4, threshold = 127)

        // Then — 2×2 축소판에서 왼쪽 위만 참이다
        assertContentEquals(booleanArrayOf(true, false, false, false), mask)
    }

    @Test
    fun downscaleMask_alphaAbove127_readsAsForegroundDespiteSignedByte() {
        // Given — 128 은 Byte 로 담으면 음수다. and 0xFF 가 없으면 배경으로 오판한다
        val alpha = ByteArray(1) { 128.toByte() }

        // When
        val mask = downscaleMask(alpha, width = 1, height = 1, factor = 1, threshold = 127)

        // Then
        assertContentEquals(booleanArrayOf(true), mask)
    }

    @Test
    fun downscaleMask_alphaExactlyAtThreshold_readsAsBackground() {
        // Given
        val alpha = ByteArray(1) { 127.toByte() }

        // When
        val mask = downscaleMask(alpha, width = 1, height = 1, factor = 1, threshold = 127)

        // Then
        assertContentEquals(booleanArrayOf(false), mask)
    }

    @Test
    fun downscaleMask_sizeIsNotAMultipleOfFactor_keepsTheTrailingEdge() {
        // Given — 5×1 에서 마지막 픽셀만 불투명하다. 내림하면 그 픽셀이 판정에서 빠진다
        val alpha = alphaOf("....#")

        // When
        val mask = downscaleMask(alpha, width = 5, height = 1, factor = 4, threshold = 127)

        // Then — 축소판 폭은 2 이고 두 번째 칸이 참이다
        assertEquals(2, mask.size)
        assertContentEquals(booleanArrayOf(false, true), mask)
    }
}
