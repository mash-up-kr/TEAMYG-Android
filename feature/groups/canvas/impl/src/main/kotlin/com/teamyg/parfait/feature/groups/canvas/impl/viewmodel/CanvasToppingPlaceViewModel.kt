package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
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
import com.teamyg.parfait.core.util.android.extension.toRgbHexString
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitUseCase
import com.teamyg.parfait.domain.usecase.topping.AddToppingUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BASE_LONG_SIDE_RATIO
import com.teamyg.parfait.feature.groups.canvas.impl.util.isPermanentPlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.util.resizeOutwardDirection
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingTransform
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 캔버스·토핑 실측 전(치수를 아직 모를 때)에만 쓰는 임시 하한·상한.
private const val TOPPING_MIN_SCALE_FALLBACK = 0.5f
private const val TOPPING_MAX_SCALE_FALLBACK = 2.5f

/**
 * 원본이 큰 사진은 그 배율만으로는 캔버스를 못 벗어나 "아무리 키워도 안 벗어나는" 것처럼 보인다 —
 * 최대 배율을 캔버스 대비 객체의 실제 크기로 역산해야 원본 크기와 무관하게 항상 벗어날 수 있다.
 */
private const val TOPPING_MAX_OVERFLOW_RATIO = 1.5f

/** 세로로 이 픽셀만큼 드래그해야 배율이 1.0만큼 바뀐다 */
private const val TOPPING_DRAG_PX_PER_SCALE = 300f

/** 가로로 1픽셀 드래그할 때 회전하는 각도 */
private const val TOPPING_DRAG_DEGREES_PER_PX = 0.5f

/** 스케일링된 토핑의 짧은 변이 이보다 작아지면, 그 변을 이 크기로 맞추도록 강제 상향한다 */
private val MIN_TOPPING_SHORT_SIDE = 48.dp

data class CanvasToppingPlaceUiState(
    /** 올릴 알맹이의 파일 시스템 절대경로. `file://` uri 가 아니다 */
    val toppingImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    /** `false` 인 동안은 "아직 못 읽음"과 "비었음"을 구분하지 못한다 */
    val isDraftLoaded: Boolean = false,
    /** 흐름 진입 때 초안에 못 박힌 캔버스다. 화면이 다시 고르지 않는다 */
    val groupId: GroupId? = null,
    val parfaitId: ParfaitId? = null,
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
) : UiState

sealed interface CanvasToppingPlaceIntent : UiIntent {
    data object OnClickClose : CanvasToppingPlaceIntent

    data object OnClickConfirm : CanvasToppingPlaceIntent

    /** 토핑을 잡고 드래그한 만큼 넘어온다. 드래그한 그대로 위치를 옮긴다. */
    data class OnToppingMoveDrag(
        val delta: DpOffset,
    ) : CanvasToppingPlaceIntent

    /**
     * 크기조절 아이콘을 잡고 드래그한 만큼 넘어온다. 토핑 바깥쪽(우측 상단 대각선) 방향으로 끌면 커지고
     * 안쪽으로 끌면 작아진다.
     */
    data class OnToppingResizeDrag(
        val delta: Offset,
    ) : CanvasToppingPlaceIntent

    /** 회전 아이콘을 잡고 드래그한 만큼 넘어온다. 가로 드래그 거리만큼 회전한다. */
    data class OnToppingRotateDrag(
        val delta: Offset,
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

@HiltViewModel
class CanvasToppingPlaceViewModel
@Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
    private val addToppingUseCase: AddToppingUseCase,
    private val addRecentImageUseCase: AddRecentImageUseCase,
    private val getTodayParfaitUseCase: GetTodayParfaitUseCase,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, CanvasToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(),
) {
    /** 캔버스(배경·기존 토핑)는 초안이 아니라 서버가 SSOT다 — groupId 하나당 한 번만 조회한다 */
    private var canvasLoadedForGroupId: GroupId? = null

    init {
        observeDraft()
    }

    private fun observeDraft() {
        launch(onError = { postSideEffect(effect = CanvasToppingPlaceEffect.DraftMissing) }) {
            toppingDraftRepository.draft.collect { draft ->
                updateState {
                    copy(
                        toppingImagePath = draft?.subjectImagePath,
                        borderColorArgb = draft?.borderColorArgb,
                        borderWidthDp = draft?.borderWidthDp,
                        groupId = draft?.groupId,
                        parfaitId = draft?.parfaitId,
                        nextPositionZ = draft?.nextPositionZ,
                        isDraftLoaded = true,
                    )
                }
                draft?.groupId?.let { groupId -> loadCanvasIfNeeded(groupId) }
            }
        }
    }

    /**
     * 배치 화면은 캔버스를 새로 만들지 않는다
     */
    private fun loadCanvasIfNeeded(groupId: GroupId) {
        if (canvasLoadedForGroupId == groupId) return
        canvasLoadedForGroupId = groupId

        launch(key = LOAD_CANVAS_KEY) {
            getTodayParfaitUseCase(groupId)
                .onSuccess { canvas -> updateState { withCanvas(canvas) } }
                .onFailure { throwable ->
                    // 조회 실패는 토핑 배치 자체를 막지 않는다 — 기본 배경·빈 토핑 목록으로 그대로 둔다
                    viewModelLogger.e(throwable) { "캔버스를 불러오지 못했다 - groupId: ${groupId.value}" }
                }
        }
    }

    private fun CanvasToppingPlaceUiState.withCanvas(canvas: CanvasVO): CanvasToppingPlaceUiState = copy(
        backgroundColor = (canvas.background as? CanvasBackground.Color)
            ?.value
            ?.toColorOrNull()
            ?: backgroundColor,
        backgroundImageUrl = (canvas.background as? CanvasBackground.Image)?.url,
        existingToppings = canvas.toppings.sortedBy { topping -> topping.transform.positionZ },
    )

    override fun processIntent(intent: CanvasToppingPlaceIntent) {
        when (intent) {
            CanvasToppingPlaceIntent.OnClickClose -> postSideEffect(effect = CanvasToppingPlaceEffect.NavigateBack)

            CanvasToppingPlaceIntent.OnClickConfirm -> handleOnClickConfirm()

            is CanvasToppingPlaceIntent.OnToppingMoveDrag -> handleOnToppingMoveDrag(intent)

            is CanvasToppingPlaceIntent.OnToppingResizeDrag -> handleOnToppingResizeDrag(intent)

            is CanvasToppingPlaceIntent.OnToppingRotateDrag -> handleOnToppingRotateDrag(intent)

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

    private fun handleOnToppingMoveDrag(intent: CanvasToppingPlaceIntent.OnToppingMoveDrag) {
        updateState {
            copy(
                offsetX = offsetX + intent.delta.x,
                offsetY = offsetY + intent.delta.y,
                hasUserAdjustedPlacement = true,
            )
        }
    }

    private fun handleOnToppingResizeDrag(intent: CanvasToppingPlaceIntent.OnToppingResizeDrag) {
        updateState {
            // 핸들이 우측 상단 모서리에 있으므로, 그 모서리의 바깥쪽 방향으로 끌면 커지고 안쪽이면 작아진다
            val (outX, outY) = resizeOutwardDirection(rotationDegrees)
            val deltaScale = (intent.delta.x * outX + intent.delta.y * outY) / TOPPING_DRAG_PX_PER_SCALE
            copy(
                scale = (scale + deltaScale).coerceIn(minScaleForTouchTarget(), maxScaleToOverflowCanvas()),
                hasUserAdjustedPlacement = true,
            )
        }
    }

    private fun handleOnToppingRotateDrag(intent: CanvasToppingPlaceIntent.OnToppingRotateDrag) {
        updateState {
            copy(
                rotationDegrees = rotationDegrees + intent.delta.x * TOPPING_DRAG_DEGREES_PER_PX,
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

    /**
     * 리사이즈로 도달할 수 있는 하한. 고정 배율(0.5)로 두면, 초기 배치 배율이 그보다 작은
     * 큰 원본 사진(예: 0.23)은 한 번이라도 리사이즈를 건드리는 순간 원래 크기보다 작게는
     * 다시 못 줄인다 — [applyInitialPlacementIfNeeded]와 같은 최소 터치 영역 기준으로 역산해
     * 원본 크기와 무관하게 항상 처음 크기까지는 줄일 수 있게 한다.
     */
    private fun CanvasToppingPlaceUiState.minScaleForTouchTarget(): Float {
        val baseSize = toppingBaseSize ?: return TOPPING_MIN_SCALE_FALLBACK
        val shorterBaseSide = minOf(baseSize.width, baseSize.height)
        if (shorterBaseSide <= 0.dp) return TOPPING_MIN_SCALE_FALLBACK

        return MIN_TOPPING_SHORT_SIDE / shorterBaseSide
    }

    /**
     * 리사이즈로 도달할 수 있는 상한.
     */
    private fun CanvasToppingPlaceUiState.maxScaleToOverflowCanvas(): Float {
        val canvasSize = canvasSize ?: return TOPPING_MAX_SCALE_FALLBACK
        val baseSize = toppingBaseSize ?: return TOPPING_MAX_SCALE_FALLBACK
        val longerCanvasSide = maxOf(canvasSize.width, canvasSize.height).value
        val longerBaseSide = maxOf(baseSize.width, baseSize.height).value
        if (longerBaseSide <= 0f) return TOPPING_MAX_SCALE_FALLBACK

        val overflowScale = (longerCanvasSide * TOPPING_MAX_OVERFLOW_RATIO) / longerBaseSide
        return maxOf(overflowScale, TOPPING_MAX_SCALE_FALLBACK)
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
            positionZ = positionZ,
        )
        launch(key = CONFIRM_JOB_KEY, onError = { postSideEffect(CanvasToppingPlaceEffect.PlaceFailed) }) {
            updateState { copy(isLoading = true) }

            // finally 하나로 성공·실패·예외·취소 네 경로를 다 덮는다 — onSuccess/onFailure 에
            // 각자 흩어 두면 Result.onSuccess { } 가 던지는 경로가 어디에도 안 걸린다
            try {
                // 테두리 조립은 던질 수 있다. launch 밖에서 부르면 그 예외가 onError 를 못 만나고
                // 호출 스레드까지 올라가 크래시가 된다. 업로드보다 앞이라 고아 이미지도 안 남는다
                val border = toToppingBorder(current.borderColorArgb, current.borderWidthDp)

                addToppingUseCase(
                    groupId = groupId,
                    parfaitId = parfaitId,
                    filePath = imagePath,
                    transform = transform,
                    border = border,
                ).onSuccess {
                    // 알림보다 먼저 남긴다 — PlaceSucceeded 를 받은 Route 가 popUpTo 로 이 화면을
                    // 걷어 내면 viewModelScope 가 취소되고, 그 뒤 코드는 실행되다 말고 끊긴다
                    runSuspendCatching {
                        addRecentImageUseCase(source = imagePath, kind = RecentImageKind.CUTOUT)
                    }.onFailure { throwable ->
                        viewModelLogger.d { "recent cutout save failed - $throwable" }
                    }

                    // 되감기를 먼저 알린다 — clear() 가 초안을 비우면 구독이 알맹이를 null 로
                    // 되돌려, 오버레이가 내려간 화면에 빈 캔버스가 잠깐 조작 가능한 상태로 남는다
                    postSideEffect(effect = CanvasToppingPlaceEffect.PlaceSucceeded)
                    toppingDraftRepository.clear()
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

    /** 색이나 두께가 빠진 `SOLID` 는 서버가 400 으로 거절한다 — 둘 다 있을 때만 만든다 */
    private fun toToppingBorder(
        colorArgb: Int?,
        widthDp: Float?,
    ): ToppingBorder = if (colorArgb != null && widthDp != null) {
        ToppingBorder.Solid(color = colorArgb.toRgbHexString(), width = widthDp.toDouble())
    } else {
        ToppingBorder.None
    }
}

/** 확정 작업의 중복 실행 키. 연타로 두 번 올라가면 고아 이미지와 겹친 토핑이 함께 생긴다 */
private const val CONFIRM_JOB_KEY = "canvas-topping-place-confirm"

/** 캔버스(배경·기존 토핑) 조회 작업의 중복 실행 키 */
private const val LOAD_CANVAS_KEY = "canvas-topping-place-load-canvas"
