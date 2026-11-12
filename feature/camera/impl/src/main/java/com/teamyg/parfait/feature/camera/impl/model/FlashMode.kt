package com.teamyg.parfait.feature.camera.impl.model

import androidx.camera.core.ImageCapture

enum class FlashMode(
    val imageCaptureFlashMode: Int,
) {
    OFF(ImageCapture.FLASH_MODE_OFF),
    ON(ImageCapture.FLASH_MODE_ON),
    ;

    operator fun not(): FlashMode = if (this == ON) OFF else ON
}
