package com.teamyg.parfait.feature.camera.impl.model

import androidx.camera.core.Camera

/**
 * 마지막으로 카메라에 건 배율. 같은 카메라에서 배율만 바뀌면 여기서부터 부드럽게 옮긴다.
 */
internal class AppliedZoom {
    var camera: Camera? = null
    var ratio: Float = 1f
}
