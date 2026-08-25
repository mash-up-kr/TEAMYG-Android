package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
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
import kotlinx.coroutines.launch

data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    /** 잘라낼 대상을 못 얻은 상태. 화면 전체가 `C-103-Error` 로 바뀐다 */
    val isError: Boolean = false,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /**
     * 고른 뒤의 실패에만 쓴다. 후보 목록이 그대로 남아 다른 대상을 고를 수 있으므로 화면을 덮지 않고
     * 토스트로 한 번 알린다. 대상을 아예 못 얻은 실패는 [SegmentationState.isError] 가 받는다.
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
        viewModelScope.launch {
            // 이번 흐름이 파일을 만들기 전에 지운다 — 뒤에 두면 방금 만든 것을 지운다
            // 지난 흐름의 파일을 못 지워도 이번 흐름은 진행돼야 한다 — 남은 파일은 다음 진입에서 다시 지운다
            runSuspendCatching { clearSegmentationCacheUseCase() }

            val bitmapWrapper = decodeImageUseCase(sourceImageUri).getOrNull()

            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false, isError = true) }
                return@launch
            }

            // 디코드를 통과한 뒤에 기록한다 — 열리지 않는 이미지를 남기면 최근 목록의 자리만 차지한다
            runSuspendCatching { addRecentImageUseCase(source = sourceImageUri, kind = RecentImageKind.SOURCE) }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        updateState { copy(isError = true) }
                        return@onSuccess
                    }

                    updateState { copy(candidates = candidates) }
                }.onFailure { updateState { copy(isError = true) } }

            // 실패해도 로딩 오버레이에 갇히지 않도록 성공/실패와 무관하게 해제한다
            updateState { copy(isLoading = false) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)
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
