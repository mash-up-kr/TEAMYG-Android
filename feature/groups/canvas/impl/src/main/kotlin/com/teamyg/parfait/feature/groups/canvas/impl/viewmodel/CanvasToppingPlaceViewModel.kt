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
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.feature.groups.canvas.impl.component.TOPPING_BASE_LONG_SIDE_RATIO
import com.teamyg.parfait.feature.groups.canvas.impl.util.resizeOutwardDirection
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
    /** 올릴 알맹이의 파일 시스템 절대경로. 초안이 비어 있으면 `null` 이다 */
    val toppingImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    /** 초안 흐름이 한 번이라도 방출됐는가. `false`인 동안은 "아직 못 읽음"과 "비었음"을 구분 못 한다 */
    val isDraftLoaded: Boolean = false,
    // TODO: 캔버스의 실제 배경(색/이미지) 로드 API 연동 필요 - 지금은 기본 배경색만 보여준다
    val backgroundColor: Color = YGAtomicColors.Gray.White,
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
}

sealed interface CanvasToppingPlaceEffect : UiSideEffect {
    data object NavigateBack : CanvasToppingPlaceEffect

    /** 올릴 알맹이가 없다. 초안이 가리키던 캐시 파일은 먼저 사라질 수 있다 */
    data object DraftMissing : CanvasToppingPlaceEffect

    /** 사용자가 정한 위치·크기·각도로 토핑 배치를 확정했다. */
    data class ToppingPlaced(
        val imagePath: String,
        val offsetX: Dp,
        val offsetY: Dp,
        val scale: Float,
        val rotationDegrees: Float,
    ) : CanvasToppingPlaceEffect
}

@HiltViewModel
class CanvasToppingPlaceViewModel
@Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, CanvasToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(),
) {
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
                        isDraftLoaded = true,
                    )
                }
            }
        }
    }

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

    private fun handleOnClickConfirm() {
        val current = state.value
        // 초안 첫 방출 전이면 "비었다"와 "아직 못 읽었다"를 구분 못 한다 — 거짓 DraftMissing을 피해 무시한다
        if (!current.isDraftLoaded) return

        val imagePath = current.toppingImagePath
        if (imagePath == null) {
            postSideEffect(effect = CanvasToppingPlaceEffect.DraftMissing)
            return
        }

        // TODO: 캔버스에 토핑을 배치하는 저장 API 연동 필요 - 지금은 결과만 이펙트로 흘려보낸다
        postSideEffect(
            effect = CanvasToppingPlaceEffect.ToppingPlaced(
                imagePath = imagePath,
                offsetX = current.offsetX,
                offsetY = current.offsetY,
                scale = current.scale,
                rotationDegrees = current.rotationDegrees,
            ),
        )
    }
}
