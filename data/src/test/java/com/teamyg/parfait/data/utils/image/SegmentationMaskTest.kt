package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.data.model.image.ProjectedRegion
import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val TEST_OPTIONS = AlphaPostProcessOptions(
    downscaleFactor = 1,
    areaOpeningMinPixels = 4,
    erodeEdge = false,
)

private fun confidenceBuffer(values: FloatArray): FloatBuffer = FloatBuffer.wrap(values)

class SegmentationMaskTest {
    @Test
    fun confidenceToAlpha_atOrBelowTheRampFloor_isFullyTransparent() {
        // Given · When · Then
        assertEquals(0, confidenceToAlpha(0f))
        assertEquals(0, confidenceToAlpha(0.35f))
    }

    @Test
    fun confidenceToAlpha_atOrAboveTheRampCeiling_isFullyOpaque() {
        // Given · When · Then
        assertEquals(255, confidenceToAlpha(0.65f))
        assertEquals(255, confidenceToAlpha(1f))
    }

    @Test
    fun confidenceToAlpha_exactlyAtTheOldThreshold_staysBackground() {
        // Given — 종전 상수는 "이 값을 넘는" 신뢰도만 객체로 봤다. 램프도 그 경계를 지켜야 한다.
        // 알파 > 127 은 신뢰도 0.5 가 아니라 0.35 + 128 × 0.3 / 255 ≈ 0.5006 이다

        // When · Then
        assertEquals(127, confidenceToAlpha(0.5f))
        assertEquals(128, confidenceToAlpha(0.5006f))
    }

    @Test
    fun maskSubjectAlpha_confidentBlobWithASpeck_dropsTheSpeck() = runTest {
        // Given — 8×8. 왼쪽 위 4×4 는 확실하고 오른쪽 아래 한 점만 튄다
        val values = FloatArray(64)
        for (y in 0 until 4) for (x in 0 until 4) values[y * 8 + x] = 1f
        values[63] = 1f

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertEquals(
            0,
            masked
                ?.alpha
                ?.get(63)
                ?.toInt()
                ?.and(0xFF),
        )
        assertEquals(4, masked?.result?.bounds?.right)
    }

    @Test
    fun maskSubjectAlpha_everyPixelIsConfident_coversTheWholePlate() = runTest {
        // Given — 프레임에 걸친 피사체가 테두리를 잃지 않는지 본다
        val values = FloatArray(64) { 1f }

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertEquals(8, masked?.result?.bounds?.right)
        assertEquals(8, masked?.result?.bounds?.bottom)
    }

    @Test
    fun maskSubjectAlpha_nothingIsConfident_returnsNull() = runTest {
        // Given
        val values = FloatArray(64) { 0.1f }

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertNull(masked)
    }

    @Test
    fun maskSubjectAlpha_bufferLimitBelowCapacity_stopsAtLimitInsteadOfReadingPastIt() = runTest {
        // Given — absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼는다
        val buffer = FloatBuffer.allocate(64).apply { limit(10) }

        // When · Then
        assertFailsWith<IndexOutOfBoundsException> {
            maskSubjectAlpha(buffer, width = 8, height = 8, options = TEST_OPTIONS)
        }
    }

    @Test
    fun maskSubjectAlpha_cancelledAtFirstCheck_throws() {
        // Given
        val values = FloatArray(64) { 1f }
        val job = CountingJob()
        job.cancelAfter = 0

        // When · Then — maskSubjectAlpha 자체에는 확인 루프가 없다. postProcessAlpha 아래
        // downscaleMask 의 첫 행에서 걸린다
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)
            }
        }
    }

    @Test
    fun confidenceToAlphaArray_mapsEveryPixelThroughTheRamp() {
        val mask = FloatBuffer.wrap(floatArrayOf(0.0f, 0.5f, 1.0f, 0.2f))

        val alpha = confidenceToAlphaArray(mask, width = 2, height = 2)

        assertEquals(confidenceToAlpha(0.0f), alpha[0].toInt() and 0xFF)
        assertEquals(confidenceToAlpha(0.5f), alpha[1].toInt() and 0xFF)
        assertEquals(confidenceToAlpha(1.0f), alpha[2].toInt() and 0xFF)
        assertEquals(confidenceToAlpha(0.2f), alpha[3].toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_sameSize_returnsTheSameValues() {
        val alpha = byteArrayOf(0, 64, 128.toByte(), 255.toByte())

        assertContentEquals(alpha, resampleAlpha(alpha, 2, 2, 2, 2))
    }

    @Test
    fun resampleAlpha_upscaleTwoToThree_interpolatesTheMiddle() {
        // Given 한 줄 두 칸 — 박스 평균이나 최근접으로 바꾸면 가운데가 0 이나 254 가 된다
        val alpha = byteArrayOf(0, 254.toByte())

        val resampled = resampleAlpha(alpha, width = 2, height = 1, targetWidth = 3, targetHeight = 1)

        assertEquals(127, resampled[1].toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_upscale_keepsTheCorners() {
        val alpha = byteArrayOf(0, 255.toByte(), 0, 255.toByte())

        val resampled = resampleAlpha(alpha, 2, 2, 4, 4)

        assertEquals(16, resampled.size)
        assertEquals(0, resampled[0].toInt() and 0xFF)
        assertEquals(255, resampled[3].toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_downscale_averagesTheBox() {
        val alpha = byteArrayOf(0, 100, 100, 200.toByte())

        val resampled = resampleAlpha(alpha, 2, 2, 1, 1)

        assertEquals(100, resampled.single().toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_lengthDoesNotMatch_throws() {
        assertFailsWith<IllegalArgumentException> { resampleAlpha(ByteArray(3), 2, 2, 2, 2) }
    }

    @Test
    fun cropAlpha_middleRegion_copiesOnlyThatRegion() {
        val alpha = ByteArray(9) { it.toByte() }

        val cropped = cropAlpha(alpha, width = 3, height = 3, region = SegmentationBounds(1, 1, 3, 3))

        assertContentEquals(byteArrayOf(4, 5, 7, 8), cropped)
    }

    @Test
    fun cropAlpha_regionOutsideTheSource_throws() {
        assertFailsWith<IllegalArgumentException> {
            cropAlpha(ByteArray(9), width = 3, height = 3, region = SegmentationBounds(1, 1, 4, 3))
        }
    }

    @Test
    fun projectAlpha_mappedEqualsClipped_isPlainResample() {
        val alpha = byteArrayOf(0, 100, 100, 200.toByte())
        val mapped = SegmentationBounds(left = 10, top = 10, right = 11, bottom = 11)
        val projected = ProjectedRegion(mapped = mapped, clipped = mapped)

        val result = projectAlpha(alpha, width = 2, height = 2, projected = projected)

        assertContentEquals(resampleAlpha(alpha, 2, 2, 1, 1), result)
    }

    @Test
    fun projectAlpha_mappedAndClippedDiffer_cropsAfterResamplingAtTheMappedOffset() {
        // Given 2x2 원본을 4x4 로 재표본하고 그중 우상단 2x2 만 자른다 — 잘린 크기로 바로 재표본하면
        // 코너 값이 달라진다
        val alpha = byteArrayOf(0, 255.toByte(), 0, 255.toByte())
        val mapped = SegmentationBounds(left = 100, top = 100, right = 104, bottom = 104)
        val clipped = SegmentationBounds(left = 102, top = 100, right = 104, bottom = 102)
        val projected = ProjectedRegion(mapped = mapped, clipped = clipped)

        val result = projectAlpha(alpha, width = 2, height = 2, projected = projected)

        assertEquals(4, result.size)
        // mapped 의 우상단 코너(재표본에서 255 로 보존)가 잘린 조각의 (1,0) 자리에 온다
        assertEquals(255, result[1].toInt() and 0xFF)
    }

    @Test
    fun alphaSum_countsBytesAsUnsigned() {
        // 부호 있는 합이면 255 가 -1 로 세어진다
        assertEquals(256L, alphaSum(byteArrayOf(255.toByte(), 1)))
    }
}
