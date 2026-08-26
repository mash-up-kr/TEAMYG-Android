package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
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

private const val MISS_KEY = "miss"

/**
 * 겹친 것부터 훑어 처음 맞는 것을 고른다. 아무것도 맞지 않으면 [onMiss] 다 —
 * 이벤트를 아래로 흘려보내지는 않는다. 레이어가 캔버스 영역의 포인터를 독점한다.
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
            detectTapGestures { offset ->
                val hit = latestEntries()
                    .asReversed()
                    .firstOrNull { (_, target) -> target.containsPoint(offset.x, offset.y) }

                if (throttle.tryPass(hit?.let { latestKeyOf(it.first) } ?: MISS_KEY)) {
                    hit?.let { latestOnHit(it.first) } ?: latestOnMiss()
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
 * 이동으로 처리된다.
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

                val afterSlop = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                    change.consume()
                } ?: return@awaitEachGesture

                drag(afterSlop.id) { change ->
                    change.consume()
                    latestOnDrag(change.positionChange())
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
