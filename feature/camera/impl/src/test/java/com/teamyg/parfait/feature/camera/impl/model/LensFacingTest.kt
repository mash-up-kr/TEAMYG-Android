package com.teamyg.parfait.feature.camera.impl.model

import androidx.camera.core.CameraSelector
import kotlin.test.Test
import kotlin.test.assertEquals

class LensFacingTest {
    @Test
    fun flipped_back_isFront() {
        assertEquals(LensFacing.FRONT, LensFacing.BACK.flipped())
    }

    @Test
    fun flipped_front_isBack() {
        assertEquals(LensFacing.BACK, LensFacing.FRONT.flipped())
    }

    @Test
    fun cameraSelectorLens_matchesCameraX() {
        assertEquals(CameraSelector.LENS_FACING_BACK, LensFacing.BACK.cameraSelectorLens)
        assertEquals(CameraSelector.LENS_FACING_FRONT, LensFacing.FRONT.cameraSelectorLens)
    }
}
