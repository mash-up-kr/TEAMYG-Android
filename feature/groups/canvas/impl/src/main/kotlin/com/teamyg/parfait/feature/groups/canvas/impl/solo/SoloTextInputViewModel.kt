package com.teamyg.parfait.feature.groups.canvas.impl.solo

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.PlaceSoloTextUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.stepTextTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingStyleOf
import com.teamyg.parfait.feature.groups.canvas.impl.util.toTextToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.withCanvasContent
import com.teamyg.parfait.feature.groups.canvas.impl.util.withLocalIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

sealed interface SoloTextInputEffect : UiSideEffect {
    /** 혼자 공간 캔버스로 되감는다 */
    data object NavigateBack : SoloTextInputEffect

    /** 실패 사유는 갈래 없이 한 문구다. 기기 안 저장이라 네트워크 탓일 수 없다 */
    data object PlaceFailed : SoloTextInputEffect

    data object TriggerSnapHaptic : SoloTextInputEffect
}

/**
 * 혼자 공간 텍스트 입력. 화면·상태·인텐트는 그룹 텍스트 입력([CanvasTextInputUiState],
 * [CanvasTextInputIntent])을 그대로 쓰고, 저장만 기기 안 use case 로 한다.
 */
@HiltViewModel
class SoloTextInputViewModel
@Inject
constructor(
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val clearToppingDraft: ClearToppingDraftUseCase,
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase,
    private val placeSoloText: PlaceSoloTextUseCase,
) : BaseViewModel<CanvasTextInputUiState, CanvasTextInputIntent, SoloTextInputEffect>(
    CanvasTextInputUiState(),
) {
    /** 저장에 필요한 z 순서. 혼자 공간 초안일 때만 담긴다 */
    private val observedDraft = MutableStateFlow<ToppingDraft?>(null)

    init {
        observeDraft()
        observeCanvas()
    }

    private fun observeDraft() {
        launch {
            getToppingDraftFlow().collect { draft ->
                val isSolo = draft?.target == ToppingDraftTarget.Solo
                // 그룹 초안은 이 화면이 다룰 수 없다 — 저장하지 않고 돌아간다
                if (draft != null && !isSolo) postSideEffect(SoloTextInputEffect.NavigateBack)
                observedDraft.value = draft.takeIf { isSolo }
            }
        }
    }

    private fun observeCanvas() {
        launchWhileSubscribed(source = { getSoloCanvasFlow() }) { result ->
            result
                .onSuccess { canvas ->
                    updateState {
                        withCanvasContent(
                            background = canvas.background,
                            toppings = canvas.toppings,
                            texts = canvas.texts,
                        )
                    }
                }
                // 배경·기존 토핑 없이도 글은 쓸 수 있어 화면을 막지 않는다
                .onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 캔버스를 읽지 못했다" } }
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

            is CanvasTextInputIntent.OnToppingTransform -> {
                val step = state.value.stepTextTransform(intent.pan, intent.zoom, intent.rotationDelta)
                updateState { step.state }
                if (step.justSnapped) postSideEffect(SoloTextInputEffect.TriggerSnapHaptic)
            }

            // 내용이 있으면 쌓기 버튼이 따로 저장한다 — 완료만으로는 취소할 때만 나간다
            CanvasTextInputIntent.OnClickConfirm -> if (state.value.content.isBlank()) discardAndNavigateBack()

            CanvasTextInputIntent.OnClickStack -> handleOnClickStack()

            CanvasTextInputIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                discardAndNavigateBack()
            }
        }
    }

    private fun handleOnClickStack() {
        val current = state.value
        val draft = observedDraft.value
        val boxSize = current.textBoxSize
        val canvasSize = current.canvasSize
        if (current.content.isBlank() || draft == null || boxSize == null || canvasSize == null) {
            viewModelLogger.d { "쌓기를 건너뛴다 - 빈 내용이거나 혼자 공간 초안·측정값이 아직 없다" }
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

        launch(key = STACK_JOB_KEY, onError = { throwable -> failToPlace(throwable) }) {
            updateState { copy(isLoading = true) }
            try {
                val result = placeSoloText(text = current.content, transform = transform, style = style)
                result.onSuccess {
                    // 되감기를 먼저 알린다 — 초안을 비우면 구독이 초안을 null 로 되돌린다
                    postSideEffect(SoloTextInputEffect.NavigateBack)
                    clearToppingDraft()
                }
                result.onFailure { throwable -> failToPlace(throwable) }
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun failToPlace(throwable: Throwable) {
        viewModelLogger.e(throwable) { "혼자 공간 텍스트를 저장하지 못했다" }
        postSideEffect(SoloTextInputEffect.PlaceFailed)
    }

    private fun discardAndNavigateBack() {
        launch {
            clearToppingDraft()
            postSideEffect(SoloTextInputEffect.NavigateBack)
        }
    }

    private companion object {
        /** 연타로 두 번 보내면 같은 글이 두 개 쌓인다 */
        const val STACK_JOB_KEY = "solo-text-input-stack"
    }
}
