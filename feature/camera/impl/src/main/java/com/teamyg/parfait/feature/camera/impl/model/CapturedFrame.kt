package com.teamyg.parfait.feature.camera.impl.model

import android.graphics.Bitmap

/**
 * 바인딩이 풀릴 때 잡아 둔 마지막 프리뷰 프레임과 그때의 렌즈 방향.
 */
data class CapturedFrame(
    val bitmap: Bitmap,
    val lensFacing: Int,
)
