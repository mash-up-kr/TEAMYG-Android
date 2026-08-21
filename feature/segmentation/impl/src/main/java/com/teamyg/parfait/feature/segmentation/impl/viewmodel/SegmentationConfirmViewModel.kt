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

/**
 * @param subjectImagePath 초안이 아직 흐르기 전 첫 프레임에만 쓰는 초기값이다. 정본은 초안이다
 */
data class SegmentationConfirmState(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val isDraftReady: Boolean = false,
) : UiState

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
    @Assisted("cutoutImagePath") cutoutImagePath: String,
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<SegmentationConfirmState, SegmentationConfirmIntent, SegmentationConfirmEffect>(
    initialState = SegmentationConfirmState(
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
    ),
) {
    // 초안이 비어 있다는 말은 한 번만 한다. 흐름이 여러 번 방출돼도 토스트가 쌓이면 안 된다
    private var hasReportedMissingDraft = false

    init {
        observeDraft()
    }

    override fun processIntent(intent: SegmentationConfirmIntent) {
        when (intent) {
            is SegmentationConfirmIntent.OnEditResult -> record(intent.result)
        }
    }

    private fun observeDraft() {
        launch {
            toppingDraftRepository.draft.collect { draft ->
                val subjectImagePath = draft?.subjectImagePath
                if (subjectImagePath == null) {
                    reportMissingDraft()
                    return@collect
                }

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
            @Assisted("cutoutImagePath") cutoutImagePath: String,
        ): SegmentationConfirmViewModel
    }
}
