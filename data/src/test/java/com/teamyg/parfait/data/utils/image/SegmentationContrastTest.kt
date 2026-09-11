package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.data.model.image.SegmentationContrastSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SegmentationContrastTest {
    private fun band(
        from: Int,
        to: Int,
        count: Int = 1_000,
    ) = IntArray(SegmentationContrastSpec.LUMINANCE_LEVELS).also { histogram ->
        for (level in from..to) histogram[level] = count
    }

    @Test
    fun contrastLut_emptyHistogram_isIdentity() {
        val lut = contrastLut(IntArray(SegmentationContrastSpec.LUMINANCE_LEVELS))

        assertTrue((0 until SegmentationContrastSpec.LUMINANCE_LEVELS).all { lut[it] == it })
    }

    @Test
    fun contrastLut_everythingOnOneLevel_isIdentity() {
        // Given 단색 — 절단점이 겹쳐 분모가 0 이 된다
        val lut = contrastLut(band(from = 128, to = 128, count = 10_000))

        assertTrue((0 until SegmentationContrastSpec.LUMINANCE_LEVELS).all { lut[it] == it })
    }

    @Test
    fun contrastLut_narrowBand_stretchesItToTheFullRange() {
        val lut = contrastLut(band(from = 100, to = 150))

        assertTrue(lut[100] <= 16)
        assertTrue(lut[150] >= 239)
    }

    @Test
    fun contrastLut_outliersAtBothEnds_areClippedByThePercentiles() {
        // Given 본체는 100..150 인데 양 끝에 이상치 한 픽셀씩 — 최소·최대로 늘리면 대역이 거의 안 벌어진다
        val histogram = band(from = 100, to = 150)
        histogram[0] = 1
        histogram[SegmentationContrastSpec.LUMINANCE_LEVELS - 1] = 1

        val lut = contrastLut(histogram)

        assertTrue(lut[100] <= 16)
        assertTrue(lut[150] >= 239)
    }

    @Test
    fun contrastLut_isMonotonic() {
        val histogram = IntArray(SegmentationContrastSpec.LUMINANCE_LEVELS)
        for (level in 30..220) histogram[level] = level

        val lut = contrastLut(histogram)

        assertTrue((1 until SegmentationContrastSpec.LUMINANCE_LEVELS).all { lut[it] >= lut[it - 1] })
    }

    @Test
    fun contrastLut_clampsOutsideTheBand() {
        val lut = contrastLut(band(from = 100, to = 150))

        assertEquals(0, lut[0])
        assertEquals(255, lut[SegmentationContrastSpec.LUMINANCE_LEVELS - 1])
    }
}
