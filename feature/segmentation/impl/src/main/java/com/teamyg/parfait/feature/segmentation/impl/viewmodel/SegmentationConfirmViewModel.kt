package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first

/**
 * @param subjectImagePath 초안이 아직 흐르기 전 첫 프레임에만 쓰는 초기값이다. 정본은 초안이다
 */
data class SegmentationConfirmState(
    val subjectImagePath: String,
    val cutoutImagePath: String?,
    val sourceImageUri: String?,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val isDraftReady: Boolean = false,
) : UiState {
    /** 원본과 재편집 마스크가 둘 다 있어야 편집 화면이 지운 영역을 되살릴 수 있다 */
    val isEditPhotoEnabled: Boolean
        get() = sourceImageUri != null && cutoutImagePath != null
}

sealed interface SegmentationConfirmIntent : UiIntent {
    data class OnEditResult(val result: ToppingEditResult) : SegmentationConfirmIntent
}

sealed interface SegmentationConfirmEffect : UiSideEffect {
    data object DraftMissing : SegmentationConfirmEffect

    data object DraftWriteFailed : SegmentationConfirmEffect
}

@HiltViewModel(assistedFactory = SegmentationConfirmViewModel.Factory::class)
class SegmentationConfirmViewModel
@AssistedInject constructor(
    @Assisted("subjectImagePath") subjectImagePath: String,
    @Assisted("cutoutImagePath") private val cutoutImagePath: String?,
    @Assisted("sourceImageUri") sourceImageUri: String?,
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<SegmentationConfirmState, SegmentationConfirmIntent, SegmentationConfirmEffect>(
    initialState = SegmentationConfirmState(
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        sourceImageUri = sourceImageUri,
    ),
) {
    // 흐름이 여러 번 방출돼도 같은 말을 되풀이하지 않는다
    private var hasReportedMissingDraft = false

    init {
        launch(onError = { reportMissingDraft() }) {
            // 최근 목록에서 되살린 알맹이는 세그멘테이션을 타지 않아 초안을 적어 준 데가 없다.
            // 구독보다 먼저 적어야 첫 방출의 null 이 없는 실패를 알리지 않는다.
            // 이미 적혀 있으면 건드리지 않는다 — record 는 테두리까지 통째로 덮어쓰므로,
            // 프로세스 사망 복원으로 이 화면이 다시 만들어질 때 사용자가 두른 테두리가 사라진다
            val isReuseEntry = cutoutImagePath == null
            val isDraftEmpty = toppingDraftRepository.draft.first()?.subjectImagePath == null

            if (isReuseEntry && isDraftEmpty) {
                val recorded = toppingDraftRepository.record(
                    subjectImagePath = subjectImagePath,
                    cutoutImagePath = null,
                    borderColorArgb = null,
                    borderWidthDp = null,
                )
                if (!recorded) {
                    reportMissingDraft()
                    return@launch
                }
            }

            collectDraft()
        }
    }

    override fun processIntent(intent: SegmentationConfirmIntent) {
        when (intent) {
            is SegmentationConfirmIntent.OnEditResult -> record(intent.result)
        }
    }

    private suspend fun collectDraft() {
        toppingDraftRepository.draft.collect { draft ->
            val subjectImagePath = draft?.subjectImagePath
            if (subjectImagePath == null) {
                reportMissingDraft()
                // 경로 값은 그대로 둔다 — 화면이 떠 있는 동안 다시 비면 그림이 깜빡이지 않게.
                // 다음 버튼만 잠근다
                updateState { copy(isDraftReady = false) }
                return@collect
            }

            // 초안이 다시 채워졌으니 이번에 또 비면 한 번 더 알려야 한다
            hasReportedMissingDraft = false

            val border = draft.borderColorArgb?.let { argb ->
                ToppingBorderLayer(colorArgb = argb, widthDp = draft.borderWidthDp ?: 0f)
            }
            updateState {
                copy(
                    subjectImagePath = subjectImagePath,
                    cutoutImagePath = draft.cutoutImagePath ?: cutoutImagePath,
                    borderColorArgb = draft.borderColorArgb,
                    borderWidthDp = draft.borderWidthDp,
                    // 겹칠 수 없어 언제나 0개 아니면 1개다(`adr/0025-topping-border-as-server-field.md`)
                    borderLayers = listOfNotNull(border),
                    isDraftReady = true,
                )
            }
        }
    }

    private fun reportMissingDraft() {
        if (hasReportedMissingDraft) return

        hasReportedMissingDraft = true
        postSideEffect(SegmentationConfirmEffect.DraftMissing)
    }

    private fun record(result: ToppingEditResult) {
        launch(onError = { postSideEffect(SegmentationConfirmEffect.DraftWriteFailed) }) {
            val border = result.borderLayers.lastOrNull()
            val recorded = toppingDraftRepository.record(
                subjectImagePath = result.subjectImagePath,
                cutoutImagePath = result.cutoutImagePath,
                borderColorArgb = border?.colorArgb,
                borderWidthDp = border?.widthDp,
            )
            if (!recorded) postSideEffect(SegmentationConfirmEffect.DraftWriteFailed)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("subjectImagePath") subjectImagePath: String,
            @Assisted("cutoutImagePath") cutoutImagePath: String?,
            @Assisted("sourceImageUri") sourceImageUri: String?,
        ): SegmentationConfirmViewModel
    }
}
