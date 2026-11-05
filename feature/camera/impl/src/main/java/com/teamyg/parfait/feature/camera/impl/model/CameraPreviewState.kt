package com.teamyg.parfait.feature.camera.impl.model

import androidx.camera.core.Camera
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class CameraPreviewState(
    val previewView: PreviewView,
) {
    var camera: Camera? by mutableStateOf(null)
    var transition: CameraTransition? by mutableStateOf(null)

    // 직전 바인딩이 dispose 때 남긴 프레임. 다음 바인딩이 덮개로 쓴다
    var lastFrame: CapturedFrame? = null

    // 한 번 안 열린 초광각은 다시 쓰지 않는다. 안 막으면 메인 재바인딩에서 0.5x가 되살아난다
    val failedUltraWideIds: MutableSet<String> = mutableSetOf()

    // 촬영은 이 값으로 크롭 기준을 고른다. false 면 ImageProxy.cropRect 가 피드에 보이는 영역이 아니다
    var isViewPortApplied: Boolean = false
}
