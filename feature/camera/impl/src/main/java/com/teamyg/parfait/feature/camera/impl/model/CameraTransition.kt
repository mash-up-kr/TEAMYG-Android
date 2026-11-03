package com.teamyg.parfait.feature.camera.impl.model

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 카메라를 다시 여는 동안 검정 화면 대신 덮을 직전 프레임.
 */
data class CameraTransition(
    val frame: ImageBitmap,
    val isFlip: Boolean,
)
