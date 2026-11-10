package com.teamyg.parfait.feature.camera.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheFileUseCase
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheUriUseCase
import com.teamyg.parfait.feature.camera.impl.model.CameraZoomLevel
import com.teamyg.parfait.feature.camera.impl.model.FlashMode
import com.teamyg.parfait.feature.camera.impl.model.HalfZoomSource
import com.teamyg.parfait.feature.camera.impl.model.LensFacing
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed interface CustomCameraEffect : UiSideEffect {
    data object RequestPermission : CustomCameraEffect

    data object OpenAppSettings : CustomCameraEffect

    data class CaptureImage(
        val file: File,
    ) : CustomCameraEffect

    /**
     * 촬영을 접고 캔버스로 돌아간다.
     *
     * 결과를 실어 보내지 않는다 — 취소는 값이 없는 사건이고, `null` 을 결과 버스에 흘리면
     * 그것을 결과로 아는 화면이 받아 터진다.
     */
    data object Cancel : CustomCameraEffect

    /** 촬영이 실패했다. 알리고 그 자리에 머문다 — 되돌아가면 사용자는 왜 아무 일도 없었는지 모른다 */
    data object CaptureFailed : CustomCameraEffect

    data class NavigateToConfirm(
        val uri: String,
    ) : CustomCameraEffect
}

sealed interface CustomCameraIntent : UiIntent {
    data class OnPermissionChecked(
        val granted: Boolean,
    ) : CustomCameraIntent

    data class OnPermissionRequestResult(
        val granted: Boolean,
    ) : CustomCameraIntent

    data object OnClickOpenAppSettings : CustomCameraIntent

    /** 메인 카메라에 바인딩됐을 때만 온다(`CameraPreviewBindingEffect`) */
    data class OnZoomRangeReady(
        val range: ClosedFloatingPointRange<Float>,
        val hasUltraWideCamera: Boolean,
    ) : CustomCameraIntent

    data object OnClickZoom : CustomCameraIntent

    /** 직전 이벤트 대비 두 손가락 간격 비율. 1보다 크면 확대 */
    data class OnPinchZoom(
        val scale: Float,
    ) : CustomCameraIntent

    data object OnPinchZoomStart : CustomCameraIntent

    data object OnPinchZoomEnd : CustomCameraIntent

    data object OnClickFlip : CustomCameraIntent

    data object OnClickShutter : CustomCameraIntent

    data object OnClickFlash : CustomCameraIntent

    data class OnCaptureSaved(
        val file: File,
    ) : CustomCameraIntent

    data object OnCaptureFailed : CustomCameraIntent

    /** 촬영을 기다리던 화면이 사라졌다. 결과를 알릴 화면이 없으니 셔터 잠금만 푼다 */
    data object OnCaptureCancelled : CustomCameraIntent

    data object OnCancel : CustomCameraIntent
}

data class CustomCameraState(
    val isInitialized: Boolean = false,
    val hasPermission: Boolean = false,
    val lensFacing: LensFacing = LensFacing.BACK,
    /** 메인 카메라 기준 배율. 초광각에 붙었으면 0.5다 */
    val zoomRatio: Float = CameraZoomLevel.ONE.ratio,
    val zoomRange: ClosedFloatingPointRange<Float> = CameraZoomLevel.ONE.ratio..CameraZoomLevel.ONE.ratio,
    val zoomLevels: List<CameraZoomLevel> = listOf(CameraZoomLevel.ONE),
    val halfZoomSource: HalfZoomSource = HalfZoomSource.NONE,
    val isZoomByGesture: Boolean = false,
    val flashMode: FlashMode = FlashMode.OFF,
    val isCapturing: Boolean = false,
) : UiState {
    val zoomLevel: CameraZoomLevel?
        get() = CameraZoomLevel.matching(zoomRatio, zoomLevels)

    val zoomLabel: String
        get() = CameraZoomLevel.labelOf(zoomRatio)

    val isZoomButtonVisible: Boolean
        get() = zoomLevels.size > 1 || zoomRange.start < zoomRange.endInclusive

    val shouldUseUltraWideCamera: Boolean
        get() = zoomRatio == CameraZoomLevel.HALF.ratio && halfZoomSource == HalfZoomSource.ULTRA_WIDE

    /** 바인딩된 카메라에 걸 배율. 초광각에 붙었으면 그 카메라의 1x가 곧 0.5x다 */
    val cameraZoomRatio: Float
        get() = if (shouldUseUltraWideCamera) 1f else zoomRatio
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

    /** 렌즈 경계(초광각 0.5x·메인 최소 배율)를 넘어선 핀치 누적 */
    private var lensSwitchScale = 1f

    // 경계 떨림으로 재바인딩이 반복되지 않게 제스처당 한 번만 전환한다
    private var hasSwitchedLensInPinch = false

    private var lensSwitchJob: Job? = null

    init {
        viewModelLogger.i { "CustomCameraViewModel::init" }
    }

    override fun processIntent(intent: CustomCameraIntent) {
        when (intent) {
            is CustomCameraIntent.OnPermissionChecked -> handleOnPermissionChecked(intent)
            is CustomCameraIntent.OnPermissionRequestResult -> handleOnPermissionRequestResult(intent)
            is CustomCameraIntent.OnClickOpenAppSettings -> handleOnClickOpenAppSettings()
            is CustomCameraIntent.OnZoomRangeReady -> handleOnZoomRangeReady(intent)
            is CustomCameraIntent.OnClickZoom -> handleOnClickZoom()
            is CustomCameraIntent.OnPinchZoom -> handleOnPinchZoom(intent)
            is CustomCameraIntent.OnPinchZoomStart -> handleOnPinchZoomStart()
            is CustomCameraIntent.OnPinchZoomEnd -> handleOnPinchZoomEnd()
            is CustomCameraIntent.OnClickFlip -> handleOnClickFlip()
            is CustomCameraIntent.OnClickShutter -> handleOnClickShutter()
            is CustomCameraIntent.OnCaptureSaved -> handleOnCaptureSaved(intent)
            is CustomCameraIntent.OnCaptureFailed -> handleOnCaptureFailed()
            is CustomCameraIntent.OnCaptureCancelled -> handleOnCaptureCancelled()
            is CustomCameraIntent.OnCancel -> handleOnCancel()
            is CustomCameraIntent.OnClickFlash -> handleOnClickFlash()
        }
    }

    private fun handleOnPermissionChecked(intent: CustomCameraIntent.OnPermissionChecked) {
        updateState {
            copy(
                isInitialized = true,
                hasPermission = intent.granted,
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
            )
        }
    }

    private fun handleOnClickOpenAppSettings() {
        postSideEffect(CustomCameraEffect.OpenAppSettings)
    }

    private fun handleOnZoomRangeReady(intent: CustomCameraIntent.OnZoomRangeReady) {
        val newHalfZoomSource = HalfZoomSource.of(intent.range, intent.hasUltraWideCamera)
        val newZoomLevels = CameraZoomLevel.available(intent.range, newHalfZoomSource)
        val isUltraWideKept = state.value.shouldUseUltraWideCamera && newHalfZoomSource == HalfZoomSource.ULTRA_WIDE
        val pinchRange = intent.range.start..minOf(intent.range.endInclusive, MAX_PINCH_ZOOM_RATIO)

        updateState {
            copy(
                halfZoomSource = newHalfZoomSource,
                zoomLevels = newZoomLevels,
                zoomRange = pinchRange,
                // 다시 찍기 등으로 같은 카메라가 다시 바인딩될 때는 배율을 지킨다
                zoomRatio = zoomRatio.takeIf { isUltraWideKept || it in pinchRange } ?: CameraZoomLevel.ONE.ratio,
                isZoomByGesture = false,
            )
        }
    }

    private fun handleOnClickZoom() {
        updateState {
            val target = zoomLevel?.next(zoomLevels) ?: CameraZoomLevel.ONE
            copy(zoomRatio = target.ratio, isZoomByGesture = false)
        }
    }

    private fun handleOnPinchZoomStart() {
        cancelLensSwitch()
        hasSwitchedLensInPinch = false
    }

    private fun handleOnPinchZoomEnd() {
        cancelLensSwitch()
    }

    private fun handleOnPinchZoom(intent: CustomCameraIntent.OnPinchZoom) {
        // 전환 뒤 남은 손가락 이동이 새 카메라 배율로 이어지면 1x로 넘어가자마자 1.4x처럼 밀린다
        if (hasSwitchedLensInPinch) return

        val current = state.value
        val canSwitchLens = current.halfZoomSource == HalfZoomSource.ULTRA_WIDE

        // 초광각은 메인 카메라와 배율 축이 달라 그 안에서 줌하지 않는다
        if (current.shouldUseUltraWideCamera) {
            lensSwitchScale = (lensSwitchScale * intent.scale).coerceAtLeast(1f)
            if (lensSwitchScale >= LENS_SWITCH_ZOOM_IN_SCALE) {
                scheduleLensSwitch(CameraZoomLevel.ONE)
            } else {
                cancelLensSwitch()
            }
            return
        }

        val rawRatio = current.zoomRatio * intent.scale
        val minRatio = current.zoomRange.start
        lensSwitchScale = if (rawRatio < minRatio) lensSwitchScale * rawRatio / minRatio else 1f
        if (canSwitchLens && lensSwitchScale <= LENS_SWITCH_ZOOM_OUT_SCALE) {
            scheduleLensSwitch(CameraZoomLevel.HALF)
        } else {
            cancelLensSwitch()
        }

        updateState {
            copy(
                zoomRatio = rawRatio.coerceIn(zoomRange),
                isZoomByGesture = true,
            )
        }
    }

    // 빠른 핀치는 문턱만으로 쉽게 뚫린다
    private fun scheduleLensSwitch(target: CameraZoomLevel) {
        if (lensSwitchJob?.isActive == true) return

        lensSwitchJob = launch {
            delay(LENS_SWITCH_HOLD)
            lensSwitchScale = 1f
            hasSwitchedLensInPinch = true
            updateState { copy(zoomRatio = target.ratio, isZoomByGesture = false) }
        }
    }

    private fun cancelLensSwitch() {
        lensSwitchJob?.cancel()
        lensSwitchJob = null
    }

    private fun handleOnClickFlip() {
        cancelLensSwitch()
        updateState {
            copy(
                zoomRatio = CameraZoomLevel.ONE.ratio,
                zoomRange = CameraZoomLevel.ONE.ratio..CameraZoomLevel.ONE.ratio,
                isZoomByGesture = false,
                // 새 렌즈 범위가 올 때까지 버튼을 숨긴다. 두면 이전 렌즈 단계로 순환한다
                zoomLevels = listOf(CameraZoomLevel.ONE),
                halfZoomSource = HalfZoomSource.NONE,
                lensFacing = lensFacing.flipped(),
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
        if (state.value.isCapturing) return

        updateState { copy(isCapturing = true) }
        postSideEffect(CustomCameraEffect.CaptureImage(file = createCameraCacheFileUseCase()))
    }

    private fun handleOnCaptureSaved(intent: CustomCameraIntent.OnCaptureSaved) {
        updateState { copy(isCapturing = false) }
        val uri = createCameraCacheUriUseCase(file = intent.file)
        postSideEffect(CustomCameraEffect.NavigateToConfirm(uri = uri))
    }

    private fun handleOnCaptureFailed() {
        updateState { copy(isCapturing = false) }
        postSideEffect(CustomCameraEffect.CaptureFailed)
    }

    private fun handleOnCaptureCancelled() {
        updateState { copy(isCapturing = false) }
    }

    private fun handleOnCancel() {
        postSideEffect(CustomCameraEffect.Cancel)
    }
}

private val MAX_PINCH_ZOOM_RATIO = CameraZoomLevel.TWO.ratio
private const val LENS_SWITCH_ZOOM_IN_SCALE = 1.25f
private const val LENS_SWITCH_ZOOM_OUT_SCALE = 1f / LENS_SWITCH_ZOOM_IN_SCALE
private val LENS_SWITCH_HOLD = 250.milliseconds
