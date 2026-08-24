package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertContentEquals

// 4x4 원본(rowStride 4) 안의 2x2 부분 영역. 셀마다 다른 값을 둬서 left·top 오프셋 항 중
// 하나라도 빠지면 엉뚱한 셀을 읽어 값이 어긋나게 만든다.
private val BOUNDS = SegmentationBounds(left = 1, top = 1, right = 3, bottom = 3)

class AlphaCompositeTest {
    @Test
    fun applyAlphaInPlace_offsetsBoundsIntoOriginCoordinates_andZerosRgbWhenAlphaIsZero() {
        // Given — bounds 안 4셀이 원본의 index 5·6·9·10 에 해당한다. alpha[9] 를 0 으로 둬서
        // 완전 투명 분기도 같이 검증한다
        val alpha = ByteArray(16) { (it + 1).toByte() }
        alpha[9] = 0
        val pixels = intArrayOf(0xFF000001.toInt(), 0xFF000002.toInt(), 0xFF000003.toInt(), 0xFF000004.toInt())

        // When
        applyAlphaInPlace(pixels, alpha, alphaRowStride = 4, bounds = BOUNDS)

        // Then
        val expected = intArrayOf(
            (6 shl 24) or 1, // alpha[5]=6, pixels[0] RGB=1
            (7 shl 24) or 2, // alpha[6]=7, pixels[1] RGB=2
            0, // alpha[9]=0 → 완전 투명, RGB 도 0
            (11 shl 24) or 4, // alpha[10]=11, pixels[3] RGB=4
        )
        assertContentEquals(expected, pixels)
    }

    @Test
    fun composeCroppedArgb_sharesOneRowStrideForBothPixelsAndAlpha_andZerosRgbWhenAlphaIsZero() {
        // Given — pixels·alpha 둘 다 같은 rowStride(4) 로 읽으므로 index 5·6·9·10 이 그대로
        // 소스다. alpha[9] 를 0 으로 둬서 완전 투명 분기도 같이 검증한다
        val pixels = IntArray(16) { it }
        val alpha = ByteArray(16) { (it + 1).toByte() }
        alpha[9] = 0

        // When
        val cropped = composeCroppedArgb(pixels, alpha, rowStride = 4, bounds = BOUNDS)

        // Then
        val expected = intArrayOf(
            (6 shl 24) or 5, // alpha[5]=6, pixels[5]=5
            (7 shl 24) or 6, // alpha[6]=7, pixels[6]=6
            0, // alpha[9]=0 → 완전 투명, RGB 도 0
            (11 shl 24) or 10, // alpha[10]=11, pixels[10]=10
        )
        assertContentEquals(expected, cropped)
    }
}
