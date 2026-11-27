package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteTextToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingsUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingSaveFailures
import com.teamyg.parfait.feature.groups.canvas.impl.util.DEFAULT_TOPPING_BORDER_WIDTH_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.ignoresToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.resolvePanelBorderWidthDp
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableText
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.dirtyChanges
import com.teamyg.parfait.feature.groups.canvas.impl.util.focusedCenterGuideStep
import com.teamyg.parfait.feature.groups.canvas.impl.util.mergeTexts
import com.teamyg.parfait.feature.groups.canvas.impl.util.mergeToppings
import com.teamyg.parfait.feature.groups.canvas.impl.util.toTransformUpdate
import com.teamyg.parfait.feature.groups.canvas.impl.util.withBorderColorSelected
import com.teamyg.parfait.feature.groups.canvas.impl.util.withBorderWidthChanged
import com.teamyg.parfait.feature.groups.canvas.impl.util.withCanvasBackground
import com.teamyg.parfait.feature.groups.canvas.impl.util.withDeleteFocusedClicked
import com.teamyg.parfait.feature.groups.canvas.impl.util.withEmptyCanvasClicked
import com.teamyg.parfait.feature.groups.canvas.impl.util.withFocus
import com.teamyg.parfait.feature.groups.canvas.impl.util.withFocusedTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.withInitialFocusIfNeeded
import com.teamyg.parfait.feature.groups.canvas.impl.util.withTransformGestureEnded
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class CanvasToppingArrangeUiState(
    /** [backgroundImageUrl] 이 있으면 그쪽이 우선이고, 이 색은 이미지가 없을 때만 그려진다 */
    val backgroundColor: Color = YGAtomicColors.Gray.White,
    val backgroundImageUrl: String? = null,
    val toppings: List<EditableTopping> = emptyList(),
    val texts: List<EditableText> = emptyList(),
    val focusedKey: CanvasToppingKey? = null,
    /** 포커스된 토핑을 캔버스 중앙 가이드로 끌 때, 손가락이 실제로 가리키는 비율 좌표. */
    val rawFocusedPositionX: Float? = null,
    val rawFocusedPositionY: Float? = null,
    val isCenterGuideXVisible: Boolean = false,
    val isCenterGuideYVisible: Boolean = false,
    /** 각도 가이드 스냅 전, 손가락이 실제로 누적시킨 원값(도). [rawFocusedPositionX]/[Y] 와 같은 이유로 따로 둔다 */
    val rawFocusedRotationDegrees: Float? = null,
    /** 실제 회전이 있었을 때만 각도 가이드·스냅을 켠다. */
    val hasFocusedRotated: Boolean = false,
    val isAngleGuideVisible: Boolean = false,
    /** true 면 0/180도(가로선), false 면 90/270도(세로선) */
    val isAngleGuideHorizontal: Boolean = false,
    /** 테두리가 없는 토핑에 두를 굵기. 색을 고르는 순간 이 굵기로 테두리가 생긴다 */
    val pendingBorderWidthDp: Float = DEFAULT_TOPPING_BORDER_WIDTH_DP,
    val isBorderPanelOpen: Boolean = false,
    val showQuitDialog: Boolean = false,
    val dirtyToppingIds: Set<Long> = emptySet(),
    /** 화면에서 지웠지만 서버에는 아직 있는 토핑. 확인 때 DELETE 로 나간다 */
    val pendingDeleteToppingIds: Set<Long> = emptySet(),
    /** 서버에서 지운 토핑의 툼스톤 — 그 전에 출발한 갱신 응답이 뒤늦게 도착하면 지운 토핑이 되살아난다 */
    val deletedToppingIds: Set<Long> = emptySet(),
    /** 이미지 쪽 세 집합과 같은 규칙이다 */
    val dirtyTextIds: Set<Long> = emptySet(),
    val pendingDeleteTextIds: Set<Long> = emptySet(),
    val deletedTextIds: Set<Long> = emptySet(),
    /**
     * 화면의 덮개가 막는 것은 포인터 입력뿐이고 시스템 뒤로가기는 덮개를 거치지 않아, 그쪽은
     * ViewModel 이 이 값을 보고 직접 막는다.
     */
    val isLoading: Boolean = false,
) : UiState {
    val focusedTopping: EditableTopping?
        get() {
            val key = focusedKey as? CanvasToppingKey.Image ?: return null
            return toppings.firstOrNull { it.parfaitImageId == key.id }
        }

    val focusedText: EditableText?
        get() {
            val key = focusedKey as? CanvasToppingKey.Text ?: return null
            return texts.firstOrNull { it.parfaitTextId == key.id }
        }

    val panelBorderColorArgb: Int? get() = focusedTopping?.border?.colorArgb

    val panelBorderWidthDp: Float get() = resolvePanelBorderWidthDp(focusedTopping?.border, pendingBorderWidthDp)

    val canOpenBorderPanel: Boolean get() = focusedKey is CanvasToppingKey.Image

    val hasUnsavedChanges: Boolean
        get() = dirtyToppingIds.isNotEmpty() ||
            pendingDeleteToppingIds.isNotEmpty() ||
            dirtyTextIds.isNotEmpty() ||
            pendingDeleteTextIds.isNotEmpty()
}

sealed interface CanvasToppingArrangeIntent : UiIntent {
    data class OnClickTopping(
        val topping: EditableTopping,
    ) : CanvasToppingArrangeIntent

    data class OnClickText(
        val text: EditableText,
    ) : CanvasToppingArrangeIntent

    data object OnClickEmptyCanvas : CanvasToppingArrangeIntent

    data object OnToggleBorderPanel : CanvasToppingArrangeIntent

    data object OnDismissBorderPanel : CanvasToppingArrangeIntent

    /** @param colorArgb `null` 은 테두리 없음 */
    data class OnSelectBorderColor(
        val colorArgb: Int?,
    ) : CanvasToppingArrangeIntent

    data class OnChangeBorderWidth(
        val widthDp: Float,
    ) : CanvasToppingArrangeIntent

    /**
     * `panX`/`panY` 는 px 가 아니라 Canvas-Area 대비 비율이다.
     */
    data class OnToppingTransform(
        val panX: Float,
        val panY: Float,
        val zoom: Float,
        val rotationDelta: Float,
        val thresholdRatioX: Float,
        val thresholdRatioY: Float,
    ) : CanvasToppingArrangeIntent

    /** 이동 제스처(드래그·핀치)가 끝났다. 중앙 가이드를 끈다 */
    data object OnToppingTransformGestureEnd : CanvasToppingArrangeIntent

    data object OnClickDeleteToppingButton : CanvasToppingArrangeIntent

    data object OnClickClose : CanvasToppingArrangeIntent

    data object OnSystemBack : CanvasToppingArrangeIntent

    data object OnQuitDialogConfirm : CanvasToppingArrangeIntent

    data object OnQuitDialogCancel : CanvasToppingArrangeIntent

    data object OnClickConfirm : CanvasToppingArrangeIntent
}

sealed interface CanvasToppingArrangeEffect : UiSideEffect {
    data object NavigateBack : CanvasToppingArrangeEffect

    data object ShowOthersToppingNotEditable : CanvasToppingArrangeEffect

    data class ShowError(
        val error: CanvasToppingArrangeError,
    ) : CanvasToppingArrangeEffect

    /** 토핑이 캔버스 중앙·각도 가이드에 막 스냅됐다. 햅틱을 한 번 울린다 */
    data object TriggerSnapHaptic : CanvasToppingArrangeEffect
}

@HiltViewModel(assistedFactory = CanvasToppingArrangeViewModel.Factory::class)
class CanvasToppingArrangeViewModel
@AssistedInject
constructor(
    @Assisted("groupId") groupIdValue: Long,
    @Assisted("parfaitId") parfaitIdValue: Long,
    @Assisted("initialToppingId") private val initialToppingId: Long,
    @Assisted("initialIsText") private val initialIsText: Boolean,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val refreshTodayParfaitDetailUseCase: RefreshTodayParfaitDetailUseCase,
    private val deleteToppingUseCase: DeleteToppingUseCase,
    private val deleteTextToppingUseCase: DeleteTextToppingUseCase,
    private val updateToppingsUseCase: UpdateToppingsUseCase,
    private val updateToppingBorderUseCase: UpdateToppingBorderUseCase,
    private val analyticsLogger: AnalyticsLogger,
) : BaseViewModel<CanvasToppingArrangeUiState, CanvasToppingArrangeIntent, CanvasToppingArrangeEffect>(
    initialState = CanvasToppingArrangeUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    /**
     * 저장할 대상 캔버스. 캔버스 메인이 열어 준 오늘의 캔버스로 시작하지만, 최초 방출이 다른
     * parfaitId 를 주면 그쪽으로 옮긴다([hasSeededFromCanvas] 참고) — 화면에 그려진 토핑과
     * 저장 대상이 갈라지는 편이 더 나쁘다.
     *
     * 그 뒤 날이 바뀌어 조회가 다른 날의 캔버스를 주는 경우는 여기서 옮기지 않는다 — 이 화면의
     * 시간 축이 닫을 몫이다(`specs/2026-08-27-canvas-today-ssot-polling.md` 「하루 경계」).
     */
    private var parfaitId = ParfaitId(parfaitIdValue)

    /**
     * 최초 방출에만 포커스를 시딩하고, 편집 대상([parfaitId])도 최초 방출로만 정한다 — 이후
     * 방출이 사용자의 포커스를 덮거나, 화면에 그려진 토핑과 다른 캔버스로 저장 대상을 바꾸면
     * 안 된다.
     */
    private var hasSeededFromCanvas = false

    /**
     * 서버가 마지막으로 준 그대로의 토핑. 확인 때 손댄 토핑의 **어느 축**이 바뀌었는지 가리는 데만
     * 쓴다 — 화면 렌더링에는 [CanvasToppingArrangeUiState.toppings] 를 본다.
     */
    private var serverToppings: List<EditableTopping> = emptyList()

    private var serverTexts: List<EditableText> = emptyList()

    init {
        viewModelLogger.i { "CanvasToppingArrangeViewModel::init" }
        observeCanvas()
    }

    /**
     * 화면이 보이는 동안만 오늘 캔버스를 구독한다. 갱신은 구독 시작 즉시, 이후 주기마다
     * 폴러가 맡는다(`adr/0029-canvas-today-ssot-polling.md`) — 여기서 따로 조회를 걸지 않는다.
     */
    private fun observeCanvas() {
        launchWhileSubscribed(source = { getTodayParfaitFlowUseCase(groupId) }) { canvas ->
            if (canvas == null) return@launchWhileSubscribed

            if (hasSeededFromCanvas.not() && canvas.parfaitId != parfaitId) {
                viewModelLogger.e {
                    "편집을 연 캔버스와 조회 결과가 다르다 — 조회 쪽으로 옮긴다" +
                        " (열린 것: ${parfaitId.value}, 받은 것: ${canvas.parfaitId.value})"
                }
                parfaitId = canvas.parfaitId
            }

            val incoming = canvas.toppings
                .sortedBy { topping -> topping.transform.positionZ }
                .map { topping -> topping.toEditableTopping() }

            val incomingTexts = canvas.texts.map { text -> text.toEditableText() }

            serverToppings = incoming
            serverTexts = incomingTexts
            updateState {
                withCanvasBackground(canvas.background)
                    .withInitialFocusIfNeeded(hasSeededFromCanvas, initialToppingId, initialIsText)
                    .mergeToppings(incoming)
                    .mergeTexts(incomingTexts)
            }
            hasSeededFromCanvas = true
        }
    }

    override fun processIntent(intent: CanvasToppingArrangeIntent) {
        when (intent) {
            is CanvasToppingArrangeIntent.OnClickTopping -> handleOnClickTopping(intent)

            is CanvasToppingArrangeIntent.OnClickText -> handleOnClickText(intent)

            CanvasToppingArrangeIntent.OnClickEmptyCanvas -> handleOnClickEmptyCanvas()

            CanvasToppingArrangeIntent.OnToggleBorderPanel -> handleOnToggleBorderPanel()

            CanvasToppingArrangeIntent.OnDismissBorderPanel -> updateState { copy(isBorderPanelOpen = false) }

            is CanvasToppingArrangeIntent.OnSelectBorderColor -> handleOnSelectBorderColor(intent)

            is CanvasToppingArrangeIntent.OnChangeBorderWidth -> handleOnChangeBorderWidth(intent)

            is CanvasToppingArrangeIntent.OnToppingTransform -> handleOnToppingTransform(intent)

            CanvasToppingArrangeIntent.OnToppingTransformGestureEnd -> updateState { withTransformGestureEnded() }

            CanvasToppingArrangeIntent.OnClickDeleteToppingButton -> handleOnClickDeleteToppingButton()

            CanvasToppingArrangeIntent.OnClickClose -> requestQuit()

            CanvasToppingArrangeIntent.OnSystemBack -> handleOnSystemBack()

            CanvasToppingArrangeIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                postSideEffect(effect = CanvasToppingArrangeEffect.NavigateBack)
            }

            CanvasToppingArrangeIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }

            CanvasToppingArrangeIntent.OnClickConfirm -> handleOnClickConfirm()
        }
    }

    /**
     * 패널이 열려 있는 동안 캔버스 위 탭은 패널을 닫는 데만 쓴다. 닫는 탭이 포커스까지 옮기면
     * 사용자는 패널을 닫으려다 편집 대상이 바뀐 것을 보게 된다.
     */
    private fun handleOnClickTopping(intent: CanvasToppingArrangeIntent.OnClickTopping) {
        val current = state.value
        val topping = intent.topping

        when {
            current.isBorderPanelOpen -> updateState { copy(isBorderPanelOpen = false) }

            !topping.isMine -> postSideEffect(effect = CanvasToppingArrangeEffect.ShowOthersToppingNotEditable)

            current.focusedKey == CanvasToppingKey.Image(topping.parfaitImageId) ->
                updateState { copy(isBorderPanelOpen = true) }

            else -> updateState { withFocus(CanvasToppingKey.Image(topping.parfaitImageId)) }
        }
    }

    /** 패널 규칙은 [handleOnClickTopping] 과 같다. 텍스트는 내용을 고치지 않아 포커스된 것을 다시 탭해도 그대로다 */
    private fun handleOnClickText(intent: CanvasToppingArrangeIntent.OnClickText) {
        val current = state.value
        val text = intent.text

        when {
            current.isBorderPanelOpen -> updateState { copy(isBorderPanelOpen = false) }
            !text.isMine -> postSideEffect(effect = CanvasToppingArrangeEffect.ShowOthersToppingNotEditable)
            current.focusedKey == CanvasToppingKey.Text(text.parfaitTextId) -> Unit
            else -> updateState { withFocus(CanvasToppingKey.Text(text.parfaitTextId)) }
        }
    }

    private fun handleOnClickEmptyCanvas() {
        updateState { withEmptyCanvasClicked() }
    }

    private fun handleOnClickDeleteToppingButton() {
        updateState { withDeleteFocusedClicked() }
    }

    /** 닫혀 있다가 열릴 때만 "탭"으로 센다 — 토글이라 닫는 쪽까지 세면 과다 집계된다 */
    private fun handleOnToggleBorderPanel() {
        val isOpening = state.value.canOpenBorderPanel && !state.value.isBorderPanelOpen
        updateState {
            if (canOpenBorderPanel) copy(isBorderPanelOpen = !isBorderPanelOpen) else this
        }
        if (isOpening) {
            analyticsLogger.logEvent(
                AnalyticsEvent(
                    type = AnalyticsEventType.CANVAS_EDIT_BORDER_TAPPED,
                ),
            )
        }
    }

    private fun handleOnSystemBack() {
        val current = state.value

        when (toppingPanelBackAction(current.isLoading, current.isBorderPanelOpen)) {
            ToppingPanelBackAction.Ignore -> Unit
            ToppingPanelBackAction.ClosePanel -> updateState { copy(isBorderPanelOpen = false) }
            ToppingPanelBackAction.NavigateBack -> postSideEffect(effect = CanvasToppingArrangeEffect.NavigateBack)
        }
    }

    private fun requestQuit() {
        val current = state.value
        if (current.isLoading) return

        if (current.hasUnsavedChanges) {
            updateState { copy(showQuitDialog = true) }
        } else {
            // 바꾼 게 없어 그만두기 팝업 없이 바로 나가는 경로 — X로 나간 걸로 센다
            logClosed(action = "close")
            postSideEffect(effect = CanvasToppingArrangeEffect.NavigateBack)
        }
    }

    private fun logClosed(action: String) {
        analyticsLogger.logEvent(
            AnalyticsEvent(
                type = AnalyticsEventType.CANVAS_TOPPING_CLOSED_UNCHANGED,
                params = mapOf("action" to action),
            ),
        )
    }

    private fun handleOnSelectBorderColor(intent: CanvasToppingArrangeIntent.OnSelectBorderColor) {
        updateState { withBorderColorSelected(intent.colorArgb) }
    }

    private fun handleOnChangeBorderWidth(intent: CanvasToppingArrangeIntent.OnChangeBorderWidth) {
        updateState { withBorderWidthChanged(intent.widthDp) }
    }

    /**
     * 저장·삭제는 이미 끝난 뒤다. 갱신이 터져도 그 실패로 알리지 않는다 — 캔버스 메인이 다음
     * 조회에서 따라잡는다.
     */
    private suspend fun refreshTodayCanvas() {
        runSuspendCatching { refreshTodayParfaitDetailUseCase(groupId = groupId, parfaitId = parfaitId) }
            .onFailure { throwable ->
                viewModelLogger.e(throwable) { "오늘 캔버스를 다시 받지 못했다 - parfaitId: ${parfaitId.value}" }
            }
    }

    private fun handleOnToppingTransform(intent: CanvasToppingArrangeIntent.OnToppingTransform) {
        val current = state.value
        if (ignoresToppingTransform(current.isBorderPanelOpen)) return
        val step = current.focusedCenterGuideStep(intent) ?: return

        updateState { withFocusedTransform(step, intent.zoom) }
        if (step.justSnapped) postSideEffect(effect = CanvasToppingArrangeEffect.TriggerSnapHaptic)
    }

    /**
     * 저장이 끝나고 갱신까지 마친 뒤에야 화면을 넘긴다 — 먼저 나가면 캔버스 메인이 저장되지 않은
     * 값을 그린 채로 서 있다가 다음 조회에서 슬그머니 되돌아간다.
     */
    private fun handleOnClickConfirm() {
        val hadNoChanges = !state.value.hasUnsavedChanges

        launch(key = CONFIRM_KEY, onError = ::failToSaveUnexpectedly) {
            updateState { copy(isLoading = true) }

            val failed = updateDirtyToppings()
            // 보낸 것만 대상에서 뺀다 — 여기서 통째 비우면 다시 누른 확인이 못 보낸 토핑을 건너뛴다
            updateState { copy(dirtyToppingIds = failed.toppingIds, dirtyTextIds = failed.textIds) }

            // 삭제는 되돌릴 수 없어 저장이 전부 끝난 뒤에만 보낸다
            val isSaved = failed.isEmpty() && deletePendingToppings()

            if (!isSaved) {
                // 닫으면 사용자는 방금 옮긴 토핑이 되돌아간 캔버스를 보게 된다
                updateState { copy(isLoading = false) }
                postSideEffect(
                    effect = CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                )
                return@launch
            }

            // 되감기 전에 기다린다 — 먼저 나가면 라우트가 되감기며 viewModelScope 가
            // 취소돼 갱신이 끊긴다
            refreshTodayCanvas()

            updateState { copy(isLoading = false) }
            if (hadNoChanges) logClosed(action = "done")
            postSideEffect(effect = CanvasToppingArrangeEffect.NavigateBack)
        }
    }

    private fun failToSaveUnexpectedly(throwable: Throwable) {
        viewModelLogger.e(throwable) { "편집을 저장하지 못했다 - parfaitId: ${parfaitId.value}" }
        updateState { copy(isLoading = false) }
        postSideEffect(
            effect = CanvasToppingArrangeEffect.ShowError(
                throwable.toCanvasToppingArrangeError(unknown = CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
            ),
        )
    }

    /** 축으로 가르는 것은 서버 API 가 갈라져 있어서다 — 변형만 한 요청에 접힌다. */
    private suspend fun updateDirtyToppings(): ToppingSaveFailures = coroutineScope {
        val (transformChanged, textUpdates, borderChanged) =
            state.value.dirtyChanges(storedToppings = serverToppings, storedTexts = serverTexts)

        val transformFailures = saveTransforms(transformChanged, textUpdates)
        val borderFailures = borderChanged
            .map { topping -> async { topping.parfaitImageId.takeIf { saveBorder(topping).not() } } }
            .awaitAll()
            .filterNotNull()

        ToppingSaveFailures(
            toppingIds = transformFailures.toppingIds + borderFailures,
            textIds = transformFailures.textIds,
        )
    }

    /** @return 대기 중인 삭제가 모두 서버에 반영됐는가. */
    private suspend fun deletePendingToppings(): Boolean = coroutineScope {
        val pendingIds = state.value.pendingDeleteToppingIds
        val pendingTextIds = state.value.pendingDeleteTextIds
        val deletedIdsAsync = pendingIds.map { toppingId -> async { toppingId.takeIf { deleteTopping(it) } } }
        val deletedTextIdsAsync = pendingTextIds.map { textId -> async { textId.takeIf { deleteText(it) } } }
        val deletedIds = deletedIdsAsync.awaitAll().filterNotNull().toSet()
        val deletedTextIds = deletedTextIdsAsync.awaitAll().filterNotNull().toSet()

        // 지운 것만 옮긴다 — 다시 누른 확인이 못 지운 토핑만 보낸다
        updateState {
            copy(
                pendingDeleteToppingIds = pendingDeleteToppingIds - deletedIds,
                deletedToppingIds = deletedToppingIds + deletedIds,
            ).markTextsGone(deletedTextIds)
        }

        deletedIds.size == pendingIds.size && deletedTextIds.size == pendingTextIds.size
    }

    private fun CanvasToppingArrangeUiState.markTextsGone(textIds: Set<Long>): CanvasToppingArrangeUiState = copy(
        pendingDeleteTextIds = pendingDeleteTextIds - textIds,
        deletedTextIds = deletedTextIds + textIds,
    )

    private suspend fun deleteText(textId: Long): Boolean =
        deleteTextToppingUseCase(groupId, parfaitId, ParfaitTextId(textId)).isDeletedOrGone(
            notFoundCode = ServerErrorCode.ParfaitText.PARFAIT_TEXT_NOT_FOUND,
        ) { "텍스트를 지우지 못했다 - parfaitTextId: $textId" }

    private suspend fun deleteTopping(toppingId: Long): Boolean =
        deleteToppingUseCase(groupId, parfaitId, ParfaitImageId(toppingId)).isDeletedOrGone(
            notFoundCode = ServerErrorCode.ParfaitImage.PARFAIT_IMAGE_NOT_FOUND,
        ) { "토핑을 지우지 못했다 - parfaitImageId: $toppingId" }

    /** DELETE 가 서버에 닿고 응답만 잃었다면 다시 보낸 요청은 404 다 — 그것은 지워진 것으로 친다 */
    private fun Result<Unit>.isDeletedOrGone(
        notFoundCode: String,
        failureMessage: () -> String,
    ): Boolean = fold(
        onSuccess = { true },
        onFailure = { throwable ->
            val isAlreadyGone = throwable is AppError.Server && throwable.code == notFoundCode

            if (!isAlreadyGone) viewModelLogger.e(throwable, message = failureMessage)
            isAlreadyGone
        },
    )

    /**
     * 일괄이라 부분 성공이 없어서, 실패하면 보낸 토핑 전부를 대상으로 남긴다. 되풀이되는 실패가
     * 섞이면 나머지까지 계속 막히는 것을 감수한 설계다 —
     * `specs/2026-08-31-topping-batch-update-and-past-canvas-status.md` 「주의 / 열린 질문」 절.
     */
    private suspend fun saveTransforms(
        toppings: List<EditableTopping>,
        textUpdates: List<TextToppingTransformUpdate>,
    ): ToppingSaveFailures {
        if (toppings.isEmpty() && textUpdates.isEmpty()) return ToppingSaveFailures()

        return updateToppingsUseCase(
            groupId = groupId,
            parfaitId = parfaitId,
            updates = toppings.map { it.toTransformUpdate() },
            textUpdates = textUpdates,
        ).fold(
            onSuccess = { ToppingSaveFailures() },
            onFailure = { throwable ->
                viewModelLogger.e(throwable) {
                    "토핑 변형을 저장하지 못했다 - 이미지 ${toppings.map { it.parfaitImageId }}" +
                        ", 텍스트 ${textUpdates.map { it.parfaitTextId.value }}"
                }
                ToppingSaveFailures(
                    toppingIds = toppings.mapTo(mutableSetOf()) { it.parfaitImageId },
                    textIds = textUpdates.mapTo(mutableSetOf()) { it.parfaitTextId.value },
                )
            },
        )
    }

    private suspend fun saveBorder(topping: EditableTopping): Boolean = updateToppingBorderUseCase(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = ParfaitImageId(topping.parfaitImageId),
        border = topping.border.toToppingBorder(),
    ).onFailure { throwable ->
        viewModelLogger.e(throwable) { "토핑 테두리를 저장하지 못했다 - ${topping.parfaitImageId}" }
    }.isSuccess

    /**
     * @param unknown 갈래를 가리지 못했을 때 쓸 값. 무엇을 하다 실패했는지는 부르는 쪽만 안다.
     */
    private fun Throwable.toCanvasToppingArrangeError(unknown: CanvasToppingArrangeError): CanvasToppingArrangeError =
        when (this) {
            is AppError.Network -> CanvasToppingArrangeError.NETWORK
            else -> unknown
        }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupId: Long,
            @Assisted("parfaitId") parfaitId: Long,
            @Assisted("initialToppingId") initialToppingId: Long,
            @Assisted("initialIsText") initialIsText: Boolean,
        ): CanvasToppingArrangeViewModel
    }

    private companion object {
        const val CONFIRM_KEY = "confirm"
    }
}
