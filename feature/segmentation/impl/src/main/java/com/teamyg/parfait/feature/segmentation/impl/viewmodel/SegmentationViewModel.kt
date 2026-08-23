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
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /** 재시도 동선이 없는 실패라 상태로 남기지 않는다 — 토스트로 한 번 알리고 끝이다. */
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
                postSideEffect(SegmentationEffect.ShowError)
                updateState { copy(isLoading = false) }
                return@launch
            }

            // 디코드를 통과한 뒤에 기록한다 — 열리지 않는 이미지를 남기면 최근 목록의 자리만 차지한다
            runSuspendCatching { addRecentImageUseCase(source = sourceImageUri, kind = RecentImageKind.SOURCE) }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        postSideEffect(SegmentationEffect.ShowError)
                        return@onSuccess
                    }

                    updateState { copy(candidates = candidates) }
                }.onFailure { postSideEffect(SegmentationEffect.ShowError) }

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
     * 저장 → 초안 기록 → 이동 순서를 지킨다. 확인 화면은 정상 진입에서 초안을 구독만 하므로,
     * 기록을 마치기 전에 보내면 그 화면이 "다음"을 잠근 채 뜬다.
     */
    private fun selectCandidate(index: Int) {
        val candidate = state.value.candidates.getOrNull(index) ?: return

        launch(key = SELECT_CANDIDATE_KEY) {
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
                    updateState { copy(isLoading = false) }

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
                    updateState { copy(isLoading = false) }
                    postSideEffect(SegmentationEffect.ShowError)
                }
        }
    }
}

private const val SELECT_CANDIDATE_KEY = "select-candidate"
