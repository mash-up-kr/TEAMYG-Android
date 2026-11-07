package com.teamyg.parfait.feature.camera.impl.model

import android.graphics.Bitmap

internal data class CapturedFrame(
    val bitmap: Bitmap,
    val lensFacing: LensFacing,
)
