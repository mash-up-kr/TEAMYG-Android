package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 제스처의 첫 down 때 [isPanelOpen] 이 참이면 [onDismiss] 를 부른다. down 은 소비하지 않는다.
 *
 * [toppingTapInput]·[toppingTransformInput] 보다 **바깥**(체인의 앞)에 단다. 같은 down 을 안쪽
 * 입력이 먼저 받으므로, 두 입력이 `enabled` 를 읽고 그 제스처를 버린 뒤에야 [onDismiss] 가 상태를
 * 바꾼다. 순서를 뒤집으면 패널을 닫은 그 터치가 탭·드래그로 이어진다.
 *
 * 세 입력은 패널 상태에 따라 체인에서 빼지 않는다. 갈아 끼우면 진행 중이던 핸들러가 리셋돼서,
 * 리컴포지션 뒤에 닿는 두 번째 손가락이 새로 붙은 변형 입력으로 샌다.
 */
@Composable
internal fun Modifier.dismissPanelOnTouch(
    isPanelOpen: () -> Boolean,
    onDismiss: () -> Unit,
): Modifier {
    val latestIsPanelOpen by rememberUpdatedState(isPanelOpen)
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                if (latestIsPanelOpen()) latestOnDismiss()
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
