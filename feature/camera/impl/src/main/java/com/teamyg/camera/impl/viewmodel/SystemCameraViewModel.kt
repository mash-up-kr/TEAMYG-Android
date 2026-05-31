package com.teamyg.camera.impl.viewmodel

import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface SystemCameraEffect : UiSideEffect {
    data object RequestPermission : SystemCameraEffect

    data object OpenAppSettings : SystemCameraEffect

    data object LaunchCamera : SystemCameraEffect

    data class ReturnResult(val uri: String) : SystemCameraEffect

    data object NavigateToBack : SystemCameraEffect
}

sealed interface SystemCameraIntent : UiIntent {
    data class OnPermissionResult(val granted: Boolean) : SystemCameraIntent

    data class OnPermissionRequestResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean,
    ) : SystemCameraIntent

    data object OnRequestPermission : SystemCameraIntent

    data object OnOpenAppSettings : SystemCameraIntent

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

    data class RequestingPermission(
        val permanentlyDenied: Boolean = false,
    ) : SystemCameraState

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
                is SystemCameraIntent.OnPermissionRequestResult -> handleOnPermissionRequestResult(intent)
                is SystemCameraIntent.OnRequestPermission -> handleOnRequestPermission()
                is SystemCameraIntent.OnOpenAppSettings -> handleOnOpenAppSettings()
                is SystemCameraIntent.OnCaptureLaunched -> handleOnCaptureLaunched()
                is SystemCameraIntent.OnCaptureResult -> handleOnCaptureResult(intent)
                is SystemCameraIntent.OnRetry -> handleOnRetry()
                is SystemCameraIntent.OnCancel -> handleOnCancel()
            }
        }

        private fun handleOnPermissionResult(intent: SystemCameraIntent.OnPermissionResult) {
            if (intent.granted.not()) {
                val current = state.value
                val permanentlyDenied: Boolean = (current as? SystemCameraState.RequestingPermission)
                    ?.permanentlyDenied
                    ?: false

                updateState { SystemCameraState.RequestingPermission(permanentlyDenied = permanentlyDenied) }
                return
            }

            updateState { SystemCameraState.Launching }
            postSideEffect(SystemCameraEffect.LaunchCamera)
        }

        private fun handleOnPermissionRequestResult(intent: SystemCameraIntent.OnPermissionRequestResult) {
            if (intent.granted.not()) {
                updateState {
                    SystemCameraState.RequestingPermission(
                        permanentlyDenied = !intent.shouldShowRationale,
                    )
                }
                return
            }

            updateState { SystemCameraState.Launching }
            postSideEffect(SystemCameraEffect.LaunchCamera)
        }

        private fun handleOnRequestPermission() {
            postSideEffect(SystemCameraEffect.RequestPermission)
        }

        private fun handleOnOpenAppSettings() {
            postSideEffect(SystemCameraEffect.OpenAppSettings)
        }

        private fun handleOnCaptureLaunched() {
            updateState { SystemCameraState.Capturing }
        }

        private fun handleOnCaptureResult(intent: SystemCameraIntent.OnCaptureResult) {
            val success: Boolean = intent.success
            val uri: String? = intent.uri

            if (success.not()) {
                updateState { SystemCameraState.Failed }
                return
            }

            if (uri == null) {
                updateState { SystemCameraState.Failed }
                return
            }

            postSideEffect(SystemCameraEffect.ReturnResult(uri))
        }

        private fun handleOnRetry() {
            updateState { SystemCameraState.Launching }
            postSideEffect(SystemCameraEffect.LaunchCamera)
        }

        private fun handleOnCancel() {
            postSideEffect(SystemCameraEffect.NavigateToBack)
        }
    }
