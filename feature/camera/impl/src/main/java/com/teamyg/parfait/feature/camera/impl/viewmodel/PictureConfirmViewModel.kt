package com.teamyg.parfait.feature.camera.impl.viewmodel

import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.usecase.image.PrepareSegmentationModuleUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class PictureConfirmState(
    val showQuitDialog: Boolean = false,
) : UiState

sealed interface PictureConfirmIntent : UiIntent {
    data object OnClickRetake : PictureConfirmIntent

    data object OnClickConfirm : PictureConfirmIntent

    data object OnClickClose : PictureConfirmIntent

    data object OnDismissQuit : PictureConfirmIntent

    data object OnConfirmQuit : PictureConfirmIntent
}

sealed interface PictureConfirmEffect : UiSideEffect {
    data object NavigateBack : PictureConfirmEffect

    data class ReturnResult(val uri: String, val source: PictureConfirmSource) : PictureConfirmEffect

    data class NavigateToSegmentation(val uri: String) : PictureConfirmEffect

    /** 닫기 팝업은 경로와 무관하게 캔버스로 간다 — 문구만 `returnResultOnly` 로 갈린다 */
    data object QuitToCanvas : PictureConfirmEffect
}

/** 모듈 준비가 여기인 이유: `parfait/specs/2026-09-02-segmentation-module-install.md` */
@HiltViewModel(assistedFactory = PictureConfirmViewModel.Factory::class)
class PictureConfirmViewModel
@AssistedInject constructor(
    @Assisted private val uri: String,
    @Assisted private val source: PictureConfirmSource,
    @Assisted private val returnResultOnly: Boolean,
    private val prepareSegmentationModuleUseCase: PrepareSegmentationModuleUseCase,
    private val analyticsLogger: AnalyticsLogger,
) : BaseViewModel<PictureConfirmState, PictureConfirmIntent, PictureConfirmEffect>(
    initialState = PictureConfirmState(),
) {
    init {
        // 배경 편집에서 온 경로는 세그멘테이션으로 가지 않으므로 헛일을 안 한다
        if (!returnResultOnly) prepareSegmentationModule()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            uri: String,
            source: PictureConfirmSource,
            returnResultOnly: Boolean,
        ): PictureConfirmViewModel
    }

    override fun processIntent(intent: PictureConfirmIntent) {
        when (intent) {
            PictureConfirmIntent.OnClickRetake -> retake()

            PictureConfirmIntent.OnClickConfirm -> confirm()

            PictureConfirmIntent.OnClickClose -> updateState { copy(showQuitDialog = true) }

            PictureConfirmIntent.OnDismissQuit -> updateState { copy(showQuitDialog = false) }

            PictureConfirmIntent.OnConfirmQuit -> {
                updateState { copy(showQuitDialog = false) }
                postSideEffect(PictureConfirmEffect.QuitToCanvas)
            }
        }
    }

    /** 결과를 안 본다 — 이 화면은 준비를 걸기만 하고 사용자를 붙잡지 않는다 */
    private fun prepareSegmentationModule() {
        launch {
            runSuspendCatching { prepareSegmentationModuleUseCase() }
                .onFailure { viewModelLogger.w(it) { "세그멘테이션 모듈 사전 준비가 실패했다" } }
        }
    }

    /** 갤러리 소스의 "다시 선택"은 C-101 범위 밖이라 찍지 않는다 */
    private fun retake() {
        if (source == PictureConfirmSource.CAMERA) {
            analyticsLogger.logEvent(
                AnalyticsEvent(type = AnalyticsEventType.CANVAS_CAMERA_RETAKE_TAPPED),
            )
        }
        postSideEffect(PictureConfirmEffect.NavigateBack)
    }

    private fun confirm() {
        postSideEffect(
            if (returnResultOnly) {
                PictureConfirmEffect.ReturnResult(uri = uri, source = source)
            } else {
                PictureConfirmEffect.NavigateToSegmentation(uri = uri)
            },
        )
    }
}
