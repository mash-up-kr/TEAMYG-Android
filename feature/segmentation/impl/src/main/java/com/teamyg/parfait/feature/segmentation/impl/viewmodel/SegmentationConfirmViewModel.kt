package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import androidx.lifecycle.SavedStateHandle
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

private const val KEY_RECORDED_ENTRY_SUBJECT = "recorded_entry_subject"

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
    /** 되살릴 원본이 없으면 영역은 손댈 수 없고 테두리만 고칠 수 있다 */
    val isBorderOnlyEdit: Boolean
        get() = sourceImageUri == null

    /** 편집 화면이 시작 마스크로 읽을 그림. 재편집 마스크가 없는 재사용 진입은 알맹이가 곧 재료다 */
    val editImagePath: String
        get() = cutoutImagePath ?: subjectImagePath
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
    private val savedStateHandle: SavedStateHandle,
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

    /**
     * 이 진입이 초안에 알맹이를 적는 일을 이미 마쳤는가. ViewModel 필드로 두면 프로세스 사망을
     * 못 넘겨, 복원된 화면이 진입 인자로 그 사이의 편집 결과와 테두리를 덮어쓴다.
     */
    private var hasRecordedEntrySubject: Boolean
        get() = savedStateHandle[KEY_RECORDED_ENTRY_SUBJECT] ?: false
        set(value) {
            savedStateHandle[KEY_RECORDED_ENTRY_SUBJECT] = value
        }

    init {
        launch(onError = { reportMissingDraft() }) {
            // "초안이 비어 있는가"가 아니라 "이 알맹이를 가리키는가"로 판정한다
            // (`specs/2026-08-20-c106-topping-place-api.md`)
            val isReuseEntry = cutoutImagePath == null
            if (isReuseEntry && !hasRecordedEntrySubject) {
                val draftSubjectPath = toppingDraftRepository.draft.first()?.subjectImagePath
                if (draftSubjectPath == subjectImagePath) {
                    hasRecordedEntrySubject = true
                } else {
                    val recorded = toppingDraftRepository.record(
                        subjectImagePath = subjectImagePath,
                        cutoutImagePath = null,
                        borderColorArgb = null,
                        borderWidthDp = null,
                    )
                    // 여기서 못 적어도 구독은 그대로 연다 — 이어지는 초안 흐름의 첫 방출이
                    // 비어 있으면 같은 reportMissingDraft 가드가 중복 없이 알린다.
                    // 못 적었으면 표시를 남기지 않아 복원된 화면이 다시 적어 본다
                    if (recorded) hasRecordedEntrySubject = true else reportMissingDraft()
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
