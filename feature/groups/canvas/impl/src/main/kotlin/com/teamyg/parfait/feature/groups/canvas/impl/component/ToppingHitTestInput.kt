package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingClickThrottle
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.pickToppingHit

private const val MISS_KEY = "miss"

/**
 * 누른 자리로 대상을 정하고, 떼는 것이 확정되면 그 대상으로 [onHit] 을 부른다. 아무것도 맞지
 * 않으면 [onMiss] 다 — 이벤트를 아래로 흘려보내지는 않는다. 레이어가 캔버스 영역의 포인터를
 * 독점한다.
 *
 * 핸들러를 [remember] 로 붙잡는 이유는 포인터 입력이 핸들러를 참조로 비교하기 때문이다.
 * 매번 새 람다를 넘기면 진행 중인 제스처가 리셋된다.
 *
 * @param entries 겹침 순서가 **아래에서 위**인 목록. 그리는 순서 그대로 넘기면 된다.
 * @param keyOf 연타 방어가 "같은 대상"을 가리는 기준
 * @param enabled 첫 down 때 한 번만 읽는다. 거짓이면 그 제스처는 손가락이 모두 떨어질 때까지 통째로
 *   버린다. 입력을 체인에서 빼는 대신 이것으로 끈다 — [dismissPanelOnTouch] 참고.
 */
@Composable
internal fun <T> Modifier.toppingTapInput(
    entries: () -> List<Pair<T, ToppingHitTarget>>,
    keyOf: (T) -> Any,
    onHit: (T) -> Unit,
    onMiss: () -> Unit,
    enabled: () -> Boolean = { true },
): Modifier {
    val latestEntries by rememberUpdatedState(entries)
    val latestKeyOf by rememberUpdatedState(keyOf)
    val latestOnHit by rememberUpdatedState(onHit)
    val latestOnMiss by rememberUpdatedState(onMiss)
    val latestEnabled by rememberUpdatedState(enabled)
    val throttle = remember { ToppingClickThrottle() }

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                // 같은 노드에 달린 변환 입력이 이 down 을 먼저 볼 수 있으므로 소비 여부를 따지지 않는다
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!latestEnabled()) return@awaitEachGesture
                // 대상은 누른 자리로 고정한다. 뗀 자리를 보면 슬롭만큼 미끄러진 곳의 다른 토핑이
                // 잡히거나, 투명한 자리로 떨어져 미스 분기가 발동한다
                val hit = pickToppingHit(latestEntries(), down.position.x, down.position.y)
                down.consume()

                val up = waitForTapUp(down.id) ?: return@awaitEachGesture
                up.consume()

                if (throttle.tryPass(hit?.let(latestKeyOf) ?: MISS_KEY)) {
                    hit?.let(latestOnHit) ?: latestOnMiss()
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}

/**
 * [waitForUpOrCancellation] 에 "다른 손가락이 눌리면 탭 아님"을 더한 것. 두 손가락을 대고 움직이지
 * 않은 채 떼면 아무도 소비하지 않아서 이 조건이 없으면 탭으로 잡힌다.
 */
private suspend fun AwaitPointerEventScope.waitForTapUp(downId: PointerId): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.any { it.id != downId && it.pressed }) return null

        val change = event.changes.firstOrNull { it.id == downId } ?: return null
        if (change.changedToUp()) return change
        if (change.isConsumed || change.isOutOfBounds(size, extendedTouchPadding)) return null

        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.any { it.id == downId && it.isConsumed }) return null
    }
}
