package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingClickThrottle
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.pickToppingHit
import kotlin.math.atan2

private const val MISS_KEY = "miss"

/**
 * 두 손가락이 이보다 가까우면 변환하지 않는다. 좁은 폭에서는 손가락이 조금만 굴러도 거리비가 크게
 * 흔들리고, 터치 패널이 두 손가락을 하나로 합쳤다 나누며 좌표를 튀게 만든다
 */
internal val TOPPING_PINCH_MIN_SPAN = 64.dp

/** 한 이벤트에 한 포인터가 이보다 멀리 움직이면 터치 패널이 추적을 놓친 것으로 본다 */
internal val TOPPING_POINTER_MAX_JUMP = 48.dp

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
                // 같은 노드에 달린 변환 입력이 이 down 을 먼저 볼 수 있으므로 소비 여부를 따지지 않는다
                val down = awaitFirstDown(requireUnconsumed = false)
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

/**
 * 두 손가락이 닿으면 어디서 시작했든 옮기기·회전·확대를 한 번에 한다. 한 손가락으로 시작하면
 * 옮기지 않지만, 핀치 중 한 손가락을 떼면 남은 손가락으로 계속 옮긴다.
 *
 * 직전 이벤트에도 눌려 있던 포인터만 센다. 새로 down 됐거나 막 뗀 포인터를 넣으면 포인터 수가
 * 바뀌는 순간 중점이 튄다.
 *
 * @param onTransform `pan` 은 px, `rotationDelta` 는 `rotationZ` 와 같은 방향의 도 단위.
 */
@Composable
internal fun Modifier.toppingTransformInput(
    enabled: () -> Boolean,
    onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit = {},
): Modifier {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnTransform by rememberUpdatedState(onTransform)
    val latestOnGestureActiveChange by rememberUpdatedState(onGestureActiveChange)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                if (!latestEnabled()) return@awaitEachGesture

                fun emit(
                    pan: Offset,
                    zoom: Float,
                    rotationDelta: Float,
                ): Boolean {
                    if (pan == Offset.Zero && zoom == 1f && rotationDelta == 0f) return false
                    latestOnTransform(pan, zoom, rotationDelta)
                    return true
                }

                latestOnGestureActiveChange(true)
                try {
                    val minSpan = TOPPING_PINCH_MIN_SPAN.toPx()
                    val maxJump = TOPPING_POINTER_MAX_JUMP.toPx()
                    var grabbed = false
                    var pairIds: Pair<PointerId, PointerId>? = null
                    var referenceSpan = 0f
                    var referenceAngle = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break

                        if (!grabbed) {
                            grabbed = event.changes.count { it.pressed } >= 2
                            continue
                        }

                        val tracked = event.changes
                            .filter { it.pressed && it.previousPressed }
                            .sortedBy { it.id.value }
                            .take(2)
                        if (tracked.any { (it.position - it.previousPosition).getDistance() > maxJump }) {
                            pairIds = null
                            continue
                        }

                        val emitted = when (tracked.size) {
                            1 -> {
                                pairIds = null
                                emit(tracked[0].position - tracked[0].previousPosition, 1f, 0f)
                            }

                            2 -> {
                                val (first, second) = tracked
                                val pan = (first.position + second.position) / 2f -
                                    (first.previousPosition + second.previousPosition) / 2f
                                val vector = second.position - first.position
                                val span = vector.getDistance()
                                val angle = vector.angleDegrees()
                                val ids = first.id to second.id

                                // 기준은 최소 폭 아래에서 갱신하지 않는다. 오므렸다 벌려도 배율이 누적 오차 없이 맞는다
                                if (ids != pairIds) {
                                    pairIds = ids
                                    val previous = second.previousPosition - first.previousPosition
                                    referenceSpan = previous.getDistance()
                                    referenceAngle = previous.angleDegrees()
                                }
                                if (span < minSpan) {
                                    false
                                } else if (referenceSpan < minSpan) {
                                    referenceSpan = span
                                    referenceAngle = angle
                                    emit(pan, 1f, 0f)
                                } else {
                                    val zoom = span / referenceSpan
                                    val rotation = normalizeDegrees(angle - referenceAngle)
                                    referenceSpan = span
                                    referenceAngle = angle
                                    emit(pan, zoom, rotation)
                                }
                            }

                            else -> false
                        }
                        if (emitted) event.changes.forEach { it.consume() }
                    }
                } finally {
                    latestOnGestureActiveChange(false)
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}

/** 화면 좌표(y 아래)에서 시계 방향이 양수가 되는 각도 */
private fun Offset.angleDegrees(): Float = Math.toDegrees(-atan2(x, y).toDouble()).toFloat()

private fun normalizeDegrees(degrees: Float): Float {
    var result = degrees
    while (result > 180f) result -= 360f
    while (result < -180f) result += 360f
    return result
}
