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
import androidx.compose.ui.input.pointer.positionChange
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingClickThrottle
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.pickToppingHit
import kotlin.math.atan2

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

                // up 없이 끝나면(변환 입력이 이동을 소비했거나 손가락이 벗어남) 아무 콜백도 부르지 않는다
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
 * [waitForUpOrCancellation] 과 같은 조건으로 [downId] 의 up 을 기다리되, 다른 손가락이 눌리면
 * 탭이 아니므로 `null` 이다. 두 손가락을 대고 움직이지 않은 채 떼면 아무도 소비하지 않아서
 * 이 조건이 없으면 탭으로 잡힌다.
 */
private suspend fun AwaitPointerEventScope.waitForTapUp(downId: PointerId): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.any { it.id != downId && it.pressed }) return null

        val change = event.changes.firstOrNull { it.id == downId } ?: return null
        if (change.changedToUp()) return change
        if (change.isConsumed || change.isOutOfBounds(size, extendedTouchPadding)) return null

        // 같은 이벤트를 뒤 단계에서 소비한 쪽이 있는지도 본다 — waitForUpOrCancellation 과 같다
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.any { it.id == downId && it.isConsumed }) return null
    }
}

/**
 * 인스타그램 스토리식 변환 입력. 한 손가락은 [targetAt] 의 실루엣 안에서 시작했을 때만 옮기고,
 * 두 손가락은 어디서 시작했든 옮기기·회전·확대를 한 번에 한다.
 *
 * 매 이벤트에서 직전 이벤트에도 눌려 있던 포인터만 셈한다. 새로 down 됐거나 막 뗀 포인터를
 * 그 이벤트에 넣으면 포인터 수가 바뀌는 순간 중점이 튄다. 세 번째 이후 손가락은 무시한다.
 *
 * @param onTransform `pan` 은 px, `zoom` 은 직전 대비 배율, `rotationDelta` 는 시계 방향이 양수인
 *   도 단위다(`graphicsLayer` 의 `rotationZ` 와 같은 방향). 항등 변환이면 부르지 않는다.
 * @param onGestureActiveChange 첫 down 에서 [targetAt] 이 있으면 `true`, 그 제스처가 끝나거나
 *   취소되면 `false` 를 한 번씩 받는다.
 */
@Composable
internal fun Modifier.toppingTransformInput(
    targetAt: () -> ToppingHitTarget?,
    onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit = {},
): Modifier {
    val latestTargetAt by rememberUpdatedState(targetAt)
    val latestOnTransform by rememberUpdatedState(onTransform)
    val latestOnGestureActiveChange by rememberUpdatedState(onGestureActiveChange)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val target = latestTargetAt() ?: return@awaitEachGesture

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
                    val touchSlop = viewConfiguration.touchSlop
                    val canDrag = target.containsPoint(down.position.x, down.position.y)
                    var grabbed = false
                    var slopAccum = Offset.Zero

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break

                        if (!grabbed) {
                            // awaitTouchSlopOrCancellation 은 두 번째 down 에 반환하지 않아서 직접 기다린다
                            if (event.changes.count { it.pressed } >= 2) {
                                grabbed = true
                            } else if (canDrag) {
                                val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                slopAccum += change.positionChange()
                                val distance = slopAccum.getDistance()
                                if (distance > touchSlop) {
                                    grabbed = true
                                    // 슬롭을 넘긴 만큼은 첫 델타로 보낸다. 버리면 한 프레임씩 손가락 뒤로 처진다
                                    emit(slopAccum - slopAccum / distance * touchSlop, 1f, 0f)
                                    change.consume()
                                }
                            }
                            continue
                        }

                        val tracked = event.changes
                            .filter { it.pressed && it.previousPressed }
                            .sortedBy { it.id.value }
                            .take(2)
                        val emitted = when (tracked.size) {
                            1 -> emit(tracked[0].position - tracked[0].previousPosition, 1f, 0f)

                            2 -> {
                                val (first, second) = tracked
                                val current = second.position - first.position
                                val previous = second.previousPosition - first.previousPosition
                                val previousDistance = previous.getDistance()
                                val pan = (first.position + second.position) / 2f -
                                    (first.previousPosition + second.previousPosition) / 2f
                                // 직전 손가락 간 거리가 슬롭보다 가까우면 위치 오차 몇 px 만으로 거리비·각도가
                                // 크게 튄다(0 나눗셈 방지 가드로는 못 막던 자리). 이동은 그대로 두고
                                // 확대·회전만 이 프레임에서 항등으로 묶는다
                                if (previousDistance < touchSlop) {
                                    emit(pan, 1f, 0f)
                                } else {
                                    emit(
                                        pan,
                                        current.getDistance() / previousDistance,
                                        normalizeDegrees(current.angleDegrees() - previous.angleDegrees()),
                                    )
                                }
                            }

                            else -> false
                        }
                        // 움직임 없는 이벤트는 소비하지 않는다. 두 손가락을 대기만 하고 떼는 경우의 탭 취소는
                        // toppingTapInput 이 스스로 한다
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
