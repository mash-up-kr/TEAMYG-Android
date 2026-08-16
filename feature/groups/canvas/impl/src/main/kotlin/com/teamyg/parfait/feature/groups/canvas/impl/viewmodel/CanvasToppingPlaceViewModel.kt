package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.feature.groups.canvas.impl.util.resizeOutwardDirection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

private const val TOPPING_MIN_SCALE = 0.5f
private const val TOPPING_MAX_SCALE = 2.5f

/** 세로로 이 픽셀만큼 드래그해야 배율이 1.0만큼 바뀐다 */
private const val TOPPING_DRAG_PX_PER_SCALE = 300f

/** 가로로 1픽셀 드래그할 때 회전하는 각도 */
private const val TOPPING_DRAG_DEGREES_PER_PX = 0.5f

data class CanvasToppingPlaceUiState(
    val toppingImageUri: String,
    // TODO: 캔버스의 실제 배경(색/이미지) 로드 API 연동 필요 - 지금은 기본 배경색만 보여준다
    val backgroundColor: Color = YGAtomicColors.Gray.White,
    val offsetX: Dp = 60.dp,
    val offsetY: Dp = 100.dp,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
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
}

sealed interface CanvasToppingPlaceEffect : UiSideEffect {
    data object NavigateBack : CanvasToppingPlaceEffect

    /** 사용자가 정한 위치·크기·각도로 토핑 배치를 확정했다. */
    data class ToppingPlaced(
        val imageUri: String,
        val offsetX: Dp,
        val offsetY: Dp,
        val scale: Float,
        val rotationDegrees: Float,
    ) : CanvasToppingPlaceEffect
}

@HiltViewModel(assistedFactory = CanvasToppingPlaceViewModel.Factory::class)
class CanvasToppingPlaceViewModel
@AssistedInject constructor(
    @Assisted("imageUri") imageUri: String,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, CanvasToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(toppingImageUri = imageUri),
) {
    override fun processIntent(intent: CanvasToppingPlaceIntent) {
        when (intent) {
            CanvasToppingPlaceIntent.OnClickClose -> postSideEffect(effect = CanvasToppingPlaceEffect.NavigateBack)
            CanvasToppingPlaceIntent.OnClickConfirm -> handleOnClickConfirm()
            is CanvasToppingPlaceIntent.OnToppingMoveDrag -> handleOnToppingMoveDrag(intent)
            is CanvasToppingPlaceIntent.OnToppingResizeDrag -> handleOnToppingResizeDrag(intent)
            is CanvasToppingPlaceIntent.OnToppingRotateDrag -> handleOnToppingRotateDrag(intent)
        }
    }

    private fun handleOnToppingMoveDrag(intent: CanvasToppingPlaceIntent.OnToppingMoveDrag) {
        updateState {
            copy(
                offsetX = offsetX + intent.delta.x,
                offsetY = offsetY + intent.delta.y,
            )
        }
    }

    private fun handleOnToppingResizeDrag(intent: CanvasToppingPlaceIntent.OnToppingResizeDrag) {
        updateState {
            // 핸들이 우측 상단 모서리에 있으므로, 그 모서리의 바깥쪽 방향으로 끌면 커지고 안쪽이면 작아진다
            val (outX, outY) = resizeOutwardDirection(rotationDegrees)
            val deltaScale = (intent.delta.x * outX + intent.delta.y * outY) / TOPPING_DRAG_PX_PER_SCALE
            copy(scale = (scale + deltaScale).coerceIn(TOPPING_MIN_SCALE, TOPPING_MAX_SCALE))
        }
    }

    private fun handleOnToppingRotateDrag(intent: CanvasToppingPlaceIntent.OnToppingRotateDrag) {
        updateState {
            copy(rotationDegrees = rotationDegrees + intent.delta.x * TOPPING_DRAG_DEGREES_PER_PX)
        }
    }

    private fun handleOnClickConfirm() {
        val current = state.value

        // TODO: 캔버스에 토핑을 배치하는 저장 API 연동 필요 - 지금은 결과만 이펙트로 흘려보낸다
        postSideEffect(
            effect = CanvasToppingPlaceEffect.ToppingPlaced(
                imageUri = current.toppingImageUri,
                offsetX = current.offsetX,
                offsetY = current.offsetY,
                scale = current.scale,
                rotationDegrees = current.rotationDegrees,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("imageUri") imageUri: String): CanvasToppingPlaceViewModel
    }
}
