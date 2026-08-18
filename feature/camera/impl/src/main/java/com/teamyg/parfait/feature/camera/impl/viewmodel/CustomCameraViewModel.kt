package com.teamyg.parfait.feature.camera.impl.viewmodel

import androidx.camera.core.CameraSelector
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheFileUseCase
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheUriUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

sealed interface CustomCameraEffect : UiSideEffect {
    data object RequestPermission : CustomCameraEffect

    data object OpenAppSettings : CustomCameraEffect

    data class CaptureImage(
        val file: File,
    ) : CustomCameraEffect

    /**
     * 촬영을 접고 부른 쪽으로 돌아간다.
     *
     * 결과를 실어 보내지 않는다 — 취소는 값이 없는 사건이고, 예전처럼 `null` 을 결과 버스에
     * 흘리면 그것을 결과로 아는 화면이 받아 터진다.
     */
    data object Cancel : CustomCameraEffect

    data class NavigateToConfirm(
        val uri: String,
    ) : CustomCameraEffect
}

sealed interface CustomCameraIntent : UiIntent {
    data class OnPermissionResult(
        val granted: Boolean,
    ) : CustomCameraIntent

    data class OnPermissionRequestResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean,
    ) : CustomCameraIntent

    data object OnRequestPermission : CustomCameraIntent

    data object OnOpenAppSettings : CustomCameraIntent

    data class OnZoomRangeReady(
        val range: ClosedFloatingPointRange<Float>,
    ) : CustomCameraIntent

    data class OnClickZoomLevel(
        val level: Float,
    ) : CustomCameraIntent

    data object OnClickFlip : CustomCameraIntent

    data object OnClickShutter : CustomCameraIntent

    data object OnClickFlash : CustomCameraIntent

    data class OnCaptureSaved(
        val file: File,
    ) : CustomCameraIntent

    data object OnCaptureFailed : CustomCameraIntent

    data object OnCancel : CustomCameraIntent
}

data class CustomCameraState(
    val isInit: Boolean = false,
    val hasPermission: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val zoomRatio: Float = 1f,
    val zoomRange: ClosedFloatingPointRange<Float> = 1f..1f,
    val flashMode: FlashMode = FlashMode.OFF,
) : UiState

@HiltViewModel
class CustomCameraViewModel
@Inject
constructor(
    private val createCameraCacheFileUseCase: CreateCameraCacheFileUseCase,
    private val createCameraCacheUriUseCase: CreateCameraCacheUriUseCase,
) : BaseViewModel<CustomCameraState, CustomCameraIntent, CustomCameraEffect>(
    initialState = CustomCameraState(),
) {
    init {
        viewModelLogger.i { "CustomCameraViewModel::init" }
    }

    override fun processIntent(intent: CustomCameraIntent) {
        when (intent) {
            is CustomCameraIntent.OnPermissionResult -> handleOnPermissionResult(intent)
            is CustomCameraIntent.OnPermissionRequestResult -> handleOnPermissionRequestResult(intent)
            is CustomCameraIntent.OnRequestPermission -> handleOnRequestPermission()
            is CustomCameraIntent.OnOpenAppSettings -> handleOnOpenAppSettings()
            is CustomCameraIntent.OnZoomRangeReady -> handleOnZoomRangeReady(intent)
            is CustomCameraIntent.OnClickZoomLevel -> handleOnClickZoomLevel(intent)
            is CustomCameraIntent.OnClickFlip -> handleOnClickFlip()
            is CustomCameraIntent.OnClickShutter -> handleOnClickShutter()
            is CustomCameraIntent.OnCaptureSaved -> handleOnCaptureSaved(intent)
            is CustomCameraIntent.OnCaptureFailed -> handleOnCaptureFailed()
            is CustomCameraIntent.OnCancel -> handleOnCancel()
            is CustomCameraIntent.OnClickFlash -> handleOnClickFlash()
        }
    }

    private fun handleOnPermissionResult(intent: CustomCameraIntent.OnPermissionResult) {
        updateState {
            copy(
                isInit = true,
                hasPermission = intent.granted,
                permanentlyDenied = if (intent.granted) false else permanentlyDenied,
            )
        }
    }

    private fun handleOnPermissionRequestResult(intent: CustomCameraIntent.OnPermissionRequestResult) {
        updateState {
            copy(
                hasPermission = intent.granted,
                permanentlyDenied = !intent.granted && !intent.shouldShowRationale,
            )
        }
    }

    private fun handleOnRequestPermission() {
        postSideEffect(CustomCameraEffect.RequestPermission)
    }

    private fun handleOnOpenAppSettings() {
        postSideEffect(CustomCameraEffect.OpenAppSettings)
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

    private fun handleOnClickFlash() {
        updateState {
            copy(
                flashMode = !flashMode,
            )
        }
    }

    private fun handleOnClickShutter() {
        postSideEffect(CustomCameraEffect.CaptureImage(file = createCameraCacheFileUseCase()))
    }

    private fun handleOnCaptureSaved(intent: CustomCameraIntent.OnCaptureSaved) {
        val uri = createCameraCacheUriUseCase(file = intent.file)
        postSideEffect(CustomCameraEffect.NavigateToConfirm(uri = uri))
    }

    private fun handleOnCaptureFailed() {
        postSideEffect(CustomCameraEffect.Cancel)
    }

    private fun handleOnCancel() {
        postSideEffect(CustomCameraEffect.Cancel)
    }
}

enum class FlashMode {
    OFF,
    ON,
    ;

    operator fun not(): FlashMode = if (this == ON) OFF else ON
}
