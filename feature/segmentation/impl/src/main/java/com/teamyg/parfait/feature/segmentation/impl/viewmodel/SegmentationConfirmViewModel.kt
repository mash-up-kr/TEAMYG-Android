package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.usecase.member.CompleteTutorialUseCase
import com.teamyg.parfait.domain.usecase.member.GetTutorialVisibleFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.EnsureDraftSubjectRecordedUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

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
    /** 앱 설치 후 이 화면 첫 진입에서만 `true`. 화면 전체를 덮는다 */
    val isTutorialVisible: Boolean = false,
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

    /** 튜토리얼 칩을 눌렀다. 한 장뿐이라 그대로 닫고 다시 뜨지 않게 남긴다 */
    data object OnConfirmTutorial : SegmentationConfirmIntent
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
    private val ensureDraftSubjectRecorded: EnsureDraftSubjectRecordedUseCase,
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val recordToppingDraft: RecordToppingDraftUseCase,
    private val getTutorialVisibleFlowUseCase: GetTutorialVisibleFlowUseCase,
    private val completeTutorialUseCase: CompleteTutorialUseCase,
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
            val isReuseEntry = cutoutImagePath == null
            if (isReuseEntry && !hasRecordedEntrySubject) {
                // 못 맞춰도 구독은 그대로 연다 — 첫 방출이 비어 있으면 같은
                // reportMissingDraft 가드가 중복 없이 알린다
                if (ensureDraftSubjectRecorded(subjectImagePath)) {
                    hasRecordedEntrySubject = true
                } else {
                    // 표시를 남기지 않아 복원된 화면이 다시 맞춰 본다
                    reportMissingDraft()
                }
            }

            collectDraft()
        }

        observeTutorial()
    }

    override fun processIntent(intent: SegmentationConfirmIntent) {
        when (intent) {
            is SegmentationConfirmIntent.OnEditResult -> record(intent.result)
            is SegmentationConfirmIntent.OnConfirmTutorial -> handleConfirmTutorial()
        }
    }

    private fun observeTutorial() {
        launchWhileSubscribed(source = { getTutorialVisibleFlowUseCase(TutorialKind.SEGMENTATION) }) { isVisible ->
            updateState { copy(isTutorialVisible = isVisible) }
        }
    }

    private fun handleConfirmTutorial() {
        updateState { copy(isTutorialVisible = false) }
        launch(key = COMPLETE_TUTORIAL_KEY) { completeTutorialUseCase(TutorialKind.SEGMENTATION) }
    }

    private suspend fun collectDraft() {
        getToppingDraftFlow().collect { draft ->
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
            val recorded = recordToppingDraft(
                subjectImagePath = result.subjectImagePath,
                cutoutImagePath = result.cutoutImagePath,
                borderColorArgb = border?.colorArgb,
                borderWidthDp = border?.widthDp,
                sourceLongSide = result.sourceLongSide?.let(::SourceLongSide),
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

    private companion object {
        const val COMPLETE_TUTORIAL_KEY = "completeTutorial"
    }
}
