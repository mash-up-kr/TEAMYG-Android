package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_DEFAULT_ALIGN
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.util.TEXT_TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.util.nextTextToppingAlign
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
    /** 실측한 텍스트 박스 크기. hit-target·포커스 중심 계산에 쓴다 */
    val textBoxSize: DpSize? = null,
    val canvasSize: DpSize? = null,
    /** 사용자가 아직 손대지 않은 동안에만 캔버스 정중앙에 자동으로 놓는다 */
    val hasUserAdjustedPlacement: Boolean = false,
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

    /** 배치 상태 헤더의 X — 그만두기 */
    data object OnClickClose : CanvasTextInputIntent

    /** 배치 상태 헤더의 ✓ — 캔버스에 쌓기 */
    data object OnClickStack : CanvasTextInputIntent
}

sealed interface CanvasTextInputEffect : UiSideEffect {
    /** 내용 없이 완료해 생성을 취소한 경우 — C-001로 복귀 */
    data object NavigateBack : CanvasTextInputEffect

    /** 그만두기 또는 캔버스에 쌓기 — C-001로 복귀(쌓기는 아직 스텁, 아래 ViewModel KDoc 참고) */
    data object NavigateToCanvas : CanvasTextInputEffect
}

/**
 * C-106 텍스트 입력 ↔ 배치. 인스타그램 스토리 텍스트처럼 화면 전환 없이 한 화면·한
 * ViewModel 안에서 처리한다. 입력/배치 모드 구분은 Screen 쪽의 로컬 `isEditing` 플래그가
 * 들고(키보드 노출 여부로 가르지 않는 이유는 [com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasTextInputScreen]
 * KDoc 참고), ViewModel 은 그와 무관하게 내용·색·타입·정렬·배치 좌표만 들고 있는다 —]]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CanvasTextInputViewModel
@Inject
constructor(
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val clearToppingDraft: ClearToppingDraftUseCase,
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

            CanvasTextInputIntent.OnClickClose -> handleDiscardAndNavigateToCanvas()

            CanvasTextInputIntent.OnClickStack -> handleDiscardAndNavigateToCanvas()
        }
    }

    private fun handleOnClickConfirm() {
        if (state.value.content.isBlank()) {
            postSideEffect(CanvasTextInputEffect.NavigateBack)
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
     * (캔버스 정중앙)에 그대로 남도록 저장 좌표를 맞춘다 — 안 맞추면 기본값(0,0) 쪽으로
     * 튀어 보인다.
     *
     * 캔버스 실측 크기와 텍스트 박스 크기를 둘 다 알아야 계산되고, 이 둘은 서로 다른 시점에
     * 들어오므로 어느 쪽이 먼저 도착하든 매번 다시 시도한다.
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

    private fun handleDiscardAndNavigateToCanvas() {
        launch {
            clearToppingDraft()
            postSideEffect(CanvasTextInputEffect.NavigateToCanvas)
        }
    }
}
