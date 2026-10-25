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
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.jvm.extension.graphemeClusterCount
import com.teamyg.parfait.core.util.jvm.extension.truncateToGraphemeClusters
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_FONT_SIZE_SP
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_DEFAULT_ALIGN
import com.teamyg.parfait.feature.groups.canvas.impl.model.nextTextToppingAlign
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

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
    /** 키보드가 내려가 있는 동안(배치 상태)의 자리·크기·회전. 저장이 아니라 화면 전용 상태다 */
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val textBoxSize: DpSize? = null,
    val canvasSize: DpSize? = null,
    /** 사용자가 아직 손대지 않은 동안에만 캔버스 정중앙에 자동으로 놓는다 */
    val hasUserAdjustedPlacement: Boolean = false,
    val showQuitDialog: Boolean = false,
    /** 자동 크기(디자인 단위). 키보드를 내린 순간의 값을 배치에서 그대로 쓴다 */
    val designFontSize: Float = TEXT_TOPPING_FONT_SIZE_SP,
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
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CanvasTextInputViewModel
@Inject
constructor(
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val clearToppingDraft: ClearToppingDraftUseCase,
    private val analyticsLogger: AnalyticsLogger,
) : BaseViewModel<CanvasTextInputUiState, CanvasTextInputIntent, CanvasTextInputEffect>(
    CanvasTextInputUiState(),
) {
    /** 초안이 그룹을 알려 주기 전엔 비어 있다. [CanvasToppingPlaceViewModel] 과 같은 이유로 [state] 를 게이트로 쓰지 않는다 */
    private val observedGroupId = MutableStateFlow<GroupId?>(null)

    init {
        observeDraft()
        observeCanvas()
    }

    /** 이 화면이 쓰는 건 캔버스 식별값뿐이다 — 내용·색·타입·정렬은 화면 수명 동안 로컬 상태로만 있는다 */
    private fun observeDraft() {
        launch {
            getToppingDraftFlow().collect { draft ->
                observedGroupId.value = draft?.groupId
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
            updateState { withCanvas(canvas) }
        }
    }

    private fun CanvasTextInputUiState.withCanvas(canvas: CanvasVO): CanvasTextInputUiState = copy(
        backgroundColor = (canvas.background as? CanvasBackground.Color)
            ?.value
            ?.toColorOrNull()
            ?: backgroundColor,
        backgroundImageUrl = (canvas.background as? CanvasBackground.Image)?.url,
        existingToppings = canvas.toppings.sortedBy { topping -> topping.transform.positionZ },
    )

    override fun processIntent(intent: CanvasTextInputIntent) {
        when (intent) {
            is CanvasTextInputIntent.OnContentChange -> updateState {
                copy(content = intent.value.truncateToGraphemeClusters(TEXT_TOPPING_MAX_LENGTH))
            }

            is CanvasTextInputIntent.OnSelectColor -> updateState {
                copy(selectedColor = intent.color)
            }

            CanvasTextInputIntent.OnToggleColorPopover -> updateState {
                copy(isColorPopoverOpen = !isColorPopoverOpen)
            }

            CanvasTextInputIntent.OnDismissColorPopover -> updateState { copy(isColorPopoverOpen = false) }

            is CanvasTextInputIntent.OnAutoFontSizeChange -> updateState {
                copy(designFontSize = intent.designFontSize)
            }

            CanvasTextInputIntent.OnClickType -> updateState { copy(textType = textType.next()) }

            CanvasTextInputIntent.OnClickAlign -> updateState { copy(align = align.nextTextToppingAlign()) }

            is CanvasTextInputIntent.OnTextBoxMeasured -> updateState {
                copy(textBoxSize = intent.size).applyInitialPlacementIfNeeded()
            }

            is CanvasTextInputIntent.OnCanvasMeasured -> updateState {
                copy(canvasSize = intent.size).applyInitialPlacementIfNeeded()
            }

            is CanvasTextInputIntent.OnToppingTransform -> handleOnToppingTransform(intent)

            is CanvasTextInputIntent.OnDragPlacementSeed -> updateState {
                copy(offsetX = intent.offsetX, offsetY = intent.offsetY, hasUserAdjustedPlacement = true)
            }

            CanvasTextInputIntent.OnClickConfirm -> handleOnClickConfirm()

            CanvasTextInputIntent.OnClickClose -> updateState { copy(showQuitDialog = true) }

            CanvasTextInputIntent.OnClickStack -> handleOnClickStack()

            CanvasTextInputIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                handleDiscardAndNavigateBack()
            }

            CanvasTextInputIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }
        }
    }

    private fun handleOnClickConfirm() {
        if (state.value.content.isBlank()) {
            handleDiscardAndNavigateBack()
        }
    }

    private fun handleOnToppingTransform(intent: CanvasTextInputIntent.OnToppingTransform) {
        updateState {
            copy(
                offsetX = offsetX + intent.pan.x,
                offsetY = offsetY + intent.pan.y,
                scale = (scale * intent.zoom).coerceAtLeast(TEXT_TOPPING_MIN_SCALE),
                rotationDegrees = rotationDegrees + intent.rotationDelta,
                hasUserAdjustedPlacement = true,
            )
        }
    }

    /**
     * 사용자가 아직 손대지 않았다면, 키보드가 처음 내려갈 때 텍스트가 타이핑 중이던 자리
     * (캔버스 정중앙)에 그대로 남도록 저장 좌표를 맞춘다
     */
    private fun CanvasTextInputUiState.applyInitialPlacementIfNeeded(): CanvasTextInputUiState {
        if (hasUserAdjustedPlacement) return this
        val canvas = canvasSize ?: return this
        val box = textBoxSize ?: return this

        return copy(
            offsetX = (canvas.width - box.width) / 2,
            offsetY = (canvas.height - box.height) / 2,
        )
    }

    // 서버 API 가 없어 저장하지 않는 스텁이다 — OQ-P-439
    private fun handleOnClickStack() {
        val groupId = observedGroupId.value?.value?.toString() ?: ""
        analyticsLogger.logEvent(
            AnalyticsEvent(
                type = AnalyticsEventType.CANVAS_TEXT_STACK_TAPPED,
                params = mapOf("group_id" to groupId),
            ),
        )
        handleDiscardAndNavigateBack()
    }

    private fun handleDiscardAndNavigateBack() {
        launch {
            clearToppingDraft()
            postSideEffect(CanvasTextInputEffect.NavigateBack)
        }
    }
}
