package com.teamyg.parfait.feature.camera.impl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LensSwitchTrackerTest {
    @Test
    fun ultraWide_zoomInPastThreshold_targetsOne() {
        val tracker = LensSwitchTracker()

        assertNull(tracker.onUltraWidePinch(1.1f))
        assertEquals(CameraZoomLevel.ONE, tracker.onUltraWidePinch(1.2f))
    }

    @Test
    fun ultraWide_zoomOutFirst_doesNotDelayZoomIn() {
        val tracker = LensSwitchTracker()
        tracker.onUltraWidePinch(0.5f)

        assertEquals(CameraZoomLevel.ONE, tracker.onUltraWidePinch(1.3f))
    }

    @Test
    fun main_zoomOutPastMin_targetsHalf() {
        val tracker = LensSwitchTracker()

        assertNull(tracker.onMainPinch(rawRatio = 0.9f, minRatio = 1f, hasUltraWideCamera = true))
        assertEquals(
            CameraZoomLevel.HALF,
            tracker.onMainPinch(rawRatio = 0.85f, minRatio = 1f, hasUltraWideCamera = true),
        )
    }

    @Test
    fun main_aboveMin_resetsOvershoot() {
        val tracker = LensSwitchTracker()
        tracker.onMainPinch(rawRatio = 0.85f, minRatio = 1f, hasUltraWideCamera = true)
        tracker.onMainPinch(rawRatio = 1.2f, minRatio = 1f, hasUltraWideCamera = true)

        assertNull(tracker.onMainPinch(rawRatio = 0.9f, minRatio = 1f, hasUltraWideCamera = true))
    }

    @Test
    fun main_withoutUltraWide_neverTargets() {
        val tracker = LensSwitchTracker()

        assertNull(tracker.onMainPinch(rawRatio = 0.5f, minRatio = 1f, hasUltraWideCamera = false))
    }

    @Test
    fun reset_clearsOvershootAndSwitch() {
        val tracker = LensSwitchTracker()
        tracker.onUltraWidePinch(1.15f)
        tracker.onSwitched()

        tracker.reset()

        assertFalse(tracker.hasSwitched)
        assertNull(tracker.onUltraWidePinch(1.1f))
    }

    @Test
    fun onSwitched_locksUntilReset() {
        val tracker = LensSwitchTracker()

        tracker.onSwitched()

        assertTrue(tracker.hasSwitched)
    }
}
