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
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
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

    /** X 버튼, 분석 중 시스템 뒤로가기 */
    data object ClickClose : SegmentationIntent

    /** 팝업의 「그만두기」 */
    data object ConfirmQuit : SegmentationIntent

    /** 팝업의 「계속 편집」, 팝업 바깥 탭 */
    data object DismissQuit : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /** 후보 저장 실패를 토스트로만 알린다 */
    data object ShowError : SegmentationEffect

    data object QuitToCanvas : SegmentationEffect

    /**
     * 자동 인식으로 대상을 못 얻었다. 원본을 따로 저장하지 않는다 — 편집 화면이 같은 원본 URI 를
     * 직접 읽으므로 Route 가 `key.sourceImageUri` 로 편집을 연다.
     */
    data object GoToEdit : SegmentationEffect

    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
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
    private val recordToppingDraft: RecordToppingDraftUseCase,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    /** 팝업이 떠 있는 동안 나온 결과. 화면이 안 쓰는 값이라 상태로 올리지 않는다 */
    private var pendingOutcome: Outcome? = null

    /** 그만두기를 확정했다. 이후 도착하는 분석 결과는 버린다 */
    private var quit = false

    init {
        analyze()
    }

    /** 실패는 모두 편집으로 접는다. `onError` 가 없으면 던진 예외에 로딩 화면에 갇힌다 */
    private fun analyze() {
        launch(key = LOAD_CANDIDATES_KEY, onError = { deliver(Outcome.Edit) }) {
            // 이번 흐름이 파일을 만들기 전에 지운다 — 뒤에 두면 방금 만든 것을 지운다
            // 지난 흐름의 파일을 못 지워도 이번 흐름은 진행돼야 한다 — 남은 파일은 다음 진입에서 다시 지운다
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
        if (quit) return

        if (state.value.showQuitDialog) {
            pendingOutcome = outcome
            return
        }

        when (outcome) {
            // 편집으로 갈 때 분석 상태를 먼저 끄면 교체 직전 한 프레임 동안 후보 0개인 선택 UI 가 보인다
            Outcome.Edit -> postSideEffect(SegmentationEffect.GoToEdit)

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
                // 그만두기를 두 번 눌러도 캔버스로 나가는 effect 는 한 번이다
                if (quit) return

                quit = true
                pendingOutcome = null
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

    /**
     * 저장 → 초안 기록 → 이동 순서를 지킨다. 그 순서인 이유는
     * `parfait/specs/2026-08-23-c103-multi-subject-selection.md`의 「선택 시점에 일어나는
     * 일의 순서」절.
     */
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
                    val recorded = runSuspendCatching {
                        recordToppingDraft(
                            subjectImagePath = result.trimmedSubjectImagePath,
                            cutoutImagePath = result.subjectImagePath,
                            borderColorArgb = null,
                            borderWidthDp = null,
                            sourceLongSide = result.sourceLongSide,
                        )
                    }.getOrDefault(false)

                    // 이동이 goTo 라 이 화면이 백스택에 남는다. 켠 채 나가면 돌아왔을 때 갇힌다
                    releaseLoading()

                    if (recorded) {
                        postSideEffect(
                            SegmentationEffect.GoToConfirm(
                                subjectImagePath = result.subjectImagePath,
                                trimmedSubjectImagePath = result.trimmedSubjectImagePath,
                            ),
                        )
                    } else {
                        postSideEffect(SegmentationEffect.ShowError)
                    }
                }.onFailure {
                    releaseLoading()
                    postSideEffect(SegmentationEffect.ShowError)
                }
        }
    }

    private fun releaseLoading() {
        updateState { copy(isSaving = false) }
    }
}

private const val SELECT_CANDIDATE_KEY = "select-candidate"
private const val LOAD_CANDIDATES_KEY = "loadCandidates"
