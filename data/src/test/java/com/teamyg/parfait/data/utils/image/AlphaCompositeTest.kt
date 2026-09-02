package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertContentEquals

// 5x4 원본(rowStride 5) 안의 3x2 부분 영역. 폭≠높이·left(1)≠top(2) 로 비정사각·비대칭이라
// x/y 를 뒤바꾸거나 left·top 을 맞바꾸는 변이도 값이 어긋나 걸린다.
// 덮이는 원본 index 는 (top+y)*5+(left+x) 로 y=0,1 / x=0,1,2 일 때 11·12·13·16·17·18.
private val BOUNDS = SegmentationBounds(left = 1, top = 2, right = 4, bottom = 4)

class AlphaCompositeTest {
    @Test
    fun applyAlphaInPlace_offsetsBoundsIntoOriginCoordinates_andZerosRgbWhenAlphaIsZero() {
        // Given — alpha 는 원본 좌표계(rowStride 5)라 위 6개 index 에서 읽힌다. pixels 는 이미
        // bounds 크기로 잘려 있어 로컬 순서(y*3+x)로만 채운다. alpha[17](로컬 (1,1)) 을 0 으로
        // 둬서 완전 투명 분기도 같이 검증한다
        val alpha = ByteArray(20) { (it + 1).toByte() }
        alpha[17] = 0
        val pixels = intArrayOf(
            0xFF000001.toInt(),
            0xFF000002.toInt(),
            0xFF000003.toInt(),
            0xFF000004.toInt(),
            0xFF000005.toInt(),
            0xFF000006.toInt(),
        )

        // When
        applyAlphaInPlace(pixels, alpha, alphaRowStride = 5, bounds = BOUNDS)

        // Then — 로컬 (0,0)..(1,2) 가 각각 원본 index 11·12·13·16·17·18 을 읽는다
        val expected = intArrayOf(
            (12 shl 24) or 1, // alpha[11]=12, pixels[0] RGB=1
            (13 shl 24) or 2, // alpha[12]=13, pixels[1] RGB=2
            (14 shl 24) or 3, // alpha[13]=14, pixels[2] RGB=3
            (17 shl 24) or 4, // alpha[16]=17, pixels[3] RGB=4
            0, // alpha[17]=0 → 완전 투명, RGB 도 0
            (19 shl 24) or 6, // alpha[18]=19, pixels[5] RGB=6
        )
        assertContentEquals(expected, pixels)
    }

    @Test
    fun composeCroppedArgb_sharesOneRowStrideForBothPixelsAndAlpha_andZerosRgbWhenAlphaIsZero() {
        // Given — pixels·alpha 둘 다 같은 rowStride(5) 로 읽으므로 index 11·12·13·16·17·18 이
        // 그대로 소스다. pixels 의 상위 바이트를 0xFF 로 채워 둬서(원본 알파 흔적) `and
        // 0x00FFFFFF` 마스킹이 빠지면 이 테스트만으로 깨지게 만든다. alpha[17](로컬 (1,1)) 을
        // 0 으로 둬서 완전 투명 분기도 같이 검증한다
        val pixels = IntArray(20) { 0xFF000000.toInt() or it }
        val alpha = ByteArray(20) { (it + 1).toByte() }
        alpha[17] = 0

        // When
        val cropped = composeCroppedArgb(pixels, alpha, rowStride = 5, bounds = BOUNDS)

        // Then — 로컬 (0,0)..(1,2) 가 각각 원본 index 11·12·13·16·17·18 을 읽는다.
        // 마스킹이 살아 있으면 결과의 RGB 는 pixels 하위 24비트(=index 값)이고 알파만 바뀐다
        val expected = intArrayOf(
            (12 shl 24) or 11, // alpha[11]=12, pixels[11] RGB=0x0B
            (13 shl 24) or 12, // alpha[12]=13, pixels[12] RGB=0x0C
            (14 shl 24) or 13, // alpha[13]=14, pixels[13] RGB=0x0D
            (17 shl 24) or 16, // alpha[16]=17, pixels[16] RGB=0x10
            0, // alpha[17]=0 → 완전 투명, RGB 도 0
            (19 shl 24) or 18, // alpha[18]=19, pixels[18] RGB=0x12
        )
        assertContentEquals(expected, cropped)
    }
}
