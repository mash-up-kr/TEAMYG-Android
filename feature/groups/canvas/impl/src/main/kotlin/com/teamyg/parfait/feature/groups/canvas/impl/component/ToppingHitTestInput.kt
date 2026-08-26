package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
 */
@Composable
internal fun <T> Modifier.toppingTapInput(
    entries: () -> List<Pair<T, ToppingHitTarget>>,
    keyOf: (T) -> Any,
    onHit: (T) -> Unit,
    onMiss: () -> Unit,
): Modifier {
    val latestEntries by rememberUpdatedState(entries)
    val latestKeyOf by rememberUpdatedState(keyOf)
    val latestOnHit by rememberUpdatedState(onHit)
    val latestOnMiss by rememberUpdatedState(onMiss)
    val throttle = remember { ToppingClickThrottle() }

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                // 같은 노드에 달린 드래그 입력이 이 down 을 먼저 볼 수 있으므로 소비 여부를 따지지 않는다
                val down = awaitFirstDown(requireUnconsumed = false)
                // 대상은 누른 자리로 고정한다. 뗀 자리를 보면 슬롭만큼 미끄러진 곳의 다른 토핑이
                // 잡히거나, 투명한 자리로 떨어져 미스 분기가 발동한다
                val hit = pickToppingHit(latestEntries(), down.position.x, down.position.y)
                down.consume()

                // up 없이 끝나면(드래그가 이동을 소비했거나 손가락이 벗어남) 아무 콜백도 부르지 않는다
                val up = waitForUpOrCancellation() ?: return@awaitEachGesture
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
 * 터치 다운 지점이 [targetAt] 의 실루엣 안일 때만 드래그를 소비한다.
 *
 * 판정은 down 좌표로 하고 이동은 슬롭을 넘긴 뒤부터 친다 — 슬롭을 버리면 탭이 미세 이동만으로
 * 이동으로 처리된다. 슬롭을 넘긴 그 프레임의 이동량도 첫 델타로 흘려보낸다. 버리면 제스처마다
 * 한 프레임씩 손가락 뒤로 처지고 오차가 쌓인다.
 */
@Composable
internal fun Modifier.toppingDragInput(
    targetAt: () -> ToppingHitTarget?,
    onDrag: (Offset) -> Unit,
): Modifier {
    val latestTargetAt by rememberUpdatedState(targetAt)
    val latestOnDrag by rememberUpdatedState(onDrag)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val target = latestTargetAt() ?: return@awaitEachGesture
                if (!target.containsPoint(down.position.x, down.position.y)) {
                    return@awaitEachGesture
                }

                var overSlop = Offset.Zero
                val afterSlop = awaitTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    overSlop = over
                } ?: return@awaitEachGesture

                latestOnDrag(overSlop)
                drag(afterSlop.id) { change ->
                    // 소비한 change 의 positionChange() 는 Offset.Zero 다. 읽고 나서 소비한다
                    latestOnDrag(change.positionChange())
                    change.consume()
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
