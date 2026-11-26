package com.teamyg.parfait.feature.groups.canvas.impl.solo

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.usecase.solo.DeleteSoloTextUseCase
import com.teamyg.parfait.domain.usecase.solo.DeleteSoloToppingUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.UpdateSoloToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.solo.UpdateSoloTransformsUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingSaveFailures
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.dirtyChanges
import com.teamyg.parfait.feature.groups.canvas.impl.util.focusedCenterGuideStep
import com.teamyg.parfait.feature.groups.canvas.impl.util.ignoresToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.mergeTexts
import com.teamyg.parfait.feature.groups.canvas.impl.util.mergeToppings
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableText
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.util.toTransformUpdate
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.withBorderColorSelected
import com.teamyg.parfait.feature.groups.canvas.impl.util.withBorderWidthChanged
import com.teamyg.parfait.feature.groups.canvas.impl.util.withCanvasBackground
import com.teamyg.parfait.feature.groups.canvas.impl.util.withDeleteFocusedClicked
import com.teamyg.parfait.feature.groups.canvas.impl.util.withEmptyCanvasClicked
import com.teamyg.parfait.feature.groups.canvas.impl.util.withFocus
import com.teamyg.parfait.feature.groups.canvas.impl.util.withFocusedTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.withInitialFocusIfNeeded
import com.teamyg.parfait.feature.groups.canvas.impl.util.withTransformGestureEnded
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

sealed interface SoloToppingArrangeEffect : UiSideEffect {
    /** 혼자 공간 캔버스로 되감는다 */
    data object NavigateBack : SoloToppingArrangeEffect

    /** 기기 안 저장이라 네트워크 탓일 수 없어 사유를 가르지 않는다 */
    data object SaveFailed : SoloToppingArrangeEffect

    data object DeleteFailed : SoloToppingArrangeEffect

    data object TriggerSnapHaptic : SoloToppingArrangeEffect
}

/**
 * 혼자 공간 토핑 재편집. 화면·상태·인텐트는 그룹 재편집([CanvasToppingArrangeUiState],
 * [CanvasToppingArrangeIntent])을 그대로 쓰고, 저장만 기기 안 use case 로 한다.
 *
 * 혼자 공간의 토핑은 전부 내 것이라 남의 토핑 안내가 없다.
 */
@HiltViewModel(assistedFactory = SoloToppingArrangeViewModel.Factory::class)
class SoloToppingArrangeViewModel
@AssistedInject
constructor(
    @Assisted("initialToppingId") private val initialToppingId: Long,
    @Assisted("initialIsText") private val initialIsText: Boolean,
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase,
    private val updateSoloTransforms: UpdateSoloTransformsUseCase,
    private val updateSoloToppingBorder: UpdateSoloToppingBorderUseCase,
    private val deleteSoloTopping: DeleteSoloToppingUseCase,
    private val deleteSoloText: DeleteSoloTextUseCase,
) : BaseViewModel<CanvasToppingArrangeUiState, CanvasToppingArrangeIntent, SoloToppingArrangeEffect>(
    initialState = CanvasToppingArrangeUiState(),
) {
    private var hasSeededFromCanvas = false

    /** 마지막으로 읽은 그대로의 토핑. 확인 때 손댄 토핑의 어느 축이 바뀌었는지 가리는 데만 쓴다 */
    private var storedToppings: List<EditableTopping> = emptyList()

    private var storedTexts: List<EditableText> = emptyList()

    init {
        observeCanvas()
    }

    private fun observeCanvas() {
        launchWhileSubscribed(source = { getSoloCanvasFlow() }) { result ->
            result
                .onSuccess { canvas ->
                    val incoming = canvas.toppings
                        .sortedBy { topping -> topping.transform.positionZ }
                        .map { topping -> topping.toEditableTopping() }
                    val incomingTexts = canvas.texts.map { text -> text.toEditableText() }

                    storedToppings = incoming
                    storedTexts = incomingTexts
                    updateState {
                        withCanvasBackground(canvas.background)
                            .withInitialFocusIfNeeded(hasSeededFromCanvas, initialToppingId, initialIsText)
                            .mergeToppings(incoming)
                            .mergeTexts(incomingTexts)
                    }
                    hasSeededFromCanvas = true
                }
                // 고칠 것이 안 그려질 뿐 닫기로 나갈 수 있어 화면을 막지 않는다
                .onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 캔버스를 읽지 못했다" } }
        }
    }

    override fun processIntent(intent: CanvasToppingArrangeIntent) {
        when (intent) {
            is CanvasToppingArrangeIntent.OnClickTopping ->
                handleOnClickLayer(CanvasToppingKey.Image(intent.topping.parfaitImageId))

            is CanvasToppingArrangeIntent.OnClickText ->
                handleOnClickLayer(CanvasToppingKey.Text(intent.text.parfaitTextId))

            CanvasToppingArrangeIntent.OnClickEmptyCanvas -> updateState { withEmptyCanvasClicked() }

            CanvasToppingArrangeIntent.OnToggleBorderPanel -> updateState {
                if (canOpenBorderPanel) copy(isBorderPanelOpen = !isBorderPanelOpen) else this
            }

            CanvasToppingArrangeIntent.OnDismissBorderPanel -> updateState { copy(isBorderPanelOpen = false) }

            is CanvasToppingArrangeIntent.OnSelectBorderColor ->
                updateState { withBorderColorSelected(intent.colorArgb) }

            is CanvasToppingArrangeIntent.OnChangeBorderWidth -> updateState { withBorderWidthChanged(intent.widthDp) }

            is CanvasToppingArrangeIntent.OnToppingTransform -> handleOnToppingTransform(intent)

            CanvasToppingArrangeIntent.OnToppingTransformGestureEnd -> updateState { withTransformGestureEnded() }

            CanvasToppingArrangeIntent.OnClickDeleteToppingButton -> updateState { withDeleteFocusedClicked() }

            CanvasToppingArrangeIntent.OnClickClose -> requestQuit()

            CanvasToppingArrangeIntent.OnSystemBack -> handleOnSystemBack()

            CanvasToppingArrangeIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                postSideEffect(SoloToppingArrangeEffect.NavigateBack)
            }

            CanvasToppingArrangeIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }

            CanvasToppingArrangeIntent.OnClickConfirm -> handleOnClickConfirm()
        }
    }

    /**
     * 패널이 열려 있는 동안 캔버스 위 탭은 패널을 닫는 데만 쓴다. 포커스된 사진을 다시 탭하면
     * 패널을 열고, 텍스트는 테두리가 없어 그대로 둔다.
     */
    private fun handleOnClickLayer(key: CanvasToppingKey) {
        updateState {
            when {
                isBorderPanelOpen -> copy(isBorderPanelOpen = false)
                focusedKey != key -> withFocus(key)
                key is CanvasToppingKey.Image -> copy(isBorderPanelOpen = true)
                else -> this
            }
        }
    }

    private fun handleOnToppingTransform(intent: CanvasToppingArrangeIntent.OnToppingTransform) {
        val current = state.value
        if (ignoresToppingTransform(current.isBorderPanelOpen)) return
        val step = current.focusedCenterGuideStep(intent) ?: return

        updateState { withFocusedTransform(step, intent.zoom) }
        if (step.justSnapped) postSideEffect(SoloToppingArrangeEffect.TriggerSnapHaptic)
    }

    private fun handleOnSystemBack() {
        val current = state.value

        when (toppingPanelBackAction(current.isLoading, current.isBorderPanelOpen)) {
            ToppingPanelBackAction.Ignore -> Unit
            ToppingPanelBackAction.ClosePanel -> updateState { copy(isBorderPanelOpen = false) }
            ToppingPanelBackAction.NavigateBack -> postSideEffect(SoloToppingArrangeEffect.NavigateBack)
        }
    }

    private fun requestQuit() {
        val current = state.value
        if (current.isLoading) return

        if (current.hasUnsavedChanges) {
            updateState { copy(showQuitDialog = true) }
        } else {
            postSideEffect(SoloToppingArrangeEffect.NavigateBack)
        }
    }

    private fun handleOnClickConfirm() {
        launch(key = CONFIRM_KEY, onError = ::failToSaveUnexpectedly) {
            updateState { copy(isLoading = true) }

            val failed = saveDirty()
            // 저장한 것만 대상에서 뺀다 — 다시 누른 확인이 못 저장한 것만 보낸다
            updateState { copy(dirtyToppingIds = failed.toppingIds, dirtyTextIds = failed.textIds) }
            if (!failed.isEmpty()) {
                updateState { copy(isLoading = false) }
                postSideEffect(SoloToppingArrangeEffect.SaveFailed)
                return@launch
            }

            // 삭제는 되돌릴 수 없어 저장이 전부 끝난 뒤에만 한다
            if (!deletePending()) {
                updateState { copy(isLoading = false) }
                postSideEffect(SoloToppingArrangeEffect.DeleteFailed)
                return@launch
            }

            updateState { copy(isLoading = false) }
            postSideEffect(SoloToppingArrangeEffect.NavigateBack)
        }
    }

    private fun failToSaveUnexpectedly(throwable: Throwable) {
        viewModelLogger.e(throwable) { "혼자 공간 재편집을 저장하지 못했다" }
        updateState { copy(isLoading = false) }
        postSideEffect(SoloToppingArrangeEffect.SaveFailed)
    }

    /** 변형은 한 번에 저장해 부분 성공이 없다. 실패하면 실었던 토핑·텍스트 전부를 대상으로 남긴다 */
    private suspend fun saveDirty(): ToppingSaveFailures {
        val changes = state.value.dirtyChanges(storedToppings = storedToppings, storedTexts = storedTexts)

        val isTransformSaved = (changes.transformChanged.isEmpty() && changes.textUpdates.isEmpty()) ||
            updateSoloTransforms(
                updates = changes.transformChanged.map { it.toTransformUpdate() },
                textUpdates = changes.textUpdates,
            ).onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 토핑 변형을 저장하지 못했다" } }
                .isSuccess
        val borderFailures = changes.borderChanged
            .filterNot { topping -> saveBorder(topping) }
            .mapTo(mutableSetOf()) { it.parfaitImageId }

        if (isTransformSaved) return ToppingSaveFailures(toppingIds = borderFailures)
        return ToppingSaveFailures(
            toppingIds = changes.transformChanged.mapTo(borderFailures) { it.parfaitImageId },
            textIds = changes.textUpdates.mapTo(mutableSetOf()) { it.parfaitTextId.value },
        )
    }

    private suspend fun saveBorder(topping: EditableTopping): Boolean = updateSoloToppingBorder(
        id = ParfaitImageId(topping.parfaitImageId),
        border = topping.border.toToppingBorder(),
    ).onFailure { throwable ->
        viewModelLogger.e(throwable) { "혼자 공간 토핑 테두리를 저장하지 못했다 - ${topping.parfaitImageId}" }
    }.isSuccess

    /** @return 대기 중인 삭제가 모두 반영됐는가. 지운 것만 대기에서 툼스톤으로 옮긴다 */
    private suspend fun deletePending(): Boolean {
        val pendingIds = state.value.pendingDeleteToppingIds
        val pendingTextIds = state.value.pendingDeleteTextIds

        val goneIds = pendingIds.filterTo(mutableSetOf()) { id ->
            deleteSoloTopping(ParfaitImageId(id))
                .onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 토핑을 지우지 못했다 - $id" } }
                .isSuccess
        }
        val goneTextIds = pendingTextIds.filterTo(mutableSetOf()) { id ->
            deleteSoloText(ParfaitTextId(id))
                .onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 텍스트를 지우지 못했다 - $id" } }
                .isSuccess
        }

        updateState {
            copy(
                pendingDeleteToppingIds = pendingDeleteToppingIds - goneIds,
                deletedToppingIds = deletedToppingIds + goneIds,
                pendingDeleteTextIds = pendingDeleteTextIds - goneTextIds,
                deletedTextIds = deletedTextIds + goneTextIds,
            )
        }

        return goneIds.size == pendingIds.size && goneTextIds.size == pendingTextIds.size
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("initialToppingId") initialToppingId: Long,
            @Assisted("initialIsText") initialIsText: Boolean,
        ): SoloToppingArrangeViewModel
    }

    private companion object {
        /** 연타로 두 번 보내면 같은 삭제·저장이 겹친다 */
        const val CONFIRM_KEY = "solo-topping-arrange-confirm"
    }
}
