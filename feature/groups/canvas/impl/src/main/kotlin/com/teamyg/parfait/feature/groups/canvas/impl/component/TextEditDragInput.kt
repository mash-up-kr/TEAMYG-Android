package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull

/** 끌기로 인정하는 이동 거리 */
private const val TEXT_EDIT_DRAG_SLOP_MULTIPLIER = 2.5f

/**
 * **탭** = 커서(시스템), **길게 누르기** = 시스템 선택, **한 손가락 슬롭 이상 끌기 또는 두 번째
 * 손가락 도착** = 키보드 내리며 그 자리에서 이어서 이동·확대·회전.
 */
@Composable
internal fun Modifier.textEditDragInput(
    enabled: () -> Boolean,
    canStartSingleFingerDrag: (x: Float, y: Float) -> Boolean,
    onDragStart: () -> Unit,
    onDrag: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
): Modifier {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestCanStartSingleFingerDrag by rememberUpdatedState(canStartSingleFingerDrag)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                // 소비 여부를 따지지 않고 가장 먼저(Initial) down 을 본다. 아직 소비하지 않는다
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!latestEnabled()) return@awaitEachGesture
                val canDragSingleFinger = latestCanStartSingleFingerDrag(down.position.x, down.position.y)

                val dragSlop = viewConfiguration.touchSlop * TEXT_EDIT_DRAG_SLOP_MULTIPLIER
                val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
                val minSpan = TOPPING_PINCH_MIN_SPAN.toPx()
                val maxJump = TOPPING_POINTER_MAX_JUMP.toPx()

                var accumulated = Offset.Zero
                val draggedPastSlop = withTimeoutOrNull(longPressTimeoutMs) {
                    var dragged = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            dragged = true
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || change.changedToUpIgnoreConsumed()) break
                        accumulated += change.positionChange()
                        if (canDragSingleFinger && accumulated.getDistance() > dragSlop) {
                            dragged = true
                            break
                        }
                    }
                    dragged
                } ?: false

                // 탭·롱프레스면 아무것도 소비하지 않고 넘긴다 — 필드가 커서/선택을 처리한다
                if (!draggedPastSlop) return@awaitEachGesture

                latestOnDragStart()

                var pairIds: Pair<PointerId, PointerId>? = null
                var referenceSpan = 0f
                var referenceAngle = 0f
                var pairWasClose = false

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.none { it.pressed }) break

                    val wasClose = pairWasClose
                    val pressed = event.changes.filter { it.pressed }.sortedBy { it.id.value }
                    if (pressed.size >= 2) {
                        pairWasClose = (pressed[1].position - pressed[0].position).getDistance() < minSpan
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
                            val change = tracked[0]
                            if (pairWasClose) {
                                false
                            } else {
                                val delta = change.positionChange()
                                if (delta != Offset.Zero) {
                                    latestOnDrag(delta, 1f, 0f)
                                    true
                                } else {
                                    false
                                }
                            }
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
                                latestOnDrag(pan, 1f, 0f)
                                true
                            } else {
                                val zoom = span / referenceSpan
                                // 붙어 있는 동안 굴린 각도는 버린다
                                val rotation = if (wasClose) 0f else normalizeDegrees(angle - referenceAngle)
                                referenceSpan = span
                                referenceAngle = angle
                                latestOnDrag(pan, zoom, rotation)
                                true
                            }
                        }

                        else -> false
                    }
                    if (emitted) event.changes.forEach { it.consume() }
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
