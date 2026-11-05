package com.teamyg.parfait.feature.camera.impl.model

import androidx.compose.ui.graphics.ImageBitmap

internal data class CameraTransition(
    val frame: ImageBitmap,
    val isFlip: Boolean,
)
