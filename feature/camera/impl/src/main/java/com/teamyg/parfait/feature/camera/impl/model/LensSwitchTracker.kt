package com.teamyg.parfait.feature.camera.impl.model

/**
 * 핀치로 메인 카메라와 초광각 카메라를 오갈지 판정한다. 문턱을 넘은 뒤 머무는 시간은 ViewModel이 잰다.
 *
 * 핀치 구간(두 손가락이 닿아 있는 동안)마다 [reset]해야 한다. 누적이 다음 핀치로 이어지면 문턱이 그만큼 낮아진다.
 */
class LensSwitchTracker {
    /** 렌즈 경계(초광각 0.5x·메인 최소 배율)를 넘어선 핀치 누적 */
    private var overshootScale = 1f

    /**
     * 이번 핀치 구간에 이미 렌즈를 바꿨다. 경계 떨림으로 재바인딩이 반복되지 않게 구간당 한 번만 바꾸고,
     * 남은 핀치는 버린다 — 이어 받으면 1x로 넘어가자마자 1.4x처럼 밀린다.
     */
    var hasSwitched: Boolean = false
        private set

    fun reset() {
        overshootScale = 1f
        hasSwitched = false
    }

    /** 초광각은 메인 카메라와 배율 축이 달라 그 안에서 줌하지 않는다. 확대 누적만 본다 */
    fun onUltraWidePinch(scale: Float): CameraZoomLevel? {
        overshootScale = (overshootScale * scale).coerceAtLeast(1f)
        return CameraZoomLevel.ONE.takeIf { overshootScale >= ZOOM_IN_SCALE }
    }

    /** 최소 배율 아래로 내려간 축소만 누적한다 */
    fun onMainPinch(
        rawRatio: Float,
        minRatio: Float,
        hasUltraWideCamera: Boolean,
    ): CameraZoomLevel? {
        overshootScale = if (rawRatio < minRatio) overshootScale * rawRatio / minRatio else 1f
        return CameraZoomLevel.HALF.takeIf { hasUltraWideCamera && overshootScale <= ZOOM_OUT_SCALE }
    }

    fun onSwitched() {
        overshootScale = 1f
        hasSwitched = true
    }

    private companion object {
        const val ZOOM_IN_SCALE = 1.25f
        const val ZOOM_OUT_SCALE = 1f / ZOOM_IN_SCALE
    }
}
