package com.teamyg.camera.impl.vm

import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface SystemCameraEffect : UiSideEffect {
    data object RequestPermission : SystemCameraEffect

    data object LaunchCamera : SystemCameraEffect

    data class ReturnResult(val uri: String) : SystemCameraEffect

    data object Back : SystemCameraEffect
}

sealed interface SystemCameraIntent : UiIntent {
    data class OnPermissionResult(val granted: Boolean) : SystemCameraIntent

    data object OnRequestPermission : SystemCameraIntent

    data object OnCaptureLaunched : SystemCameraIntent

    data class OnCaptureResult(
        val success: Boolean,
        val uri: String?,
    ) : SystemCameraIntent

    data object OnRetry : SystemCameraIntent

    data object OnCancel : SystemCameraIntent
}

sealed interface SystemCameraState : UiState {
    data object Init : SystemCameraState

    data object RequestingPermission : SystemCameraState

    data object Launching : SystemCameraState

    data object Capturing : SystemCameraState

    data object Failed : SystemCameraState
}

@HiltViewModel
class SystemCameraViewModel
    @Inject
    constructor() :
    BaseViewModel<SystemCameraState, SystemCameraIntent, SystemCameraEffect>(initialState = SystemCameraState.Init) {
        override fun processIntent(intent: SystemCameraIntent) {
            when (intent) {
                is SystemCameraIntent.OnPermissionResult -> handleOnPermissionResult(intent)
                is SystemCameraIntent.OnRequestPermission -> handleOnRequestPermission()
                is SystemCameraIntent.OnCaptureLaunched -> handleOnCaptureLaunched()
                is SystemCameraIntent.OnCaptureResult -> handleOnCaptureResult(intent)
                is SystemCameraIntent.OnRetry -> handleOnRetry()
                is SystemCameraIntent.OnCancel -> handleOnCancel()
            }
        }

        private fun handleOnPermissionResult(intent: SystemCameraIntent.OnPermissionResult) {
            val granted: Boolean = intent.granted

            if (granted.not()) {
                updateState { SystemCameraState.RequestingPermission }
                return
            }

            updateState { SystemCameraState.Launching }
            postSideEffect(SystemCameraEffect.LaunchCamera)
        }

        private fun handleOnRequestPermission() {
            postSideEffect(SystemCameraEffect.RequestPermission)
        }

        private fun handleOnCaptureLaunched() {
            updateState { SystemCameraState.Capturing }
        }

        private fun handleOnCaptureResult(intent: SystemCameraIntent.OnCaptureResult) {
            val success: Boolean = intent.success
            val uri: String? = intent.uri

            if (success && uri != null) {
                postSideEffect(SystemCameraEffect.ReturnResult(uri))
                return
            }

            updateState { SystemCameraState.Failed }
        }

        private fun handleOnRetry() {
            updateState { SystemCameraState.Launching }
            postSideEffect(SystemCameraEffect.LaunchCamera)
        }

        private fun handleOnCancel() {
            postSideEffect(SystemCameraEffect.Back)
        }
    }
