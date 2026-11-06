package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.core.CameraState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Observer

@Composable
internal fun CameraZoomEffect(
    camera: Camera?,
    zoomRatio: Float,
) {
    // 카메라가 바뀌면 새 슬롯이 현재 배율로 시작해 애니메이션 없이 바로 걸린다
    val appliedRatio = remember(camera) { mutableFloatStateOf(zoomRatio) }
    LaunchedEffect(camera, zoomRatio) {
        val target = camera ?: return@LaunchedEffect
        if (appliedRatio.floatValue == zoomRatio) {
            target.cameraControl.setZoomRatio(zoomRatio)
            return@LaunchedEffect
        }

        animate(
            initialValue = appliedRatio.floatValue,
            targetValue = zoomRatio,
            animationSpec = tween(durationMillis = ZOOM_ANIMATION_MILLIS),
        ) { value, _ ->
            appliedRatio.floatValue = value
            target.cameraControl.setZoomRatio(value)
        }
    }

    // CameraX는 카메라가 닫히면(ON_STOP) 줌을 1.0으로 되돌리고, 복귀 때는 위 키가 그대로라 다시 걸지 않는다
    val currentZoomRatio by rememberUpdatedState(zoomRatio)
    DisposableEffect(camera) {
        val target = camera ?: return@DisposableEffect onDispose { }
        val observer = Observer<CameraState> { cameraState ->
            if (cameraState.type == CameraState.Type.OPEN) {
                target.cameraControl.setZoomRatio(currentZoomRatio)
            }
        }
        target.cameraInfo.cameraState.observeForever(observer)

        onDispose { target.cameraInfo.cameraState.removeObserver(observer) }
    }
}

private const val ZOOM_ANIMATION_MILLIS = 200
