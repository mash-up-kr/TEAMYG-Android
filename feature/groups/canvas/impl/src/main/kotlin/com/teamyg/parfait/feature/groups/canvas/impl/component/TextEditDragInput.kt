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
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull

/** 끌기로 인정하는 이동 거리 = `touchSlop` × 이 배수(스펙). 실기기 튜닝 대상 */
private const val TEXT_EDIT_DRAG_SLOP_MULTIPLIER = 2.5f

/**
 * 키보드가 올라와 있는(편집) 동안 텍스트 박스의 3분기 제스처를 가린다(스펙 「키보드 ↑ 제스처 3분기」):
 * **탭** = 커서(시스템), **길게 누르기** = 시스템 선택, **슬롭 이상 끌기** = 키보드 내리며 그 자리에서
 * 이어서 이동.
 *
 * `PointerEventPass.Initial` 에서 먼저 보고 — 슬롭 초과와 롱프레스 타임아웃을 레이스해서, **끌기로
 * 확정됐을 때만** 이벤트를 `consume()` 한다. 탭·롱프레스면 아무것도 소비하지 않고 흘려보내 아래
 * [androidx.compose.foundation.text.BasicTextField] 의 기본 제스처(커서·선택)에 그대로 양보한다.
 *
 * 끌기 확정 시 [onDragStart] 로 키보드를 내리고(편집→배치 전환), 이후 이동 델타를 [onDrag] 로 흘린다.
 * 이 입력은 모드와 무관하게 늘 달려 있어야 한다 — 끌기 도중 편집 모드가 꺼져도(배치로 전환) 진행 중인
 * 제스처가 끊기지 않게. 지금 편집 중인지(그리고 빈 내용이 아닌지)는 [enabled] 가 첫 down 때 한 번 읽는다.
 *
 * @param onDrag `pan` 은 px. 한 손가락 이동만 — 확대·회전은 배치 모드의 핀치가 따로 맡는다.
 */
@Composable
internal fun Modifier.textEditDragInput(
    enabled: () -> Boolean,
    onDragStart: () -> Unit,
    onDrag: (pan: Offset) -> Unit,
): Modifier {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                // 소비 여부를 따지지 않고 가장 먼저(Initial) down 을 본다. 아직 소비하지 않는다
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!latestEnabled()) return@awaitEachGesture

                val dragSlop = viewConfiguration.touchSlop * TEXT_EDIT_DRAG_SLOP_MULTIPLIER
                val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis

                // 슬롭 초과(끌기) vs 롱프레스 타임아웃 레이스. null = 타임아웃(롱프레스),
                // false = 슬롭 전에 뗌(탭), true = 끌기 확정
                var accumulated = Offset.Zero
                val draggedPastSlop = withTimeoutOrNull(longPressTimeoutMs) {
                    var dragged = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || change.changedToUpIgnoreConsumed()) break
                        accumulated += change.positionChange()
                        if (accumulated.getDistance() > dragSlop) {
                            dragged = true
                            break
                        }
                    }
                    dragged
                } ?: false

                // 탭·롱프레스면 아무것도 소비하지 않고 넘긴다 — 필드가 커서/선택을 처리한다
                if (!draggedPastSlop) return@awaitEachGesture

                latestOnDragStart()
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.positionChange()
                    if (delta != Offset.Zero) latestOnDrag(delta)
                    change.consume()
                    if (change.changedToUpIgnoreConsumed()) break
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
