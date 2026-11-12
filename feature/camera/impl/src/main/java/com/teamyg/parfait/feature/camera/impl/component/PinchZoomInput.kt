package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.abs

/**
 * 두 손가락일 때만 배율 변화를 넘긴다. `detectTransformGestures`는 한 손가락 이동도 소비해 탭 초점과 겹친다.
 * 손가락 사이 거리가 터치 슬롭만큼 벌어지기 전의 떨림은 줌으로 치지 않는다.
 */
internal suspend fun PointerInputScope.detectPinchZoom(
    onPinchZoomStart: () -> Unit,
    onPinchZoom: (scale: Float) -> Unit,
    onPinchZoomEnd: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var isPinching = false
        var slopZoom = 1f
        var isPastSlop = false
        try {
            do {
                val event = awaitPointerEvent()
                val isPinchEvent = event.changes.count { it.pressed } >= MIN_PINCH_POINTERS
                if (isPinchEvent && !isPinching) {
                    onPinchZoomStart()
                    slopZoom = 1f
                    isPastSlop = false
                }
                if (!isPinchEvent && isPinching) onPinchZoomEnd()
                isPinching = isPinchEvent
                if (!isPinchEvent) continue

                val zoom = event.calculateZoom()
                if (isPastSlop) {
                    if (zoom != 1f) onPinchZoom(zoom)
                } else {
                    slopZoom *= zoom
                    if (!event.isPastTouchSlop(slopZoom, viewConfiguration.touchSlop)) continue
                    isPastSlop = true
                    // 슬롭 안에서 쌓인 배율을 버리면 손가락보다 줌이 뒤처진다
                    onPinchZoom(slopZoom)
                }
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        } finally {
            // 취소된 제스처도 끝을 알려야 대기 중인 렌즈 전환이 손을 뗀 뒤에 터지지 않는다
            if (isPinching) onPinchZoomEnd()
        }
    }
}

private fun PointerEvent.isPastTouchSlop(
    zoom: Float,
    touchSlop: Float,
): Boolean = abs(1f - zoom) * calculateCentroidSize(useCurrent = false) > touchSlop

private const val MIN_PINCH_POINTERS = 2
