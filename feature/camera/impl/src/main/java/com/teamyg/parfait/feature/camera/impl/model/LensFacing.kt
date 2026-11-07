package com.teamyg.parfait.feature.camera.impl.model

import androidx.camera.core.CameraSelector

enum class LensFacing(
    val cameraSelectorLens: Int,
) {
    BACK(CameraSelector.LENS_FACING_BACK),
    FRONT(CameraSelector.LENS_FACING_FRONT),
    ;

    fun flipped(): LensFacing = if (this == BACK) FRONT else BACK
}
