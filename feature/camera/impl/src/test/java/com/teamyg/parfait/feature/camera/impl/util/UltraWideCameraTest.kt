package com.teamyg.parfait.feature.camera.impl.util

import com.teamyg.parfait.feature.camera.impl.model.LensOptics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UltraWideCameraTest {
    private val main = LensOptics(cameraId = "0", sensorWidthMm = 6.4f, minFocalLengthMm = 4.69f)

    @Test
    fun ultraWide_a35Like_isFound() {
        val ultraWide = LensOptics(cameraId = "2", sensorWidthMm = 4.0f, minFocalLengthMm = 1.64f)

        assertEquals(ultraWide, findUltraWideCamera(main = main, backCameras = listOf(main, ultraWide)))
    }

    @Test
    fun ultraWide_macroOnly_isNull() {
        // 초점거리는 메인보다 짧지만 센서가 작아 화각은 메인과 비슷하다
        val macro = LensOptics(cameraId = "3", sensorWidthMm = 2.0f, minFocalLengthMm = 1.8f)

        assertNull(findUltraWideCamera(main = main, backCameras = listOf(main, macro)))
    }

    @Test
    fun ultraWide_several_picksWidest() {
        val widest = LensOptics(cameraId = "2", sensorWidthMm = 4.0f, minFocalLengthMm = 1.64f)
        val wide = LensOptics(cameraId = "4", sensorWidthMm = 4.0f, minFocalLengthMm = 2.2f)

        assertEquals(widest, findUltraWideCamera(main = main, backCameras = listOf(main, wide, widest)))
    }

    @Test
    fun ultraWide_onlyMain_isNull() {
        assertNull(findUltraWideCamera(main = main, backCameras = listOf(main)))
    }
}
