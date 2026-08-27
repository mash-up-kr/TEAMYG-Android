package com.teamyg.parfait.data.repository.image

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

/** `#` 는 불투명, `.` 은 투명. 한 줄이 한 행이다 */
private fun alphaOf(vararg rows: String): ByteArray {
    val flat = rows.joinToString(separator = "")
    return ByteArray(flat.length) { if (flat[it] == '#') 255.toByte() else 0 }
}

/** `#` 는 참, `.` 은 거짓 */
private fun maskOf(vararg rows: String): BooleanArray {
    val flat = rows.joinToString(separator = "")
    return BooleanArray(flat.length) { flat[it] == '#' }
}

private fun BooleanArray.render(width: Int): String = toList().chunked(width).joinToString(separator = "\n") { row ->
    row.joinToString(separator = "") { if (it) "#" else "." }
}

class AlphaComponentsTest {
    @Test
    fun ceilDiv_roundsUpOnRemainder_andStaysExactOnMultiples() = runTest {
        assertEquals(1000, ceilDiv(4000, 4))
        assertEquals(1001, ceilDiv(4001, 4))
        assertEquals(1001, ceilDiv(4004, 4))
        assertEquals(0, ceilDiv(0, 4))
    }

    @Test
    fun ceilDiv_zeroDivisor_failsAtTheCallInsteadOfDividing() = runTest {
        // 0 을 돌려주면 factor 로 나누는 나머지 세 자리에서 같은 예외가 더 안쪽에서 난다
        assertFailsWith<IllegalArgumentException> { ceilDiv(4000, 0) }
    }

    @Test
    fun ceilDiv_negativeDivisor_fails() = runTest {
        // 음수는 안 터지고 조용히 틀린 값을 내보내는 쪽이라 더 위험하다
        assertFailsWith<IllegalArgumentException> { ceilDiv(4000, -4) }
    }

    @Test
    fun downscaleMask_factorFour_orsEachBlock() = runTest {
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
    fun downscaleMask_alphaAbove127_readsAsForegroundDespiteSignedByte() = runTest {
        // Given — 128 은 Byte 로 담으면 음수다. and 0xFF 가 없으면 배경으로 오판한다
        val alpha = ByteArray(1) { 128.toByte() }

        // When
        val mask = downscaleMask(alpha, width = 1, height = 1, factor = 1, threshold = 127)

        // Then
        assertContentEquals(booleanArrayOf(true), mask)
    }

    @Test
    fun downscaleMask_alphaExactlyAtThreshold_readsAsBackground() = runTest {
        // Given
        val alpha = ByteArray(1) { 127.toByte() }

        // When
        val mask = downscaleMask(alpha, width = 1, height = 1, factor = 1, threshold = 127)

        // Then
        assertContentEquals(booleanArrayOf(false), mask)
    }

    @Test
    fun downscaleMask_sizeIsNotAMultipleOfFactor_keepsTheTrailingEdge() = runTest {
        // Given — 5×1 에서 마지막 픽셀만 불투명하다. 내림하면 그 픽셀이 판정에서 빠진다
        val alpha = alphaOf("....#")

        // When
        val mask = downscaleMask(alpha, width = 5, height = 1, factor = 4, threshold = 127)

        // Then — 축소판 폭은 2 이고 두 번째 칸이 참이다
        assertEquals(2, mask.size)
        assertContentEquals(booleanArrayOf(false, true), mask)
    }

    @Test
    fun applyAreaOpening_tinyComponentBesideABigOne_removesOnlyTheTinyOne() = runTest {
        // Given — 왼쪽 4×4 덩어리(16px)와 오른쪽 아래 한 점
        val mask = maskOf(
            "####..",
            "####..",
            "####..",
            "####..",
            "......",
            ".....#",
        )

        // When
        val survived = applyAreaOpening(mask, width = 6, height = 6, minPixels = 4)

        // Then
        assertEquals(true, survived)
        assertEquals(
            """
            ####..
            ####..
            ####..
            ####..
            ......
            ......
            """.trimIndent(),
            mask.render(width = 6),
        )
    }

    @Test
    fun applyAreaOpening_componentExactlyAtThreshold_keepsIt() = runTest {
        // Given — 정확히 4픽셀짜리 성분 하나
        val mask = maskOf(
            "##..",
            "##..",
            "....",
            "....",
        )

        // When
        val survived = applyAreaOpening(mask, width = 4, height = 4, minPixels = 4)

        // Then
        assertEquals(true, survived)
        assertEquals(4, mask.count { it })
    }

    @Test
    fun applyAreaOpening_componentOnePixelBelowThreshold_removesIt() = runTest {
        // Given — 3픽셀짜리 성분 하나
        val mask = maskOf(
            "##..",
            "#...",
            "....",
            "....",
        )

        // When
        val survived = applyAreaOpening(mask, width = 4, height = 4, minPixels = 4)

        // Then
        assertEquals(false, survived)
        assertEquals(0, mask.count { it })
    }

    @Test
    fun applyAreaOpening_blobsTouchingOnlyDiagonally_countAsOneComponent() = runTest {
        // Given — 두 2×2 가 대각선으로만 닿는다. 합치면 8픽셀이라 살고, 따로면 각 4픽셀이라 죽는다
        val mask = maskOf(
            "##....",
            "##....",
            "..##..",
            "..##..",
            "......",
            "......",
        )

        // When
        val survived = applyAreaOpening(mask, width = 6, height = 6, minPixels = 5)

        // Then
        assertEquals(true, survived)
        assertEquals(8, mask.count { it })
    }

    @Test
    fun applyAreaOpening_oneRunBridgesTwoRunsAbove_mergesAllThree() = runTest {
        // Given — 윗행 두 런을 아랫행 한 런이 잇는다. 첫 매치에서 멈추는 구현이면 갈린다
        val mask = maskOf(
            "#.#.",
            "###.",
            "....",
            "....",
        )

        // When — 전부 한 성분이면 5픽셀이라 산다. 갈리면 어느 조각도 5를 못 넘는다
        val survived = applyAreaOpening(mask, width = 4, height = 4, minPixels = 5)

        // Then
        assertEquals(true, survived)
        assertEquals(5, mask.count { it })
    }

    @Test
    fun applyAreaOpening_everythingIsBackground_reportsNothingSurvived() = runTest {
        // Given
        val mask = maskOf("....", "....")

        // When
        val survived = applyAreaOpening(mask, width = 4, height = 2, minPixels = 1)

        // Then
        assertEquals(false, survived)
    }

    @Test
    fun dilateMask_singlePixel_growsToThreeByThree() = runTest {
        // Given
        val mask = maskOf(
            ".....",
            ".....",
            "..#..",
            ".....",
            ".....",
        )

        // When
        val dilated = dilateMask(mask, width = 5, height = 5)

        // Then
        assertEquals(
            """
            .....
            .###.
            .###.
            .###.
            .....
            """.trimIndent(),
            dilated.render(width = 5),
        )
    }

    @Test
    fun dilateMask_pixelAtTheCorner_doesNotWrapAround() = runTest {
        // Given
        val mask = maskOf(
            "#..",
            "...",
            "...",
        )

        // When
        val dilated = dilateMask(mask, width = 3, height = 3)

        // Then
        assertEquals(
            """
            ##.
            ##.
            ...
            """.trimIndent(),
            dilated.render(width = 3),
        )
    }

    @Test
    fun dilateMask_componentRemovedByAreaOpening_staysRemoved() = runTest {
        // Given — 지워진 소성분이 살아남은 성분과 체비쇼프 거리 2 다. 8-연결이라 그보다 가까울 수 없다
        val mask = maskOf(
            "###..#",
            "###...",
            "###...",
            "......",
        )
        applyAreaOpening(mask, width = 6, height = 4, minPixels = 4)

        // When
        val dilated = dilateMask(mask, width = 6, height = 4)

        // Then — 오른쪽 위 한 점이 부활하지 않는다
        assertEquals(false, dilated[5])
    }

    @Test
    fun downscaleMask_cancelledAtSecondRow_throwsAndStops() {
        // Given — 4행짜리 판에서 둘째 확인 때 취소한다
        val alpha = alphaOf(
            "####",
            "####",
            "####",
            "####",
        )
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { downscaleMask(alpha, width = 4, height = 4, factor = 1, threshold = 127) }
        }
        // 확인이 행 루프 맨 위에 있으므로 4행을 다 돌지 못한다
        assertEquals(2, job.calls)
    }

    @Test
    fun applyAreaOpening_cancelledMidway_throws() {
        // Given — union 단계가 행 쌍마다 확인한다
        val mask = maskOf(
            "####",
            "####",
            "####",
            "####",
        )
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { applyAreaOpening(mask, width = 4, height = 4, minPixels = 1) }
        }
    }

    @Test
    fun dilateMask_cancelledMidway_throws() {
        // Given
        val mask = maskOf(
            "####",
            "####",
            "####",
            "####",
        )
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { dilateMask(mask, width = 4, height = 4) }
        }
    }
}
