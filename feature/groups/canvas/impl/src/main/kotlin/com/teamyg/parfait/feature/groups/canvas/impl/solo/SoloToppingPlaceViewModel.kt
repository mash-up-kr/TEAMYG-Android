package com.teamyg.parfait.feature.groups.canvas.impl.solo

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.PlaceSoloToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.clampPanelBorderWidthDp
import com.teamyg.parfait.feature.groups.canvas.impl.util.nextToppingPositionZ
import com.teamyg.parfait.feature.groups.canvas.impl.util.panelBorderForColor
import com.teamyg.parfait.feature.groups.canvas.impl.util.stepToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.withCanvasContent
import com.teamyg.parfait.feature.groups.canvas.impl.util.withInitialPlacementIfNeeded
import com.teamyg.parfait.feature.groups.canvas.impl.util.withPanelWidth
import com.teamyg.parfait.feature.groups.canvas.impl.util.withTransformGestureEnded
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface SoloToppingPlaceEffect : UiSideEffect {
    data object NavigateBack : SoloToppingPlaceEffect

    /** 토핑 만들기를 접는다. 사이에 쌓인 화면까지 걷고 혼자 공간 캔버스로 되감는다 */
    data object QuitToCanvas : SoloToppingPlaceEffect

    /** 초안이 가리키던 캐시 파일은 초안보다 먼저 사라질 수 있다 */
    data object DraftMissing : SoloToppingPlaceEffect

    data object PlaceSucceeded : SoloToppingPlaceEffect

    /** 실패 사유는 갈래 없이 한 문구다. 기기 안 저장이라 네트워크 탓일 수 없다 */
    data object PlaceFailed : SoloToppingPlaceEffect

    /** painter 가 아직 그림을 못 들었는데 확인을 눌렀다 */
    data object ToppingImageNotReady : SoloToppingPlaceEffect

    data object TriggerSnapHaptic : SoloToppingPlaceEffect
}

/**
 * 혼자 공간 토핑 배치. 화면·상태·인텐트는 그룹 배치([CanvasToppingPlaceUiState],
 * [CanvasToppingPlaceIntent])를 그대로 쓰고, 저장만 기기 안 use case 로 한다.
 *
 * 그룹과 달리 z 순서는 확정 때 언제나 지금 캔버스로 다시 센다 — 캔버스가 하나뿐이라
 * 하루 경계로 다른 캔버스를 볼 일이 없다.
 */
@HiltViewModel
class SoloToppingPlaceViewModel
@Inject
constructor(
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val clearToppingDraft: ClearToppingDraftUseCase,
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase,
    private val placeSoloTopping: PlaceSoloToppingUseCase,
    private val addRecentImage: AddRecentImageUseCase,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, SoloToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(),
) {
    /** 캔버스를 한 번이라도 읽었는가. 못 읽었으면 z 를 초안 값으로 둔다 */
    private var hasCanvas = false

    init {
        observeDraft()
        observeCanvas()
    }

    private fun observeDraft() {
        launch(onError = { postSideEffect(SoloToppingPlaceEffect.DraftMissing) }) {
            getToppingDraftFlow().collect { draft ->
                val isSolo = draft?.target == ToppingDraftTarget.Solo
                // 그룹 초안은 이 화면이 다룰 수 없다 — 배치하지 않고 캔버스로 돌아간다
                if (draft != null && !isSolo) postSideEffect(SoloToppingPlaceEffect.QuitToCanvas)
                updateState {
                    copy(
                        toppingImagePath = draft?.subjectImagePath.takeIf { isSolo },
                        toppingSourceLongSide = draft?.sourceLongSide.takeIf { isSolo },
                        nextPositionZ = draft?.nextPositionZ.takeIf { isSolo },
                        isDraftLoaded = true,
                    )
                }
            }
        }
    }

    private fun observeCanvas() {
        launchWhileSubscribed(source = { getSoloCanvasFlow() }) { result ->
            result
                .onSuccess { canvas ->
                    hasCanvas = true
                    updateState {
                        withCanvasContent(
                            background = canvas.background,
                            toppings = canvas.toppings,
                            texts = canvas.texts,
                        )
                    }
                }
                // 배경·기존 토핑 없이도 놓을 수는 있어 화면을 막지 않는다
                .onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 캔버스를 읽지 못했다" } }
        }
    }

    override fun processIntent(intent: CanvasToppingPlaceIntent) {
        when (intent) {
            CanvasToppingPlaceIntent.OnClickBack -> postSideEffect(SoloToppingPlaceEffect.NavigateBack)

            CanvasToppingPlaceIntent.OnClickClose -> updateState { copy(showQuitDialog = true) }

            CanvasToppingPlaceIntent.OnSystemBack -> handleOnSystemBack()

            CanvasToppingPlaceIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                postSideEffect(SoloToppingPlaceEffect.QuitToCanvas)
            }

            CanvasToppingPlaceIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }

            CanvasToppingPlaceIntent.OnClickConfirm -> handleOnClickConfirm()

            CanvasToppingPlaceIntent.OnClickTopping,
            CanvasToppingPlaceIntent.OnToggleBorderPanel,
            -> updateState {
                // 그림이 뜨기 전에는 열지 않는다 — 테두리를 입힐 실루엣이 아직 없다
                copy(isBorderPanelOpen = !isBorderPanelOpen && isToppingImageReady)
            }

            CanvasToppingPlaceIntent.OnDismissBorderPanel -> updateState { copy(isBorderPanelOpen = false) }

            is CanvasToppingPlaceIntent.OnSelectBorderColor -> updateState {
                copy(border = panelBorderForColor(intent.colorArgb, panelBorderWidthDp))
            }

            is CanvasToppingPlaceIntent.OnChangeBorderWidth -> updateState {
                val widthDp = clampPanelBorderWidthDp(intent.widthDp)
                copy(pendingBorderWidthDp = widthDp, border = border.withPanelWidth(widthDp))
            }

            is CanvasToppingPlaceIntent.OnToppingTransform -> {
                val step = state.value.stepToppingTransform(intent.pan, intent.zoom, intent.rotationDelta) ?: return
                updateState { step.state }
                if (step.justSnapped) postSideEffect(SoloToppingPlaceEffect.TriggerSnapHaptic)
            }

            CanvasToppingPlaceIntent.OnToppingTransformGestureEnd -> updateState { withTransformGestureEnded() }

            is CanvasToppingPlaceIntent.OnCanvasMeasured -> updateState {
                copy(canvasSize = intent.canvasSize).withInitialPlacementIfNeeded()
            }

            is CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured -> updateState {
                copy(toppingBaseSize = intent.baseSize).withInitialPlacementIfNeeded()
            }

            is CanvasToppingPlaceIntent.OnToppingImageReadyChanged -> updateState {
                copy(isToppingImageReady = intent.isReady)
            }
        }
    }

    private fun handleOnSystemBack() {
        val current = state.value
        when (toppingPanelBackAction(current.isLoading, current.isBorderPanelOpen)) {
            ToppingPanelBackAction.Ignore -> Unit
            ToppingPanelBackAction.ClosePanel -> updateState { copy(isBorderPanelOpen = false) }
            ToppingPanelBackAction.NavigateBack -> postSideEffect(SoloToppingPlaceEffect.NavigateBack)
        }
    }

    private fun handleOnClickConfirm() {
        val current = state.value
        if (!current.isDraftLoaded) return

        val imagePath = current.toppingImagePath
        val draftPositionZ = current.nextPositionZ
        if (imagePath == null || draftPositionZ == null) {
            postSideEffect(SoloToppingPlaceEffect.DraftMissing)
            return
        }

        // 그림이 아직 없으면 실측이 폴백 크기다. 그것으로 계산한 배율이 저장에 굳는다
        val canvasSize = current.canvasSize
        val baseSize = current.toppingBaseSize
        if (!current.isToppingImageReady || canvasSize == null || baseSize == null) {
            postSideEffect(SoloToppingPlaceEffect.ToppingImageNotReady)
            return
        }

        val positionZ = if (hasCanvas) {
            nextToppingPositionZ(current.existingToppings, current.existingTexts)
        } else {
            draftPositionZ
        }
        val transform = toToppingTransform(
            offsetX = current.offsetX,
            offsetY = current.offsetY,
            scale = current.scale,
            rotationDegrees = current.rotationDegrees,
            canvasSize = canvasSize,
            toppingBaseSize = baseSize,
            positionZ = positionZ,
        )
        launch(key = CONFIRM_KEY, onError = { throwable -> failToPlace(throwable) }) {
            updateState { copy(isLoading = true) }
            try {
                // 테두리 조립은 던질 수 있어 launch 안에서 부른다 — 밖이면 onError 를 못 만나 크래시다
                val border = current.border.toToppingBorder()

                placeSoloTopping(
                    filePath = imagePath,
                    transform = transform,
                    border = border,
                ).onSuccess {
                    // 알림보다 먼저 남긴다 — 되감기가 viewModelScope 를 취소하면 뒤 코드가 끊긴다
                    runSuspendCatching {
                        addRecentImage(source = imagePath, kind = RecentImageKind.CUTOUT)
                    }.onFailure { throwable -> viewModelLogger.d { "recent cutout save failed - $throwable" } }

                    // 되감기를 먼저 알린다 — 초안을 비우면 구독이 알맹이를 null 로 되돌린다
                    postSideEffect(SoloToppingPlaceEffect.PlaceSucceeded)
                    // 저장은 끝났다 — 초안 정리 실패를 저장 실패로 알리면 안 된다
                    runSuspendCatching { clearToppingDraft() }
                        .onFailure { throwable -> viewModelLogger.w(throwable) { "토핑 초안을 비우지 못했다" } }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "혼자 공간 토핑을 저장하지 못했다" }
                    postSideEffect(SoloToppingPlaceEffect.PlaceFailed)
                }
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun failToPlace(throwable: Throwable) {
        viewModelLogger.e(throwable) { "혼자 공간 토핑 배치 중 예기치 못한 실패" }
        postSideEffect(SoloToppingPlaceEffect.PlaceFailed)
    }

    private companion object {
        /** 연타로 두 번 놓이면 같은 사진이 겹쳐 쌓인다 */
        const val CONFIRM_KEY = "confirm"
    }
}
