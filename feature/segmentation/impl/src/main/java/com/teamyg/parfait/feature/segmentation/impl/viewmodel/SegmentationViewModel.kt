package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.PersistSubjectUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/** 실패 화면이 무엇을 말할지 가른다 */
enum class SegmentationErrorKind {
    /** 후보도 폴백도 못 얻었다 */
    SubjectNotFound,

    /** 세그멘테이션 모델을 못 받았다 */
    ModuleNotReady,
}

data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    /** 널이 아니면 화면 전체가 `C-103-Error` 로 바뀐다 */
    val errorKind: SegmentationErrorKind? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent

    data object Retry : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /**
     * 고른 뒤의 실패에만 쓴다. 후보 목록이 그대로 남아 다른 대상을 고를 수 있으므로 화면을 덮지 않고
     * 토스트로 한 번 알린다. 대상을 아예 못 얻은 실패는 [SegmentationState.errorKind] 가 받는다.
     */
    data object ShowError : SegmentationEffect

    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
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
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    init {
        loadCandidates()
    }

    /**
     * ⚠️ 진입과 재시도가 **같은 키**를 쓴다. 진입만 다른 경로로 띄우면 진입 흐름이 도는 중에
     * 누른 재시도를 막지 못한다.
     */
    private fun loadCandidates() {
        launch(key = LOAD_CANDIDATES_KEY) {
            // 실패 표시를 걷지 않으면 재시도가 성공해도 에러 화면이 그대로 남는다
            updateState { copy(isLoading = true, errorKind = null, candidates = emptyList()) }

            // 이번 흐름이 파일을 만들기 전에 지운다 — 뒤에 두면 방금 만든 것을 지운다
            // 지난 흐름의 파일을 못 지워도 이번 흐름은 진행돼야 한다 — 남은 파일은 다음 진입에서 다시 지운다
            runSuspendCatching { clearSegmentationCacheUseCase() }

            val bitmapWrapper = decodeImageUseCase(sourceImageUri).getOrNull()

            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false, errorKind = SegmentationErrorKind.SubjectNotFound) }
                return@launch
            }

            // 디코드를 통과한 뒤에 기록한다 — 열리지 않는 이미지를 남기면 최근 목록의 자리만 차지한다
            runSuspendCatching { addRecentImageUseCase(source = sourceImageUri, kind = RecentImageKind.SOURCE) }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        updateState { copy(errorKind = SegmentationErrorKind.SubjectNotFound) }
                        return@onSuccess
                    }

                    updateState { copy(candidates = candidates) }
                }.onFailure { throwable ->
                    // 원인을 삼키면 모듈 미설치와 처리 실패가 화면에서 똑같아 보인다
                    viewModelLogger.e(throwable) {
                        "세그멘테이션 실패 ${throwable::class.simpleName}, 원인 ${throwable.cause}"
                    }
                    updateState { copy(errorKind = throwable.toErrorKind()) }
                }

            // 실패해도 로딩 오버레이에 갇히지 않도록 성공/실패와 무관하게 해제한다
            updateState { copy(isLoading = false) }
        }
    }

    private fun Throwable.toErrorKind(): SegmentationErrorKind = when (this) {
        is SegmentationException.ModuleNotReady -> SegmentationErrorKind.ModuleNotReady
        else -> SegmentationErrorKind.SubjectNotFound
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)
            SegmentationIntent.Retry -> loadCandidates()
        }
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
            updateState { copy(isLoading = true) }

            persistSubjectUseCase(candidate)
                .onSuccess { result ->
                    val recorded = runSuspendCatching {
                        toppingDraftRepository.record(
                            subjectImagePath = result.trimmedSubjectImagePath,
                            cutoutImagePath = result.subjectImagePath,
                            borderColorArgb = null,
                            borderWidthDp = null,
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
        updateState { copy(isLoading = false) }
    }
}

private const val SELECT_CANDIDATE_KEY = "select-candidate"
private const val LOAD_CANDIDATES_KEY = "loadCandidates"
