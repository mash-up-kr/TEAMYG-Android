package com.teamyg.camera.impl.vm

import androidx.camera.core.CameraSelector
import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface CustomCameraEffect : UiSideEffect {
    data object RequestPermission : CustomCameraEffect

    data object CaptureImage : CustomCameraEffect

    data class ReturnResult(
        val uri: String,
    ) : CustomCameraEffect

    data object NavigateToBack : CustomCameraEffect
}

sealed interface CustomCameraIntent : UiIntent {
    data class OnPermissionResult(
        val granted: Boolean,
    ) : CustomCameraIntent

    data object OnRequestPermission : CustomCameraIntent

    data class OnZoomRangeReady(
        val range: ClosedFloatingPointRange<Float>,
    ) : CustomCameraIntent

    data class OnClickZoomLevel(
        val level: Float,
    ) : CustomCameraIntent

    data object OnClickFlip : CustomCameraIntent

    data object OnClickShutter : CustomCameraIntent

    data class OnCaptureSaved(
        val uri: String,
    ) : CustomCameraIntent

    data object OnCaptureFailed : CustomCameraIntent

    data object OnCancel : CustomCameraIntent
}

data class CustomCameraState(
    val hasPermission: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val zoomRatio: Float = 1f,
    val zoomRange: ClosedFloatingPointRange<Float> = 1f..1f,
) : UiState

@HiltViewModel
class CustomCameraViewModel
    @Inject
    constructor() :
    BaseViewModel<CustomCameraState, CustomCameraIntent, CustomCameraEffect>(initialState = CustomCameraState()) {
        override fun processIntent(intent: CustomCameraIntent) {
            when (intent) {
                is CustomCameraIntent.OnPermissionResult -> handleOnPermissionResult(intent)
                is CustomCameraIntent.OnRequestPermission -> handleOnRequestPermission()
                is CustomCameraIntent.OnZoomRangeReady -> handleOnZoomRangeReady(intent)
                is CustomCameraIntent.OnClickZoomLevel -> handleOnClickZoomLevel(intent)
                is CustomCameraIntent.OnClickFlip -> handleOnClickFlip()
                is CustomCameraIntent.OnClickShutter -> handleOnClickShutter()
                is CustomCameraIntent.OnCaptureSaved -> handleOnCaptureSaved(intent)
                is CustomCameraIntent.OnCaptureFailed -> handleOnCaptureFailed()
                is CustomCameraIntent.OnCancel -> handleOnCancel()
            }
        }

        private fun handleOnPermissionResult(intent: CustomCameraIntent.OnPermissionResult) {
            updateState { copy(hasPermission = intent.granted) }
        }

        private fun handleOnRequestPermission() {
            postSideEffect(CustomCameraEffect.RequestPermission)
        }

        private fun handleOnZoomRangeReady(intent: CustomCameraIntent.OnZoomRangeReady) {
            updateState {
                copy(
                    zoomRange = intent.range,
                    zoomRatio = zoomRatio.coerceIn(intent.range),
                )
            }
        }

        private fun handleOnClickZoomLevel(intent: CustomCameraIntent.OnClickZoomLevel) {
            updateState { copy(zoomRatio = intent.level.coerceIn(zoomRange)) }
        }

        private fun handleOnClickFlip() {
            updateState {
                copy(
                    lensFacing = when (lensFacing) {
                        CameraSelector.LENS_FACING_BACK -> CameraSelector.LENS_FACING_FRONT
                        else -> CameraSelector.LENS_FACING_BACK
                    },
                )
            }
        }

        private fun handleOnClickShutter() {
            postSideEffect(CustomCameraEffect.CaptureImage)
        }

        private fun handleOnCaptureSaved(intent: CustomCameraIntent.OnCaptureSaved) {
            postSideEffect(CustomCameraEffect.ReturnResult(intent.uri))
        }

        private fun handleOnCaptureFailed() {
            postSideEffect(CustomCameraEffect.NavigateToBack)
        }

        private fun handleOnCancel() {
            postSideEffect(CustomCameraEffect.NavigateToBack)
        }
    }
