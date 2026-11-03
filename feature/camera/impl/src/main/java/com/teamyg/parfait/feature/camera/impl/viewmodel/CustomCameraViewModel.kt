package com.teamyg.parfait.feature.camera.impl.viewmodel

import androidx.camera.core.CameraSelector
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheFileUseCase
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheUriUseCase
import com.teamyg.parfait.feature.camera.impl.model.CameraZoomLevel
import com.teamyg.parfait.feature.camera.impl.model.HalfZoomSource
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
     * 촬영을 접고 캔버스로 돌아간다.
     *
     * 결과를 실어 보내지 않는다 — 취소는 값이 없는 사건이고, 예전처럼 `null` 을 결과 버스에
     * 흘리면 그것을 결과로 아는 화면이 받아 터진다.
     */
    data object Cancel : CustomCameraEffect

    /** 촬영이 실패했다. 알리고 그 자리에 머문다 — 되돌아가면 사용자는 왜 아무 일도 없었는지 모른다 */
    data object CaptureFailed : CustomCameraEffect

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

    data object OnOpenAppSettings : CustomCameraIntent

    /** 메인 카메라에 바인딩됐을 때만 온다. 초광각에 붙었을 때 오면 1x로 초기화돼 0.5x가 풀린다 */
    data class OnZoomRangeReady(
        val range: ClosedFloatingPointRange<Float>,
        val hasUltraWideCamera: Boolean = false,
    ) : CustomCameraIntent

    data object OnClickZoom : CustomCameraIntent

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
    val zoomLevel: CameraZoomLevel = CameraZoomLevel.ONE,
    val zoomLevels: List<CameraZoomLevel> = listOf(CameraZoomLevel.ONE),
    val halfZoomSource: HalfZoomSource = HalfZoomSource.NONE,
    val flashMode: FlashMode = FlashMode.OFF,
) : UiState {
    val isZoomButtonVisible: Boolean
        get() = zoomLevels.size > 1

    val useUltraWideCamera: Boolean
        get() = zoomLevel == CameraZoomLevel.HALF && halfZoomSource == HalfZoomSource.ULTRA_WIDE

    /** 바인딩된 카메라에 걸 배율. 초광각에 붙었으면 그 카메라의 1x가 곧 0.5x다 */
    val cameraZoomRatio: Float
        get() = if (useUltraWideCamera) 1f else zoomLevel.ratio
}

@HiltViewModel
class CustomCameraViewModel
@Inject
constructor(
    private val createCameraCacheFileUseCase: CreateCameraCacheFileUseCase,
    private val createCameraCacheUriUseCase: CreateCameraCacheUriUseCase,
) : BaseViewModel<CustomCameraState, CustomCameraIntent, CustomCameraEffect>(
    initialState = CustomCameraState(),
) {
    private var hasRequestedPermission = false

    init {
        viewModelLogger.i { "CustomCameraViewModel::init" }
    }

    override fun processIntent(intent: CustomCameraIntent) {
        when (intent) {
            is CustomCameraIntent.OnPermissionResult -> handleOnPermissionResult(intent)
            is CustomCameraIntent.OnPermissionRequestResult -> handleOnPermissionRequestResult(intent)
            is CustomCameraIntent.OnOpenAppSettings -> handleOnOpenAppSettings()
            is CustomCameraIntent.OnZoomRangeReady -> handleOnZoomRangeReady(intent)
            is CustomCameraIntent.OnClickZoom -> handleOnClickZoom()
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

        if (!intent.granted) {
            requestPermissionOnce()
        }
    }

    private fun requestPermissionOnce() {
        if (hasRequestedPermission) return

        hasRequestedPermission = true
        postSideEffect(CustomCameraEffect.RequestPermission)
    }

    private fun handleOnPermissionRequestResult(intent: CustomCameraIntent.OnPermissionRequestResult) {
        updateState {
            copy(
                hasPermission = intent.granted,
                permanentlyDenied = !intent.granted && !intent.shouldShowRationale,
            )
        }
    }

    private fun handleOnOpenAppSettings() {
        postSideEffect(CustomCameraEffect.OpenAppSettings)
    }

    private fun handleOnZoomRangeReady(intent: CustomCameraIntent.OnZoomRangeReady) {
        val halfZoomSource = HalfZoomSource.of(intent.range, intent.hasUltraWideCamera)

        updateState {
            copy(
                halfZoomSource = halfZoomSource,
                zoomLevels = CameraZoomLevel.available(intent.range, halfZoomSource),
                zoomLevel = CameraZoomLevel.ONE,
            )
        }
    }

    private fun handleOnClickZoom() {
        updateState { copy(zoomLevel = zoomLevel.next(zoomLevels)) }
    }

    private fun handleOnClickFlip() {
        updateState {
            copy(
                zoomLevel = CameraZoomLevel.ONE,
                // 새 렌즈 범위가 올 때까지 버튼을 숨긴다. 두면 이전 렌즈 단계로 순환한다
                zoomLevels = listOf(CameraZoomLevel.ONE),
                halfZoomSource = HalfZoomSource.NONE,
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
        postSideEffect(CustomCameraEffect.CaptureFailed)
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
