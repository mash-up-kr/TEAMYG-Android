package com.teamyg.parfait.data.repository.image

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 값이 0~1 범위라 이 정도면 충분하다 */
private const val TOLERANCE = 1e-4f

private fun assertClose(
    expected: Float,
    actual: Float,
    message: String = "",
) {
    assertTrue(abs(expected - actual) <= TOLERANCE, "$message expected=$expected actual=$actual")
}

class AlphaRefineTest {
    @Test
    fun boxMean_everyValueIsOne_staysOneEvenAtTheCorners() {
        // Given — 고정 개수로 나누면 모서리가 4/9 로 내려앉는다
        val src = FloatArray(9) { 1f }

        // When
        val mean = boxMean(src, width = 3, height = 3, radius = 1)

        // Then
        for (index in mean.indices) assertClose(1f, mean[index], "index=$index")
    }

    @Test
    fun boxMean_radiusLargerThanTheArray_averagesEverything() {
        // Given
        val src = floatArrayOf(0f, 1f, 0f, 1f)

        // When
        val mean = boxMean(src, width = 2, height = 2, radius = 5)

        // Then
        for (index in mean.indices) assertClose(0.5f, mean[index], "index=$index")
    }

    @Test
    fun boxMean_singleSpike_spreadsOverTheWindowOnly() {
        // Given — 5×5 한가운데(2,2)만 1 이다
        val src = FloatArray(25)
        src[12] = 1f

        // When
        val mean = boxMean(src, width = 5, height = 5, radius = 1)

        // Then — 중앙과 대각 이웃(1,1)은 창이 온전히 안에 들어 1/9, 두 칸 밖은 0
        assertClose(1f / 9f, mean[12], "center")
        assertClose(1f / 9f, mean[6], "diagonal neighbour")
        assertClose(0f, mean[0], "two cells away")
    }

    @Test
    fun boxMean_windowClippedAtTheEdge_usesTheActualCount() {
        // Given — 한 행짜리. 왼쪽 끝의 창은 두 칸만 포함한다
        val src = floatArrayOf(1f, 0f, 0f, 0f)

        // When
        val mean = boxMean(src, width = 4, height = 1, radius = 1)

        // Then
        assertClose(0.5f, mean[0], "left edge counts two cells")
        assertClose(1f / 3f, mean[1], "interior counts three cells")
    }

    @Test
    fun downscaleLuminance_greenAndBlueWeights_areNotSwapped() {
        // Given — 순수색 셋. 계수를 맞바꾸면 회색·빨강 테스트로는 안 잡힌다
        val pixels = intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFF0000.toInt())

        // When
        val sub = downscaleLuminance(pixels, width = 3, height = 1, factor = 1)

        // Then
        assertClose(0.587f, sub[0], "green")
        assertClose(0.114f, sub[1], "blue")
        assertClose(0.299f, sub[2], "red")
    }

    @Test
    fun downscaleLuminance_ignoresTheAlphaChannel() {
        // Given — 같은 빨강인데 알파만 다르다. 안내자는 색만 봐야 한다
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0x00FF0000)

        // When
        val sub = downscaleLuminance(pixels, width = 2, height = 1, factor = 1)

        // Then
        assertClose(0.299f, sub[0], "opaque red")
        assertClose(0.299f, sub[1], "transparent red")
    }

    @Test
    fun downscaleAlpha_valueAbove127_isNotMisreadAsNegative() {
        // Given — 부호 처리를 빠뜨리면 음수가 된다
        val alpha = byteArrayOf(0, 128.toByte(), 255.toByte())

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 1, factor = 1)

        // Then
        assertClose(0f, sub[0], "transparent")
        assertClose(128f / 255f, sub[1], "half")
        assertClose(1f, sub[2], "opaque")
    }

    @Test
    fun downscaleAlpha_widthIsNotAMultipleOfFactor_averagesTheShortBlock() {
        // Given — 3×1 을 배율 2 로 줄이면 두 번째 블록에 한 칸만 든다
        val alpha = byteArrayOf(255.toByte(), 0, 255.toByte())

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 1, factor = 2)

        // Then
        assertEquals(2, sub.size)
        assertClose(0.5f, sub[0], "full block")
        assertClose(1f, sub[1], "short block averages one cell, not two")
    }

    @Test
    fun downscaleAlpha_heightIsNotAMultipleOfFactor_keepsTheTrailingRow() {
        // Given — 3×3 을 배율 2 로 줄이면 2×2 다. 마지막 행·열은 한 칸짜리 블록이다.
        // 아래 행만 불투명하게 두면 세로 인덱싱이 틀렸을 때 값이 어긋난다
        val alpha = ByteArray(9) { index -> if (index >= 6) 255.toByte() else 0 }

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 3, factor = 2)

        // Then
        assertEquals(4, sub.size)
        assertClose(0f, sub[0], "top-left block has no opaque row")
        assertClose(0f, sub[1], "top-right block has no opaque row")
        assertClose(1f, sub[2], "bottom-left block is the trailing row")
        assertClose(1f, sub[3], "bottom-right block is the trailing row")
    }
}
