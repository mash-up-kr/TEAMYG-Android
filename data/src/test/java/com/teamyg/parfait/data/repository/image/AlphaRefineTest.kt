package com.teamyg.parfait.data.repository.image

import kotlin.math.abs
import kotlin.test.Test
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
}
