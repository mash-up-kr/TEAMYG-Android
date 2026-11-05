package com.teamyg.parfait.feature.camera.impl.model

internal data class LensOptics(
    val cameraId: String,
    val sensorWidthMm: Float,
    val minFocalLengthMm: Float,
) {
    /** 클수록 화각이 넓다. 같은 기기 안의 카메라끼리만 비교한다 */
    val fieldOfViewIndex: Float
        get() = sensorWidthMm / minFocalLengthMm
}
