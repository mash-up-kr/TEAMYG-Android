package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import kotlin.math.atan2

/** 두 손가락이 이보다 붙으면 거리비가 흔들리고 터치 패널이 두 손가락을 합쳐 좌표가 튄다 */
internal val TOPPING_PINCH_MIN_SPAN = 64.dp

/** 한 이벤트에 한 포인터가 이보다 멀리 움직이면 터치 패널이 추적을 놓친 것으로 본다 */
internal val TOPPING_POINTER_MAX_JUMP = 48.dp

/**
 * 한 손가락은 [targetAt] 실루엣 안에서 시작했을 때만 옮기고, 두 손가락은 어디서 시작했든
 * 옮기기·회전·확대를 한 번에 한다. 핀치 중 한 손가락을 떼면 남은 손가락으로 계속 옮긴다.
 *
 * 직전 이벤트에도 눌려 있던 포인터만 센다. 새로 down 됐거나 막 뗀 포인터를 넣으면 포인터 수가
 * 바뀌는 순간 중점이 튄다. 짝은 id 가 작은 두 포인터다 — `PointerId` 는 닿은 순서로 늘고 재사용되지
 * 않아서 먼저 닿은 두 손가락이 된다.
 *
 * @param onTransform `pan` 은 px, `rotationDelta` 는 `rotationZ` 와 같은 방향의 도 단위.
 * @param enabled 첫 down 때 한 번만 읽는다. 거짓이면 뒤늦게 닿는 손가락까지 그 제스처를 통째로
 *   버린다. 입력을 체인에서 빼는 대신 이것으로 끈다 — [dismissPanelOnTouch] 참고.
 */
@Composable
internal fun Modifier.toppingTransformInput(
    targetAt: () -> ToppingHitTarget?,
    onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit = {},
    enabled: () -> Boolean = { true },
): Modifier {
    val latestTargetAt by rememberUpdatedState(targetAt)
    val latestOnTransform by rememberUpdatedState(onTransform)
    val latestOnGestureActiveChange by rememberUpdatedState(onGestureActiveChange)
    val latestEnabled by rememberUpdatedState(enabled)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!latestEnabled()) return@awaitEachGesture
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
                    val minSpan = TOPPING_PINCH_MIN_SPAN.toPx()
                    val maxJump = TOPPING_POINTER_MAX_JUMP.toPx()
                    val touchSlop = viewConfiguration.touchSlop
                    val canDrag = target.containsPoint(down.position.x, down.position.y)
                    var grabbed = false
                    var slopAccum = Offset.Zero
                    var pairIds: Pair<PointerId, PointerId>? = null
                    var referenceSpan = 0f
                    var referenceAngle = 0f
                    var pairWasClose = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break

                        val wasClose = pairWasClose
                        val pressed = event.changes.filter { it.pressed }.sortedBy { it.id.value }
                        if (pressed.size >= 2) {
                            pairWasClose = (pressed[1].position - pressed[0].position).getDistance() < minSpan
                        }

                        if (!grabbed) {
                            // awaitTouchSlopOrCancellation 은 두 번째 down 에 반환하지 않아서 직접 기다린다
                            if (pressed.size >= 2) {
                                grabbed = true
                            } else if (canDrag) {
                                val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                slopAccum += change.positionChange()
                                val distance = slopAccum.getDistance()
                                if (distance > touchSlop) {
                                    grabbed = true
                                    // 슬롭을 넘긴 몫을 버리면 한 프레임씩 손가락 뒤로 처진다
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
                        if (tracked.any { (it.position - it.previousPosition).getDistance() > maxJump }) {
                            pairIds = null
                            continue
                        }

                        val emitted = when (tracked.size) {
                            1 -> {
                                pairIds = null
                                // 붙어 있던 짝에서 한쪽이 떨어졌다면 패널이 두 접점을 합친 것일 수 있다
                                val change = tracked[0]
                                if (pairWasClose) false else emit(change.position - change.previousPosition, 1f, 0f)
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
                                    // 붙어 있는 동안 굴린 각도는 버린다
                                    val rotation = if (wasClose) 0f else normalizeDegrees(angle - referenceAngle)
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
