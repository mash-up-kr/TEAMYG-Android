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

/** 회색 픽셀. 휘도가 `value / 255` 다 */
private fun gray(value: Int): Int = (0xFF shl 24) or (value shl 16) or (value shl 8) or value

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

    @Test
    fun guidedCoefficients_constantGuidance_degeneratesToADoubleMean() {
        // Given — 안내자에 경계가 없으면 알파를 옮길 근거가 없다. a 는 0 이고 b 만 남는다
        val guidance = FloatArray(16) { 0.5f }
        val input = FloatArray(16) { index -> if (index % 4 < 2) 1f else 0f }

        // When
        val coefficients = guidedCoefficients(
            guidance = guidance,
            input = input,
            width = 4,
            height = 4,
            radius = 1,
            epsilon = 1e-4f,
        )

        // Then — b 는 창 평균을 **두 번** 거친 값이다. 한 번만 기대하면 가장자리에서 0.167 어긋난다
        val once = boxMean(input, width = 4, height = 4, radius = 1)
        val twice = boxMean(once, width = 4, height = 4, radius = 1)
        for (index in coefficients.a.indices) {
            assertClose(0f, coefficients.a[index], "a index=$index")
            assertClose(twice[index], coefficients.b[index], "b index=$index")
        }
    }

    @Test
    fun guidedCoefficients_inputEqualsGuidance_reproducesTheGuidance() {
        // Given — p 가 I 와 같으면 q = I 여야 하므로 a = 1, b = 0 이다.
        // ⚠️ 안내자는 **모든 창에 분산이 있어야** 한다. 계단형이면 가장자리 창의 분산이 0 이라
        // 그 자리 a 가 0 으로 떨어지고 두 번째 평균이 그것을 안쪽까지 번지게 한다
        val guidance = FloatArray(16) { index -> (index % 4) * 0.3f }

        // When
        val coefficients = guidedCoefficients(
            guidance = guidance,
            input = guidance.copyOf(),
            width = 4,
            height = 4,
            radius = 1,
            epsilon = 1e-8f,
        )

        // Then
        for (index in coefficients.a.indices) {
            assertClose(1f, coefficients.a[index], "a index=$index")
            assertClose(0f, coefficients.b[index], "b index=$index")
        }
    }

    @Test
    fun applyCoefficients_identityCoefficients_writeTheGuidanceAsAlpha() {
        // Given — a = 1, b = 0 이면 알파가 안내자 휘도 그대로여야 한다
        val alpha = ByteArray(4)
        val guidance = intArrayOf(gray(0), gray(128), gray(255), gray(64))
        val coefficients = GuidedCoefficients(a = FloatArray(4) { 1f }, b = FloatArray(4))

        // When
        val changed = applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 2,
            subWidth = 2,
            subHeight = 2,
            factor = 1,
        )

        // Then
        assertEquals(true, changed)
        assertEquals(0, alpha[0].toInt() and 0xFF)
        assertEquals(128, alpha[1].toInt() and 0xFF)
        assertEquals(255, alpha[2].toInt() and 0xFF)
        assertEquals(64, alpha[3].toInt() and 0xFF)
    }

    @Test
    fun applyCoefficients_resultMatchesTheCurrentAlpha_reportsNoChange() {
        // Given — 호출부가 이 값으로 원본 판 재사용을 판정한다. 늘 참이면 그 경로가 죽는다
        val alpha = ByteArray(2) { 255.toByte() }
        val guidance = intArrayOf(gray(255), gray(255))
        val coefficients = GuidedCoefficients(a = FloatArray(2) { 1f }, b = FloatArray(2))

        // When
        val changed = applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 1,
        )

        // Then
        assertEquals(false, changed)
    }

    @Test
    fun applyCoefficients_outOfRangeResult_isClampedInsteadOfWrapping() {
        // Given — 자르지 않으면 바이트가 감겨 반대 값이 된다
        val alpha = ByteArray(2)
        val guidance = intArrayOf(gray(255), gray(255))
        val coefficients = GuidedCoefficients(a = floatArrayOf(4f, -4f), b = FloatArray(2))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 1,
        )

        // Then
        assertEquals(255, alpha[0].toInt() and 0xFF)
        assertEquals(0, alpha[1].toInt() and 0xFF)
    }

    @Test
    fun applyCoefficients_verticallyUpscaledCoefficients_interpolateBetweenRows() {
        // Given — 세로로만 변하는 계수. 세로 보간을 빠뜨리면 계단 둘만 나온다
        val alpha = ByteArray(8)
        val guidance = IntArray(8) { gray(255) }
        val coefficients = GuidedCoefficients(a = FloatArray(2), b = floatArrayOf(0f, 1f))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 1,
            height = 8,
            subWidth = 1,
            subHeight = 2,
            factor = 4,
        )

        // Then
        val values = IntArray(8) { alpha[it].toInt() and 0xFF }
        for (index in 1 until 8) {
            assertTrue(values[index] >= values[index - 1], "index=$index values=${values.toList()}")
        }
        assertTrue(values.toSet().size > 2, "nearest 되올림이면 값이 둘뿐이다 values=${values.toList()}")
    }
}
