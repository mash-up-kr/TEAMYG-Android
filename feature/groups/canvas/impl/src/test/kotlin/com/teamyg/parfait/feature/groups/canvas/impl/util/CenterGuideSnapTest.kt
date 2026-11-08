package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CenterGuideSnapTest {
    @Test
    fun resolveCenterGuideAxisDp_exactlyAtThreshold_snaps() {
        val result = resolveCenterGuideAxisDp(rawCenterDp = 106f, canvasCenterDp = 100f, thresholdDp = 6f)

        assertTrue(result.isSnapped)
        assertEquals(100f, result.effectiveValue)
    }

    @Test
    fun resolveCenterGuideAxisDp_justInsideThreshold_snaps() {
        val result = resolveCenterGuideAxisDp(rawCenterDp = 105.9f, canvasCenterDp = 100f, thresholdDp = 6f)

        assertTrue(result.isSnapped)
        assertEquals(100f, result.effectiveValue)
    }

    @Test
    fun resolveCenterGuideAxisDp_justOutsideThreshold_doesNotSnap() {
        val result = resolveCenterGuideAxisDp(rawCenterDp = 106.1f, canvasCenterDp = 100f, thresholdDp = 6f)

        assertFalse(result.isSnapped)
        assertEquals(106.1f, result.effectiveValue)
    }

    @Test
    fun resolveCenterGuideAxisDp_belowCenter_snapsSymmetrically() {
        val result = resolveCenterGuideAxisDp(rawCenterDp = 94.1f, canvasCenterDp = 100f, thresholdDp = 6f)

        assertTrue(result.isSnapped)
        assertEquals(100f, result.effectiveValue)
    }

    @Test
    fun resolveCenterGuideAxisRatio_exactlyAtThreshold_snapsToHalf() {
        val result = resolveCenterGuideAxisRatio(rawRatio = 0.52f, thresholdRatio = 0.02f)

        assertTrue(result.isSnapped)
        assertEquals(0.5f, result.effectiveValue)
    }

    @Test
    fun resolveCenterGuideAxisRatio_justOutsideThreshold_doesNotSnap() {
        val result = resolveCenterGuideAxisRatio(rawRatio = 0.521f, thresholdRatio = 0.02f)

        assertFalse(result.isSnapped)
        assertEquals(0.521f, result.effectiveValue)
    }

    @Test
    fun resolveCenterGuideAxisRatio_negativeThreshold_neverSnapsEvenExactlyAtCenter() {
        val result = resolveCenterGuideAxisRatio(rawRatio = 0.5f, thresholdRatio = -1f)

        assertFalse(result.isSnapped)
        assertEquals(0.5f, result.effectiveValue)
    }
}
