package com.teamyg.parfait.feature.camera.impl.model

/**
 * 0.5x를 어떻게 내는가.
 */
enum class HalfZoomSource {
    NONE,

    /** 메인 카메라 줌 배율로 낸다 — 메인·초광각을 묶은 논리 카메라를 앱에 노출하는 기기 */
    ZOOM_RATIO,

    /** 초광각 카메라로 다시 바인딩해 낸다 — 초광각을 별도 카메라로만 노출하는 기기 */
    ULTRA_WIDE,
    ;

    companion object {
        fun of(
            range: ClosedFloatingPointRange<Float>,
            hasUltraWideCamera: Boolean,
        ): HalfZoomSource = when {
            CameraZoomLevel.HALF.ratio in range -> ZOOM_RATIO
            hasUltraWideCamera -> ULTRA_WIDE
            else -> NONE
        }
    }
}
