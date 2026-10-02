package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RequestTodayParfaitRefreshUseCase
import com.teamyg.parfait.domain.usecase.topping.AddToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.DEFAULT_TOPPING_BORDER_WIDTH_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BASE_LONG_SIDE_RATIO
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.util.isPermanentPlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingTransform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/** 스케일링된 토핑의 짧은 변이 이보다 작아지면, 그 변을 이 크기로 맞추도록 강제 상향한다 */
private val MIN_TOPPING_SHORT_SIDE = 48.dp

data class CanvasToppingPlaceUiState(
    /** 올릴 알맹이의 파일 시스템 절대경로. `file://` uri 가 아니다 */
    val toppingImagePath: String? = null,
    val toppingSourceLongSide: SourceLongSide? = null,
    val border: ToppingBorderStyle? = null,
    /** 색을 고르기 전에 슬라이더로 맞춰 둔 굵기. 색을 고르는 순간 이 굵기로 테두리가 생긴다 */
    val pendingBorderWidthDp: Float = DEFAULT_TOPPING_BORDER_WIDTH_DP,
    val isBorderPanelOpen: Boolean = false,
    val showQuitDialog: Boolean = false,
    /** `false` 인 동안은 "아직 못 읽음"과 "비었음"을 구분하지 못한다 */
    val isDraftLoaded: Boolean = false,
    /** 흐름 진입 때 초안에 못 박힌 캔버스다. 화면이 다시 고르지 않는다 */
    val groupId: GroupId? = null,
    val parfaitId: ParfaitId? = null,
    /** 구독 중인 오늘 캔버스의 id. 하루 경계를 넘기면 초안의 [parfaitId] 와 다를 수 있다 */
    val observedParfaitId: ParfaitId? = null,
    val nextPositionZ: Int? = null,
    /** 확정 판정의 근거. 그림이 뜨기 전 실측은 폴백 크기라 그대로 올리면 배율이 틀어진다 */
    val isToppingImageReady: Boolean = false,
    val isLoading: Boolean = false,
    /** [backgroundImageUrl] 이 있으면 그쪽이 우선이고, 이 색은 이미지가 없을 때만 그려진다 */
    val backgroundColor: Color = YGAtomicColors.Gray.White,
    val backgroundImageUrl: String? = null,
    val existingToppings: List<CanvasToppingVO> = emptyList(),
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val canvasSize: DpSize? = null,
    val toppingBaseSize: DpSize? = null,
    /** C-106: 사용자가 아직 손대지 않은 동안에만 정중앙·기준 크기로 자동 배치한다 */
    val hasUserAdjustedPlacement: Boolean = false,
) : UiState {
    val panelBorderWidthDp: Float get() = border?.widthDp ?: pendingBorderWidthDp
}

sealed interface CanvasToppingPlaceIntent : UiIntent {
    data object OnClickBack : CanvasToppingPlaceIntent

    data object OnClickClose : CanvasToppingPlaceIntent

    data object OnSystemBack : CanvasToppingPlaceIntent

    data object OnQuitDialogConfirm : CanvasToppingPlaceIntent

    data object OnQuitDialogCancel : CanvasToppingPlaceIntent

    data object OnClickConfirm : CanvasToppingPlaceIntent

    data object OnClickTopping : CanvasToppingPlaceIntent

    data object OnToggleBorderPanel : CanvasToppingPlaceIntent

    data object OnDismissBorderPanel : CanvasToppingPlaceIntent

    /** @param colorArgb `null` 은 테두리 없음 */
    data class OnSelectBorderColor(
        val colorArgb: Int?,
    ) : CanvasToppingPlaceIntent

    data class OnChangeBorderWidth(
        val widthDp: Float,
    ) : CanvasToppingPlaceIntent

    data class OnToppingTransform(
        val pan: DpOffset,
        val zoom: Float,
        val rotationDelta: Float,
    ) : CanvasToppingPlaceIntent

    /** 화면이 Canvas-Area 를 실측해 알려준다. C-106 초기 배치를 계산하는 데 쓴다 */
    data class OnCanvasMeasured(
        val canvasSize: DpSize,
    ) : CanvasToppingPlaceIntent

    /** 화면이 토핑 이미지의 실제(배율 1배 기준) 크기를 알려준다. C-106 초기 배치를 계산하는 데 쓴다 */
    data class OnToppingBaseSizeMeasured(
        val baseSize: DpSize,
    ) : CanvasToppingPlaceIntent

    /** 토핑 이미지 painter 가 실제 그림을 들었는지 화면이 알려준다 */
    data class OnToppingImageReadyChanged(
        val isReady: Boolean,
    ) : CanvasToppingPlaceIntent
}

sealed interface CanvasToppingPlaceEffect : UiSideEffect {
    data object NavigateBack : CanvasToppingPlaceEffect

    /** 토핑 만들기를 접는다. 사이에 쌓인 화면까지 걷고 캔버스로 되감는다 */
    data object QuitToCanvas : CanvasToppingPlaceEffect

    /** 초안이 가리키던 캐시 파일은 초안보다 먼저 사라질 수 있다 */
    data object DraftMissing : CanvasToppingPlaceEffect

    /** 초안을 비웠다. 캔버스로 되감으면 새 토핑이 오늘 조회에 함께 내려온다 */
    data object PlaceSucceeded : CanvasToppingPlaceEffect

    /** 다시 눌러 볼 값이 있는 실패. 화면에 남는다 */
    data object PlaceFailed : CanvasToppingPlaceEffect

    /** 다시 눌러도 같은 실패. 알리고 화면에 남는다 */
    data object PlaceFailedPermanently : CanvasToppingPlaceEffect

    /** painter 가 아직 그림을 못 들었는데 확인을 눌렀다. 초안 결손과 달리 다시 시도해 볼 수 있다 */
    data object ToppingImageNotReady : CanvasToppingPlaceEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CanvasToppingPlaceViewModel
@Inject constructor(
    private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
    private val clearToppingDraft: ClearToppingDraftUseCase,
    private val addToppingUseCase: AddToppingUseCase,
    private val addRecentImageUseCase: AddRecentImageUseCase,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val requestTodayParfaitRefreshUseCase: RequestTodayParfaitRefreshUseCase,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, CanvasToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(),
) {
    /**
     * 초안이 그룹을 알려 주기 전엔 비어 있다. [state] 를 게이트로 쓰지 않는 이유는
     * [launchWhileSubscribed] KDoc 참고 — 그 안에서 [state] 를 수집하면 그 수집 자체가
     * 구독자로 세어져 계수가 0 으로 내려가지 않는다.
     *
     * 흐르는 내내 같은 그룹이라 가정하지 않는다 — [observeCanvas] 가 [flatMapLatest] 로 구독해
     * 값이 바뀌면 업스트림을 갈아탄다.
     */
    private val observedGroupId = MutableStateFlow<GroupId?>(null)

    init {
        observeDraft()
        observeCanvas()
    }

    private fun observeDraft() {
        launch(onError = { postSideEffect(effect = CanvasToppingPlaceEffect.DraftMissing) }) {
            getToppingDraftFlow().collect { draft ->
                updateState {
                    copy(
                        toppingImagePath = draft?.subjectImagePath,
                        toppingSourceLongSide = draft?.sourceLongSide,
                        groupId = draft?.groupId,
                        parfaitId = draft?.parfaitId,
                        nextPositionZ = draft?.nextPositionZ,
                        isDraftLoaded = true,
                    )
                }
                observedGroupId.value = draft?.groupId
            }
        }
    }

    /**
     * 화면이 보이는 동안만 오늘 캔버스를 구독한다. 갱신은 구독 시작 즉시, 이후 주기마다
     * 폴러가 맡는다(`adr/0029-canvas-today-ssot-polling.md`) — 여기서 따로 조회를 걸지 않는다.
     */
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

    private fun CanvasToppingPlaceUiState.withCanvas(canvas: CanvasVO): CanvasToppingPlaceUiState = copy(
        backgroundColor = (canvas.background as? CanvasBackground.Color)
            ?.value
            ?.toColorOrNull()
            ?: backgroundColor,
        backgroundImageUrl = (canvas.background as? CanvasBackground.Image)?.url,
        existingToppings = canvas.toppings.sortedBy { topping -> topping.transform.positionZ },
        observedParfaitId = canvas.parfaitId,
    )

    /**
     * 흐름 진입 때 초안에 못 박은 값은 카메라·누끼를 거치는 사이 남이 토핑을 올리면 낡는다.
     * 그래서 확정 시점에 다시 센다.
     *
     * **초안과 같은 캔버스일 때만이다** — 구독 값은 "오늘"로 걸러진 캔버스라 하루 경계를 넘기면
     * 초안이 가리키는 캔버스와 다를 수 있고, 그때 재계산한 값을 초안의 캔버스에 실으면 사용자가
     * 들어간 캔버스가 아닌 곳의 깊이를 쓰게 된다(`adr/0026-topping-draft-datastore-ssot.md`).
     *
     * 겹침의 해결이 아니라 완화다 — 폴링 주기 안에 두 사람이 확인을 누르면 여전히 겹친다
     * (OQ-P-322).
     */
    private fun CanvasToppingPlaceUiState.resolvedPositionZ(): Int? {
        if (observedParfaitId == null || observedParfaitId != parfaitId) return nextPositionZ
        return (existingToppings.maxOfOrNull { it.transform.positionZ } ?: 0) + 1
    }

    override fun processIntent(intent: CanvasToppingPlaceIntent) {
        when (intent) {
            CanvasToppingPlaceIntent.OnClickBack -> postSideEffect(effect = CanvasToppingPlaceEffect.NavigateBack)

            CanvasToppingPlaceIntent.OnClickClose -> updateState { copy(showQuitDialog = true) }

            CanvasToppingPlaceIntent.OnSystemBack -> handleOnSystemBack()

            CanvasToppingPlaceIntent.OnQuitDialogConfirm -> {
                updateState { copy(showQuitDialog = false) }
                postSideEffect(effect = CanvasToppingPlaceEffect.QuitToCanvas)
            }

            CanvasToppingPlaceIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }

            CanvasToppingPlaceIntent.OnClickConfirm -> handleOnClickConfirm()

            // 누른 대상이 달라 인텐트를 나눈다. 이 화면은 토핑이 하나뿐이라 둘의 결과가 같다
            CanvasToppingPlaceIntent.OnClickTopping,
            CanvasToppingPlaceIntent.OnToggleBorderPanel,
            -> updateState {
                // 그림이 뜨기 전에는 열지 않는다 — 테두리를 입힐 실루엣이 아직 없다. 닫기는 막지 않는다
                copy(isBorderPanelOpen = !isBorderPanelOpen && isToppingImageReady)
            }

            CanvasToppingPlaceIntent.OnDismissBorderPanel -> updateState { copy(isBorderPanelOpen = false) }

            is CanvasToppingPlaceIntent.OnSelectBorderColor -> updateState {
                copy(border = intent.colorArgb?.let { argb -> ToppingBorderStyle(argb, panelBorderWidthDp) })
            }

            is CanvasToppingPlaceIntent.OnChangeBorderWidth -> updateState {
                val widthDp = intent.widthDp.coerceIn(TOPPING_BORDER_WIDTH_RANGE_DP)
                copy(pendingBorderWidthDp = widthDp, border = border?.copy(widthDp = widthDp))
            }

            is CanvasToppingPlaceIntent.OnToppingTransform -> handleOnToppingTransform(intent)

            is CanvasToppingPlaceIntent.OnCanvasMeasured -> {
                updateState { copy(canvasSize = intent.canvasSize).applyInitialPlacementIfNeeded() }
            }

            is CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured -> {
                updateState { copy(toppingBaseSize = intent.baseSize).applyInitialPlacementIfNeeded() }
            }

            is CanvasToppingPlaceIntent.OnToppingImageReadyChanged -> {
                updateState { copy(isToppingImageReady = intent.isReady) }
            }
        }
    }

    private fun handleOnSystemBack() {
        updateState {
            when {
                isLoading -> this
                isBorderPanelOpen -> copy(isBorderPanelOpen = false)
                else -> copy(showQuitDialog = true)
            }
        }
    }

    private fun handleOnToppingTransform(intent: CanvasToppingPlaceIntent.OnToppingTransform) {
        updateState {
            // 패널이 열린 동안 화면은 토핑을 저장된 자리가 아닌 곳에 보여 준다. 그 상태에서 받은
            // 이동량을 저장된 자리에 더하면 닫았을 때 토핑이 엉뚱한 데로 가 있다
            if (isBorderPanelOpen) return@updateState this

            // 실측 전에 hasUserAdjustedPlacement 가 굳으면 초기 배치가 영영 안 걸린다
            val canvasSize = canvasSize ?: return@updateState this
            val baseSize = toppingBaseSize ?: return@updateState this

            copy(
                offsetX = offsetX + intent.pan.x,
                offsetY = offsetY + intent.pan.y,
                scale = (scale * intent.zoom).coerceAtLeast(minScale(canvasSize, baseSize)),
                rotationDegrees = rotationDegrees + intent.rotationDelta,
                hasUserAdjustedPlacement = true,
            )
        }
    }

    /**
     * C-106: 사용자가 아직 손대지 않았다면, 캔버스 정중앙에 토핑의 긴 변이
     * 캔버스 너비의 [TOPPING_BASE_LONG_SIDE_RATIO] 가 되도록 자동으로 놓는다.
     *
     * 캔버스 실측 크기와 토핑 원본 크기를 둘 다 알아야 계산되고, 이 둘은 서로 다른 시점에
     * 비동기로 들어오므로 어느 쪽이 먼저 도착하든 매번 다시 시도한다.
     */
    private fun CanvasToppingPlaceUiState.applyInitialPlacementIfNeeded(): CanvasToppingPlaceUiState {
        if (hasUserAdjustedPlacement) return this
        val canvasSize = canvasSize ?: return this
        val baseSize = toppingBaseSize ?: return this

        val longerBaseSide = maxOf(baseSize.width, baseSize.height)
        val shorterBaseSide = minOf(baseSize.width, baseSize.height)

        val longSideScale = (canvasSize.width * TOPPING_BASE_LONG_SIDE_RATIO) / longerBaseSide
        // 위 배율대로 두면 짧은 변이 최소 터치 영역보다 작아질 수 있어, 그 경우 짧은 변 기준으로 다시 키운다
        val minTouchScale = MIN_TOPPING_SHORT_SIDE / shorterBaseSide
        val newScale = maxOf(longSideScale, minTouchScale)

        return copy(
            scale = newScale,
            offsetX = (canvasSize.width - baseSize.width) / 2,
            offsetY = (canvasSize.height - baseSize.height) / 2,
        )
    }

    /** [TOPPING_MIN_SCALE] 을 이 화면 배율로 환산한다. [toToppingTransform] 의 역산이다 */
    private fun minScale(
        canvasSize: DpSize,
        baseSize: DpSize,
    ): Float {
        val longerBaseSide = maxOf(baseSize.width, baseSize.height)
        return canvasSize.width * (TOPPING_MIN_SCALE * TOPPING_BASE_LONG_SIDE_RATIO) / longerBaseSide
    }

    /**
     * 4단계(발급 → 전송 → 확인 → 배치)를 한 덩어리로 본다. 단계별 진행률을 표시하지 않는 것이
     * 스펙의 결정이고, 실패하면 발급부터 전부 다시 탄다.
     *
     * ⚠️ 확정이 도는 동안 화면을 떠나면 `viewModelScope` 취소가 업로드를 끊는다. 확인까지
     * 간 뒤 배치 전에 끊기면 **서버에 고아 이미지가 남는다** — 되돌리지 않기로 한 자리다
     * (`specs/2026-08-20-c106-topping-place-api.md`).
     */
    private fun handleOnClickConfirm() {
        val current = state.value
        if (!current.isDraftLoaded) return

        val imagePath = current.toppingImagePath
        val groupId = current.groupId
        val parfaitId = current.parfaitId
        val positionZ = current.nextPositionZ
        if (imagePath == null || groupId == null || parfaitId == null || positionZ == null) {
            postSideEffect(effect = CanvasToppingPlaceEffect.DraftMissing)
            return
        }

        // 그림이 아직 없으면 실측이 폴백 크기다. 그것으로 계산한 배율이 서버에 굳는다
        val canvasSize = current.canvasSize
        val baseSize = current.toppingBaseSize
        if (!current.isToppingImageReady || canvasSize == null || baseSize == null) {
            postSideEffect(effect = CanvasToppingPlaceEffect.ToppingImageNotReady)
            return
        }

        val transform = toToppingTransform(
            offsetX = current.offsetX,
            offsetY = current.offsetY,
            scale = current.scale,
            rotationDegrees = current.rotationDegrees,
            canvasSize = canvasSize,
            toppingBaseSize = baseSize,
            positionZ = current.resolvedPositionZ() ?: positionZ,
        )
        launch(key = CONFIRM_JOB_KEY, onError = { postSideEffect(CanvasToppingPlaceEffect.PlaceFailed) }) {
            updateState { copy(isLoading = true) }

            // finally 하나로 성공·실패·예외·취소 네 경로를 다 덮는다 — onSuccess/onFailure 에
            // 각자 흩어 두면 Result.onSuccess { } 가 던지는 경로가 어디에도 안 걸린다
            try {
                // 테두리 조립은 던질 수 있다. launch 밖에서 부르면 그 예외가 onError 를 못 만나고
                // 호출 스레드까지 올라가 크래시가 된다. 업로드보다 앞이라 고아 이미지도 안 남는다
                val border = current.border.toToppingBorder()

                addToppingUseCase(
                    groupId = groupId,
                    parfaitId = parfaitId,
                    filePath = imagePath,
                    transform = transform,
                    border = border,
                    sourceLongSide = current.toppingSourceLongSide,
                ).onSuccess {
                    // 알림보다 먼저 남긴다 — PlaceSucceeded 를 받은 Route 가 popUpTo 로 이 화면을
                    // 걷어 내면 viewModelScope 가 취소되고, 그 뒤 코드는 실행되다 말고 끊긴다
                    runSuspendCatching {
                        addRecentImageUseCase(source = imagePath, kind = RecentImageKind.CUTOUT)
                    }.onFailure { throwable ->
                        viewModelLogger.d { "recent cutout save failed - $throwable" }
                    }

                    // 즉시 반환하는 non-suspend 표면이라 되감기를 늦추지 않는다. suspend 로
                    // 기다리면 확인 버튼을 누른 뒤 네트워크 왕복만큼 멈춘 것처럼 보이고,
                    // PlaceSucceeded 뒤로 옮기면 되감기가 문 viewModelScope 취소로 아예 안 돈다
                    requestTodayParfaitRefreshUseCase(groupId)

                    // 되감기를 먼저 알린다 — clearToppingDraft() 가 초안을 비우면 구독이 알맹이를 null 로
                    // 되돌려, 오버레이가 내려간 화면에 빈 캔버스가 잠깐 조작 가능한 상태로 남는다
                    postSideEffect(effect = CanvasToppingPlaceEffect.PlaceSucceeded)
                    clearToppingDraft()
                }.onFailure { throwable ->
                    val error = throwable as? AppError ?: AppError.Unexpected(throwable)
                    postSideEffect(
                        effect = if (error.isPermanentPlaceFailure()) {
                            CanvasToppingPlaceEffect.PlaceFailedPermanently
                        } else {
                            CanvasToppingPlaceEffect.PlaceFailed
                        },
                    )
                }
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}

/** 확정 작업의 중복 실행 키. 연타로 두 번 올라가면 고아 이미지와 겹친 토핑이 함께 생긴다 */
private const val CONFIRM_JOB_KEY = "canvas-topping-place-confirm"
