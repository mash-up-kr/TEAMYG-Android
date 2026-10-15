package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.PersistSubjectUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class SegmentationState(
    /** 참이면 화면 전체가 `C-101-Loading` 이다 */
    val isAnalyzing: Boolean = true,
    /** 후보를 고른 뒤 저장하는 동안 화면을 덮는 오버레이 */
    val isSaving: Boolean = false,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    val showQuitDialog: Boolean = false,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent

    data object ClickClose : SegmentationIntent

    data object ConfirmQuit : SegmentationIntent

    data object DismissQuit : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /** 후보 저장 실패를 토스트로만 알린다 */
    data object ShowError : SegmentationEffect

    data object QuitToCanvas : SegmentationEffect

    /** 고른 후보로 편집에 간다. [segmentationImagePath] 는 원본 크기 저장본이다 */
    data class GoToEditCandidate(val segmentationImagePath: String) : SegmentationEffect

    /** 대상을 못 얻어 편집에 간다. 편집이 원본을 마스크로도 읽는다 */
    data object GoToEditDetectionFailed : SegmentationEffect
}

/** 분석이 낸 결과. 팝업이 떠 있으면 [SegmentationViewModel.deliver] 가 보류한다 */
private sealed interface Outcome {
    data class Select(val candidates: List<SegmentationCandidate>) : Outcome

    data object Edit : Outcome
}

@HiltViewModel(assistedFactory = SegmentationViewModel.Factory::class)
class SegmentationViewModel
@AssistedInject constructor(
    @Assisted private val sourceImageUri: String,
    private val addRecentImageUseCase: AddRecentImageUseCase,
    private val clearSegmentationCacheUseCase: ClearSegmentationCacheUseCase,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val segmentImageUseCase: SegmentImageUseCase,
    private val persistSubjectUseCase: PersistSubjectUseCase,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    /** 팝업이 떠 있는 동안 나온 결과. 화면이 안 쓰는 값이라 상태로 올리지 않는다 */
    private var pendingOutcome: Outcome? = null

    /** 그만두기를 확정했다. 이후 도착하는 분석 결과는 버린다 */
    private var isQuitConfirmed = false

    init {
        analyze()
    }

    /** 실패는 모두 편집으로 접는다. `onError` 가 없으면 던진 예외에 로딩 화면에 갇힌다 */
    private fun analyze() {
        launch(key = LOAD_CANDIDATES_KEY, onError = { deliver(Outcome.Edit) }) {
            // 이번 흐름이 파일을 만들기 전에 지운다(뒤에 두면 방금 만든 것을 지운다). 실패해도 진행한다
            runSuspendCatching { clearSegmentationCacheUseCase() }

            val bitmapWrapper = decodeImageUseCase(sourceImageUri).getOrNull()

            if (bitmapWrapper == null) {
                deliver(Outcome.Edit)
                return@launch
            }

            // 디코드를 통과한 뒤에 기록한다 — 열리지 않는 이미지를 남기면 최근 목록의 자리만 차지한다
            runSuspendCatching { addRecentImageUseCase(source = sourceImageUri, kind = RecentImageKind.SOURCE) }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    deliver(if (candidates.isEmpty()) Outcome.Edit else Outcome.Select(candidates))
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) {
                        "세그멘테이션 실패 ${throwable::class.simpleName}, 원인 ${throwable.cause}"
                    }
                    deliver(Outcome.Edit)
                }
        }
    }

    /**
     * 그만두기를 확정했으면 버리고, 팝업이 떠 있으면 [DismissQuit][SegmentationIntent.DismissQuit]
     * 까지 보류한다. 고르는 중에 화면이 바뀌면 사용자의 선택이 무엇에 대한 것인지 모호해진다.
     */
    private fun deliver(outcome: Outcome) {
        if (isQuitConfirmed) return

        if (state.value.showQuitDialog) {
            pendingOutcome = outcome
            return
        }

        when (outcome) {
            // 편집으로 갈 때 분석 상태를 먼저 끄면 교체 직전 한 프레임 동안 후보 0개인 선택 UI 가 보인다
            Outcome.Edit -> postSideEffect(SegmentationEffect.GoToEditDetectionFailed)

            is Outcome.Select ->
                updateState { copy(candidates = outcome.candidates, isAnalyzing = false) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)

            SegmentationIntent.ClickClose -> updateState { copy(showQuitDialog = true) }

            SegmentationIntent.DismissQuit -> dismissQuit()

            SegmentationIntent.ConfirmQuit -> {
                if (isQuitConfirmed) return

                isQuitConfirmed = true
                pendingOutcome = null
                // 화면이 걷히는 전환 동안 팝업이 캔버스 위에 남지 않게 닫는다
                updateState { copy(showQuitDialog = false) }
                postSideEffect(SegmentationEffect.QuitToCanvas)
            }
        }
    }

    private fun dismissQuit() {
        updateState { copy(showQuitDialog = false) }

        val outcome = pendingOutcome ?: return
        pendingOutcome = null
        deliver(outcome)
    }

    /** 초안은 여기서 적지 않는다 — 편집의 "다음"이 적는다 */
    private fun selectCandidate(index: Int) {
        val candidate = state.value.candidates.getOrNull(index) ?: return

        launch(
            key = SELECT_CANDIDATE_KEY,
            onError = {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
            },
        ) {
            updateState { copy(isSaving = true) }

            persistSubjectUseCase(candidate)
                .onSuccess { result ->
                    // 이동이 goTo 라 이 화면이 백스택에 남는다. 켠 채 나가면 돌아왔을 때 갇힌다
                    releaseLoading()
                    postSideEffect(SegmentationEffect.GoToEditCandidate(result.subjectImagePath))
                }.onFailure {
                    releaseLoading()
                    postSideEffect(SegmentationEffect.ShowError)
                }
        }
    }

    private fun releaseLoading() {
        updateState { copy(isSaving = false) }
    }

    private companion object {
        const val SELECT_CANDIDATE_KEY = "select-candidate"
        const val LOAD_CANDIDATES_KEY = "load-candidates"
    }
}
