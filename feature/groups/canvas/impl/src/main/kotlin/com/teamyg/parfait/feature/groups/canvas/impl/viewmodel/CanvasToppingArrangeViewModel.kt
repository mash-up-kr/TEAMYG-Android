package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingsUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.DEFAULT_TOPPING_BORDER_WIDTH_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.clampPanelBorderWidthDp
import com.teamyg.parfait.feature.groups.canvas.impl.util.ignoresToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.util.panelBorderForColor
import com.teamyg.parfait.feature.groups.canvas.impl.util.resolvePanelBorderWidthDp
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingPanelBackAction
import com.teamyg.parfait.feature.groups.canvas.impl.util.withPanelWidth
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
    val focusedToppingId: Long? = null,
    /** 테두리가 없는 토핑에 두를 굵기. 색을 고르는 순간 이 굵기로 테두리가 생긴다 */
    val pendingBorderWidthDp: Float = DEFAULT_TOPPING_BORDER_WIDTH_DP,
    val isBorderPanelOpen: Boolean = false,
    val showQuitDialog: Boolean = false,
    val dirtyToppingIds: Set<Long> = emptySet(),
    /** 화면에서 지웠지만 서버에는 아직 있는 토핑. 확인 때 DELETE 로 나간다 */
    val pendingDeleteToppingIds: Set<Long> = emptySet(),
    /** 서버에서 지운 토핑의 툼스톤 — 그 전에 출발한 갱신 응답이 뒤늦게 도착하면 지운 토핑이 되살아난다 */
    val deletedToppingIds: Set<Long> = emptySet(),
    /**
     * 화면의 덮개가 막는 것은 포인터 입력뿐이고 시스템 뒤로가기는 덮개를 거치지 않아, 그쪽은
     * ViewModel 이 이 값을 보고 직접 막는다.
     */
    val isLoading: Boolean = false,
) : UiState {
    val focusedTopping: EditableTopping?
        get() = toppings.firstOrNull { it.parfaitImageId == focusedToppingId }

    val panelBorderColorArgb: Int? get() = focusedTopping?.border?.colorArgb

    val panelBorderWidthDp: Float get() = resolvePanelBorderWidthDp(focusedTopping?.border, pendingBorderWidthDp)

    val canOpenBorderPanel: Boolean get() = focusedToppingId != null

    val hasUnsavedChanges: Boolean get() = dirtyToppingIds.isNotEmpty() || pendingDeleteToppingIds.isNotEmpty()
}

sealed interface CanvasToppingArrangeIntent : UiIntent {
    data class OnClickTopping(
        val topping: EditableTopping,
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

    /** `panX`/`panY` 는 px 가 아니라 Canvas-Area 대비 비율이다. */
    data class OnToppingTransform(
        val panX: Float,
        val panY: Float,
        val zoom: Float,
        val rotationDelta: Float,
    ) : CanvasToppingArrangeIntent

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
}

@HiltViewModel(assistedFactory = CanvasToppingArrangeViewModel.Factory::class)
class CanvasToppingArrangeViewModel
@AssistedInject
constructor(
    @Assisted("groupId") groupIdValue: Long,
    @Assisted("parfaitId") parfaitIdValue: Long,
    @Assisted("initialToppingId") private val initialToppingId: Long,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val refreshTodayParfaitDetailUseCase: RefreshTodayParfaitDetailUseCase,
    private val deleteToppingUseCase: DeleteToppingUseCase,
    private val updateToppingsUseCase: UpdateToppingsUseCase,
    private val updateToppingBorderUseCase: UpdateToppingBorderUseCase,
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

            serverToppings = incoming
            updateState { withCanvas(canvas).mergeToppings(incoming) }
            hasSeededFromCanvas = true
        }
    }

    /** 이 화면은 배경을 고르지 않으므로 방출마다 서버 값을 따른다. 포커스만 최초 방출에 시딩한다. */
    private fun CanvasToppingArrangeUiState.withCanvas(canvas: CanvasVO): CanvasToppingArrangeUiState = copy(
        backgroundColor = (canvas.background as? CanvasBackground.Color)
            ?.value
            ?.toColorOrNull()
            ?: backgroundColor,
        backgroundImageUrl = (canvas.background as? CanvasBackground.Image)?.url,
        focusedToppingId = if (hasSeededFromCanvas) focusedToppingId else initialToppingId,
    )

    /**
     * 최초 방출도 예외가 아니다 — 그때는 두 집합이 비어 있어 결과가 통째 대입과 같아진다.
     * 화면은 그 방출이 폴링에서 왔는지 강제 갱신에서 왔는지 구분하지 않는다.
     */
    private fun CanvasToppingArrangeUiState.mergeToppings(
        incoming: List<EditableTopping>,
    ): CanvasToppingArrangeUiState {
        val incomingIds = incoming.mapTo(mutableSetOf()) { it.parfaitImageId }
        val localById = toppings.associateBy { it.parfaitImageId }

        val removedIds = pendingDeleteToppingIds + deletedToppingIds

        val merged = incoming
            .filterNot { it.parfaitImageId in removedIds }
            .map { server ->
                if (server.parfaitImageId in dirtyToppingIds) localById[server.parfaitImageId] ?: server else server
            }
        val remainingFocusId = focusedToppingId?.takeIf { it in incomingIds && it !in removedIds }

        return copy(
            toppings = merged,
            // 서버 목록에서 사라진 것은 세 집합에서도 뺀다 — 없는 토핑에 PATCH·DELETE 를 보낼 수 없고,
            // 툼스톤도 제 역할을 다했다
            dirtyToppingIds = dirtyToppingIds intersect incomingIds,
            pendingDeleteToppingIds = pendingDeleteToppingIds intersect incomingIds,
            deletedToppingIds = deletedToppingIds intersect incomingIds,
            focusedToppingId = remainingFocusId,
            // 패널은 포커스된 토핑의 것이라 포커스와 함께 닫힌다
            isBorderPanelOpen = isBorderPanelOpen && remainingFocusId != null,
        )
    }

    override fun processIntent(intent: CanvasToppingArrangeIntent) {
        when (intent) {
            is CanvasToppingArrangeIntent.OnClickTopping -> handleOnClickTopping(intent)

            CanvasToppingArrangeIntent.OnClickEmptyCanvas -> handleOnClickEmptyCanvas()

            CanvasToppingArrangeIntent.OnToggleBorderPanel -> updateState {
                if (focusedToppingId == null) this else copy(isBorderPanelOpen = !isBorderPanelOpen)
            }

            CanvasToppingArrangeIntent.OnDismissBorderPanel -> updateState { copy(isBorderPanelOpen = false) }

            is CanvasToppingArrangeIntent.OnSelectBorderColor -> handleOnSelectBorderColor(intent)

            is CanvasToppingArrangeIntent.OnChangeBorderWidth -> handleOnChangeBorderWidth(intent)

            is CanvasToppingArrangeIntent.OnToppingTransform -> handleOnToppingTransform(intent)

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
            current.focusedToppingId == topping.parfaitImageId -> updateState { copy(isBorderPanelOpen = true) }
            else -> updateState { copy(focusedToppingId = topping.parfaitImageId) }
        }
    }

    private fun handleOnClickEmptyCanvas() {
        updateState {
            if (isBorderPanelOpen) copy(isBorderPanelOpen = false) else copy(focusedToppingId = null)
        }
    }

    private fun handleOnClickDeleteToppingButton() {
        updateState {
            val focusedId = focusedToppingId

            when {
                isBorderPanelOpen -> copy(isBorderPanelOpen = false)

                focusedId == null -> this

                else -> copy(
                    toppings = toppings.filterNot { it.parfaitImageId == focusedId },
                    pendingDeleteToppingIds = pendingDeleteToppingIds + focusedId,
                    dirtyToppingIds = dirtyToppingIds - focusedId,
                    focusedToppingId = null,
                )
            }
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
            postSideEffect(effect = CanvasToppingArrangeEffect.NavigateBack)
        }
    }

    private fun handleOnSelectBorderColor(intent: CanvasToppingArrangeIntent.OnSelectBorderColor) {
        updateState {
            val focusedId = focusedToppingId ?: return@updateState this
            val border = panelBorderForColor(intent.colorArgb, panelBorderWidthDp)

            copy(
                toppings = toppings.map { topping ->
                    if (topping.parfaitImageId == focusedId) topping.copy(border = border) else topping
                },
            ).markDirty(focusedId)
        }
    }

    /**
     * 테두리가 없는 토핑은 굵기를 [CanvasToppingArrangeUiState.pendingBorderWidthDp] 에만 담는다 —
     * 바뀐 값이 없는 토핑을 dirty 로 만들면 확인이 서버에 같은 "테두리 없음"을 보낸다.
     */
    private fun handleOnChangeBorderWidth(intent: CanvasToppingArrangeIntent.OnChangeBorderWidth) {
        updateState {
            val focusedId = focusedToppingId ?: return@updateState this
            val widthDp = clampPanelBorderWidthDp(intent.widthDp)

            if (focusedTopping?.border == null) {
                copy(pendingBorderWidthDp = widthDp)
            } else {
                copy(
                    pendingBorderWidthDp = widthDp,
                    toppings = toppings.map { topping ->
                        if (topping.parfaitImageId == focusedId) {
                            topping.copy(border = topping.border.withPanelWidth(widthDp))
                        } else {
                            topping
                        }
                    },
                ).markDirty(focusedId)
            }
        }
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
        updateState {
            if (ignoresToppingTransform(isBorderPanelOpen)) return@updateState this
            val focusedId = focusedToppingId ?: return@updateState this

            applyToppingTransform(focusedId) { topping ->
                topping.copy(
                    positionX = topping.positionX + intent.panX,
                    positionY = topping.positionY + intent.panY,
                    scale = (topping.scale * intent.zoom).coerceAtLeast(TOPPING_MIN_SCALE),
                    rotationDegrees = topping.rotationDegrees + intent.rotationDelta,
                )
            }
        }
    }

    /**
     * 캔버스 밖으로 나가는 부분은 화면에서 클립되어 안 보이므로 여기서 위치를 되돌리지 않는다.
     */
    private fun CanvasToppingArrangeUiState.applyToppingTransform(
        toppingId: Long,
        transform: (EditableTopping) -> EditableTopping,
    ): CanvasToppingArrangeUiState = copy(
        toppings = toppings.map { topping ->
            if (topping.parfaitImageId != toppingId) topping else transform(topping)
        },
    ).markDirty(toppingId)

    private fun CanvasToppingArrangeUiState.markDirty(toppingId: Long): CanvasToppingArrangeUiState =
        copy(dirtyToppingIds = dirtyToppingIds + toppingId)

    /**
     * 저장이 끝나고 갱신까지 마친 뒤에야 화면을 넘긴다 — 먼저 나가면 캔버스 메인이 저장되지 않은
     * 값을 그린 채로 서 있다가 다음 조회에서 슬그머니 되돌아간다.
     */
    private fun handleOnClickConfirm() {
        launch(key = CONFIRM_KEY, onError = ::failToSaveUnexpectedly) {
            updateState { copy(isLoading = true) }

            val failedToppingIds = updateDirtyToppings()
            // 보낸 것만 대상에서 뺀다 — 여기서 통째 비우면 다시 누른 확인이 못 보낸 토핑을 건너뛴다
            updateState { copy(dirtyToppingIds = failedToppingIds) }

            // 삭제는 되돌릴 수 없어 저장이 전부 끝난 뒤에만 보낸다
            val isSaved = failedToppingIds.isEmpty() && deletePendingToppings()

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

    /**
     * PATCH 대상은 지금 목록에 있으면서 손댄 토핑뿐이다. 대조를 dirty 안에서만 하는 것이 요점이다:
     * 목록 전체를 스냅샷과 견주면 갱신이 들여온 남의 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡힌다.
     *
     * 축으로 가르는 것은 서버 API 가 갈라져 있어서다 — 변형만 한 요청에 접힌다.
     *
     * @return 저장하지 못한 토핑의 id.
     */
    private suspend fun updateDirtyToppings(): Set<Long> = coroutineScope {
        val current = state.value
        val dirty = current.toppings.filter { it.parfaitImageId in current.dirtyToppingIds }

        val transformChanged = dirty.filter { it.hasTransformChange() }
        val borderChanged = dirty.filter { it.hasBorderChange() }

        val transformFailures = saveTransforms(transformChanged)
        val borderFailures = borderChanged
            .map { topping -> async { topping.parfaitImageId.takeIf { saveBorder(topping).not() } } }
            .awaitAll()
            .filterNotNull()

        transformFailures + borderFailures
    }

    /** @return 대기 중인 삭제가 모두 서버에 반영됐는가. */
    private suspend fun deletePendingToppings(): Boolean = coroutineScope {
        val pendingIds = state.value.pendingDeleteToppingIds
        val deletedIds = pendingIds
            .map { toppingId -> async { toppingId.takeIf { deleteTopping(it) } } }
            .awaitAll()
            .filterNotNull()
            .toSet()

        // 지운 것만 옮긴다 — 다시 누른 확인이 못 지운 토핑만 보낸다
        updateState {
            copy(
                pendingDeleteToppingIds = pendingDeleteToppingIds - deletedIds,
                deletedToppingIds = deletedToppingIds + deletedIds,
            )
        }

        deletedIds.size == pendingIds.size
    }

    /** DELETE 가 서버에 닿고 응답만 잃었다면 다시 보낸 요청은 404 다 — 그것은 지워진 것으로 친다 */
    private suspend fun deleteTopping(toppingId: Long): Boolean =
        deleteToppingUseCase(groupId, parfaitId, ParfaitImageId(toppingId)).fold(
            onSuccess = { true },
            onFailure = { throwable ->
                val isAlreadyGone = throwable is AppError.Server &&
                    throwable.code == ServerErrorCode.ParfaitImage.PARFAIT_IMAGE_NOT_FOUND

                if (!isAlreadyGone) {
                    viewModelLogger.e(throwable) { "토핑을 지우지 못했다 - parfaitImageId: $toppingId" }
                }
                isAlreadyGone
            },
        )

    /**
     * 일괄이라 부분 성공이 없어서, 실패하면 보낸 토핑 전부를 대상으로 남긴다. 되풀이되는 실패가
     * 섞이면 나머지까지 계속 막히는 것을 감수한 설계다 —
     * `specs/2026-08-31-topping-batch-update-and-past-canvas-status.md` 「주의 / 열린 질문」 절.
     *
     * @return 저장하지 못한 토핑의 id.
     */
    private suspend fun saveTransforms(toppings: List<EditableTopping>): Set<Long> {
        if (toppings.isEmpty()) return emptySet()

        return updateToppingsUseCase(
            groupId = groupId,
            parfaitId = parfaitId,
            updates = toppings.map { it.toTransformUpdate() },
        ).fold(
            onSuccess = { emptySet() },
            onFailure = { throwable ->
                viewModelLogger.e(throwable) { "토핑 변형을 저장하지 못했다 - ${toppings.map { it.parfaitImageId }}" }
                toppings.mapTo(mutableSetOf()) { it.parfaitImageId }
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

    /** 스냅샷에 없는 토핑을 바뀐 것으로 봐도 안전하다 — 갱신이 들여온 토핑은 dirty 에 안 들어온다. */
    private fun EditableTopping.hasTransformChange(): Boolean {
        val original = serverToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
        return positionX != original.positionX ||
            positionY != original.positionY ||
            scale != original.scale ||
            rotationDegrees != original.rotationDegrees
    }

    private fun EditableTopping.hasBorderChange(): Boolean {
        val original = serverToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
        return border != original.border
    }

    /** 겹침 순서는 안 보낸다 — 앱에 z 조작 경로가 없어 서버 값을 그대로 둔다. */
    private fun EditableTopping.toTransformUpdate(): ToppingTransformUpdate = ToppingTransformUpdate(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        positionX = positionX.toDouble(),
        positionY = positionY.toDouble(),
        scale = scale.toDouble(),
        rotation = rotationDegrees.toDouble(),
    )

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
        ): CanvasToppingArrangeViewModel
    }

    private companion object {
        const val CONFIRM_KEY = "confirm"
    }
}
