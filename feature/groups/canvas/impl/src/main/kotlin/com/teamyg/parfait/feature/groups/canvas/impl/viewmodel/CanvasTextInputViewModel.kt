package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.util.jvm.extension.graphemeClusterCount
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.review.RecordReviewPromptUploadUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.PlaceTextToppingUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.PlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_FONT_SIZE_SP
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_DEFAULT_ALIGN
import com.teamyg.parfait.feature.groups.canvas.impl.util.stepTextTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingStyleOf
import com.teamyg.parfait.feature.groups.canvas.impl.util.toPlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.util.toTextToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.withCanvasContent
import com.teamyg.parfait.feature.groups.canvas.impl.util.withLocalIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

data class CanvasTextInputUiState(
    val content: String = "",
    val selectedColor: TextToppingColor = TextToppingColor.White,
    val textType: TextToppingType = TextToppingType.DEFAULT,
    val align: TextAlign = TEXT_TOPPING_DEFAULT_ALIGN,
    val isColorPopoverOpen: Boolean = false,
    /** [backgroundImageUrl] 이 있으면 그쪽이 우선이고, 이 색은 이미지가 없을 때만 그려진다 */
    val backgroundColor: Color = YGAtomicColors.Gray.White,
    val backgroundImageUrl: String? = null,
    val existingToppings: List<CanvasToppingVO> = emptyList(),
    val existingTexts: List<CanvasTextToppingVO> = emptyList(),
    /** 키보드가 내려가 있는 동안(배치 상태)의 자리·크기·회전. 저장이 아니라 화면 전용 상태다 */
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    /** 중앙 가이드 스냅 전, 손가락이 실제로 가리키는 중심 좌표. [CanvasToppingPlaceUiState]와
     *  같은 이유로 [offsetX]/[offsetY] 와 따로 둔다. */
    val rawOffsetX: Dp = 0.dp,
    val rawOffsetY: Dp = 0.dp,
    val isCenterGuideXVisible: Boolean = false,
    val isCenterGuideYVisible: Boolean = false,
    /** 각도 가이드 스냅 전, 손가락이 실제로 누적시킨 원값. [rawOffsetX]/[rawOffsetY] 와 같은 이유로 따로 둔다 */
    val rawRotationDegrees: Float = 0f,
    val isAngleGuideVisible: Boolean = false,
    /** true 면 0/180도(가로선), false 면 90/270도(세로선) */
    val isAngleGuideHorizontal: Boolean = false,
    /** 이번 이동 제스처에서 회전이 있었는가. 실제 회전이 있었을 때만 각도 가이드·스냅을 켠다. 제스처가 끝나면 false. */
    val hasRotatedThisGesture: Boolean = false,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val textBoxSize: DpSize? = null,
    val canvasSize: DpSize? = null,
    /** 사용자가 아직 손대지 않은 동안에만 캔버스 정중앙에 자동으로 놓는다 */
    val hasUserAdjustedPlacement: Boolean = false,
    val showQuitDialog: Boolean = false,
    /** 자동 크기(디자인 단위). 키보드를 내린 순간의 값을 배치에서 그대로 쓴다 */
    val designFontSize: Float = TEXT_TOPPING_FONT_SIZE_SP,
    val isLoading: Boolean = false,
) : UiState {
    val characterCount: Int get() = content.graphemeClusterCount()
    val isAtLimit: Boolean get() = characterCount >= TEXT_TOPPING_MAX_LENGTH
}

sealed interface CanvasTextInputIntent : UiIntent {
    data class OnContentChange(
        val value: String,
    ) : CanvasTextInputIntent

    data class OnSelectColor(
        val color: TextToppingColor,
    ) : CanvasTextInputIntent

    data object OnToggleColorPopover : CanvasTextInputIntent

    data object OnDismissColorPopover : CanvasTextInputIntent

    data class OnAutoFontSizeChange(
        val designFontSize: Float,
    ) : CanvasTextInputIntent

    data object OnClickType : CanvasTextInputIntent

    data object OnClickAlign : CanvasTextInputIntent

    data class OnTextBoxMeasured(
        val size: DpSize,
    ) : CanvasTextInputIntent

    data class OnCanvasMeasured(
        val size: DpSize,
    ) : CanvasTextInputIntent

    data class OnToppingTransform(
        val pan: DpOffset,
        val zoom: Float,
        val rotationDelta: Float,
    ) : CanvasTextInputIntent

    data object OnToppingTransformGestureEnd : CanvasTextInputIntent

    /** 키보드 ↑ 상태에서 끌기로 배치에 진입할 때, 점프 없이 현재 보이는 자리에서 이어지도록 배치 좌표를 심는다 */
    data class OnDragPlacementSeed(
        val offsetX: Dp,
        val offsetY: Dp,
    ) : CanvasTextInputIntent

    /** 빈 내용으로 완료 — 생성 취소 */
    data object OnClickConfirm : CanvasTextInputIntent

    data object OnClickClose : CanvasTextInputIntent

    data object OnClickStack : CanvasTextInputIntent

    data object OnQuitDialogConfirm : CanvasTextInputIntent

    data object OnQuitDialogCancel : CanvasTextInputIntent
}

sealed interface CanvasTextInputEffect : UiSideEffect {
    data object NavigateBack : CanvasTextInputEffect

    data class ShowPlaceFailed(
        val failure: PlaceFailure,
    ) : CanvasTextInputEffect

    /** 토핑이 캔버스 중앙·각도 가이드에 막 스냅됐다. 햅틱을 한 번 울린다 */
    data object TriggerSnapHaptic : CanvasTextInputEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CanvasTextInputViewModel
@Inject
constructor(
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val clearToppingDraft: ClearToppingDraftUseCase,
    private val placeTextTopping: PlaceTextToppingUseCase,
    private val refreshTodayParfaitDetail: RefreshTodayParfaitDetailUseCase,
    private val recordReviewPromptUploadUseCase: RecordReviewPromptUploadUseCase,
    private val analyticsLogger: AnalyticsLogger,
) : BaseViewModel<CanvasTextInputUiState, CanvasTextInputIntent, CanvasTextInputEffect>(
    CanvasTextInputUiState(),
) {
    /** 초안이 그룹을 알려 주기 전엔 비어 있다. [CanvasToppingPlaceViewModel] 과 같은 이유로 [state] 를 게이트로 쓰지 않는다 */
    private val observedGroupId = MutableStateFlow<GroupId?>(null)

    /** 저장에 필요한 식별값·z 순서. 쌓기 시점의 값을 읽는다 */
    private val observedDraft = MutableStateFlow<ToppingDraft?>(null)

    init {
        observeDraft()
        observeCanvas()
    }

    /** 이 화면이 쓰는 건 캔버스 식별값뿐이다 — 내용·색·타입·정렬은 화면 수명 동안 로컬 상태로만 있는다 */
    private fun observeDraft() {
        launch {
            getToppingDraftFlow().collect { draft ->
                val target = draft?.target as? ToppingDraftTarget.Group
                // 혼자 공간 초안은 이 화면이 다룰 수 없다 — 저장하지 않고 돌아간다
                if (draft != null && target == null) {
                    postSideEffect(CanvasTextInputEffect.NavigateBack)
                }
                observedDraft.value = draft
                observedGroupId.value = target?.groupId
            }
        }
    }

    /** 화면이 보이는 동안만 오늘 캔버스를 구독한다(`adr/0029-canvas-today-ssot-polling.md`) */
    private fun observeCanvas() {
        launchWhileSubscribed(
            source = {
                observedGroupId
                    .filterNotNull()
                    .flatMapLatest { groupId -> getTodayParfaitFlowUseCase(groupId) }
            },
        ) { canvas ->
            // null 은 무시한다 — 비우면 배경이 흰색으로 튄다
            if (canvas == null) return@launchWhileSubscribed
            updateState {
                withCanvasContent(
                    background = canvas.background,
                    toppings = canvas.toppings,
                    texts = canvas.texts,
                )
            }
        }
    }

    override fun processIntent(intent: CanvasTextInputIntent) {
        when (intent) {
            is CanvasTextInputIntent.OnContentChange,
            is CanvasTextInputIntent.OnSelectColor,
            CanvasTextInputIntent.OnToggleColorPopover,
            CanvasTextInputIntent.OnDismissColorPopover,
            is CanvasTextInputIntent.OnAutoFontSizeChange,
            CanvasTextInputIntent.OnClickType,
            CanvasTextInputIntent.OnClickAlign,
            is CanvasTextInputIntent.OnTextBoxMeasured,
            is CanvasTextInputIntent.OnCanvasMeasured,
            CanvasTextInputIntent.OnToppingTransformGestureEnd,
            is CanvasTextInputIntent.OnDragPlacementSeed,
            CanvasTextInputIntent.OnClickClose,
            CanvasTextInputIntent.OnQuitDialogCancel,
            -> updateState { withLocalIntent(intent) }

            is CanvasTextInputIntent.OnToppingTransform -> handleOnToppingTransform(intent)

            CanvasTextInputIntent.OnClickConfirm -> handleOnClickConfirm()

            CanvasTextInputIntent.OnClickStack -> handleOnClickStack()

            CanvasTextInputIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                handleDiscardAndNavigateBack()
            }
        }
    }

    private fun handleOnClickConfirm() {
        if (state.value.content.isNotBlank()) return

        handleDiscardAndNavigateBack()
    }

    private fun handleOnToppingTransform(intent: CanvasTextInputIntent.OnToppingTransform) {
        val step = state.value.stepTextTransform(intent.pan, intent.zoom, intent.rotationDelta)
        updateState { step.state }
        if (step.justSnapped) postSideEffect(CanvasTextInputEffect.TriggerSnapHaptic)
    }

    private fun handleOnClickStack() {
        val current = state.value
        val draft = observedDraft.value
        val target = draft?.target as? ToppingDraftTarget.Group
        analyticsLogger.logEvent(
            AnalyticsEvent(
                type = AnalyticsEventType.CANVAS_TEXT_STACK_TAPPED,
                params = mapOf("group_id" to (target?.groupId?.value?.toString() ?: "")),
            ),
        )

        val boxSize = current.textBoxSize
        val canvasSize = current.canvasSize
        // 화면이 빈 내용의 버튼을 막지만, POST 가 멱등이 아니라 여기서도 막는다
        if (current.content.isBlank() || draft == null || target == null || boxSize == null || canvasSize == null) {
            viewModelLogger.d { "쌓기를 건너뛴다 - 빈 내용이거나 초안·측정값이 아직 없다" }
            return
        }

        val transform = toTextToppingTransform(
            offsetX = current.offsetX,
            offsetY = current.offsetY,
            boxSize = boxSize,
            canvasSize = canvasSize,
            scale = current.scale,
            rotationDegrees = current.rotationDegrees,
            positionZ = draft.nextPositionZ,
        )
        val style = textToppingStyleOf(
            color = current.selectedColor,
            type = current.textType,
            align = current.align,
            designFontSize = current.designFontSize,
        )

        launch(
            key = STACK_JOB_KEY,
            onError = { throwable ->
                viewModelLogger.e(throwable) { "텍스트 토핑을 쌓지 못했다" }
                postSideEffect(CanvasTextInputEffect.ShowPlaceFailed(PlaceFailure.Retryable))
            },
        ) {
            updateState { copy(isLoading = true) }
            try {
                // POST 는 멱등이 아니라 재시도하지 않는다
                placeTextTopping(
                    groupId = target.groupId,
                    parfaitId = target.parfaitId,
                    text = current.content,
                    transform = transform,
                    style = style,
                ).onSuccess {
                    // 나가기 전에 기다린다 — 라우트가 되감으며 viewModelScope 가 취소되면 갱신이 끊긴다
                    runSuspendCatching {
                        refreshTodayParfaitDetail(groupId = target.groupId, parfaitId = target.parfaitId)
                    }.onFailure { throwable ->
                        viewModelLogger.e(throwable) { "오늘 캔버스를 다시 받지 못했다" }
                    }
                    clearToppingDraft()
                    // NavigateBack 뒤로 미루지 않는다 — 되감기가 viewModelScope 를 취소하면 끊긴다
                    recordReviewPromptUploadUseCase()
                    postSideEffect(CanvasTextInputEffect.NavigateBack)
                }.onFailure { throwable ->
                    val error = throwable as? AppError ?: AppError.Unexpected(throwable)
                    val failure = error.toPlaceFailure()
                    if (failure == PlaceFailure.Uncertain) {
                        // 응답만 유실됐다면 이미 쌓였다 — 뒤 캔버스에 보이도록 갱신해 둔다
                        runSuspendCatching {
                            refreshTodayParfaitDetail(groupId = target.groupId, parfaitId = target.parfaitId)
                        }.onFailure { refreshError ->
                            viewModelLogger.e(refreshError) { "오늘 캔버스를 다시 받지 못했다" }
                        }
                    }
                    postSideEffect(CanvasTextInputEffect.ShowPlaceFailed(failure))
                }
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun handleDiscardAndNavigateBack() {
        launch {
            clearToppingDraft()
            postSideEffect(CanvasTextInputEffect.NavigateBack)
        }
    }
}

/** 쌓기 중복 실행 키. 연타로 두 번 보내면 같은 글이 두 개 쌓인다 */
private const val STACK_JOB_KEY = "canvas-text-input-stack"
