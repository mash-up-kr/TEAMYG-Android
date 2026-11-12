package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Observer
import com.teamyg.parfait.feature.camera.impl.model.CameraPreviewState
import kotlinx.coroutines.delay

@Composable
internal fun CameraTransitionEffect(state: CameraPreviewState) {
    // 덮개를 건 직후엔 이전 스트림의 STREAMING이 남아 있을 수 있어 IDLE을 본 뒤의 STREAMING만 센다
    val hasTransition = state.transition != null
    DisposableEffect(state, hasTransition) {
        if (!hasTransition) return@DisposableEffect onDispose { }

        var sawIdle = false
        val observer = Observer<PreviewView.StreamState> { streamState ->
            when (streamState) {
                PreviewView.StreamState.IDLE -> sawIdle = true
                PreviewView.StreamState.STREAMING -> if (sawIdle) state.transition = null
            }
        }
        state.previewView.previewStreamState.observeForever(observer)

        onDispose { state.previewView.previewStreamState.removeObserver(observer) }
    }

    // IDLE을 건너뛰는 기기에서도 덮개가 남지 않게 한다
    val transition = state.transition
    LaunchedEffect(transition) {
        if (transition == null) return@LaunchedEffect
        delay(TRANSITION_TIMEOUT_MILLIS)
        if (state.transition === transition) state.transition = null
    }
}

private const val TRANSITION_TIMEOUT_MILLIS = 2_000L
