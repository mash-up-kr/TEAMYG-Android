package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions

/**
 * [frameIndex] 번째 프레임에서 앞에서부터 보이는 토핑 수. 등장 시각은
 * [VideoTimelineOptions.TOPPING_APPEAR_WINDOW_SECONDS] 에 고르게 나누고 첫 토핑은 0초다.
 *
 * float 시각 비교는 경계 프레임에서 반올림 오차로 흔들려 정수로 센다. 경계는 iOS 와 같아야 한다.
 */
internal fun visibleToppingCount(
    toppingCount: Int,
    frameIndex: Int,
): Int {
    if (toppingCount == 0) return 0
    return minOf(toppingCount, frameIndex * toppingCount / VideoTimelineOptions.TOPPING_APPEAR_WINDOW_FRAMES + 1)
}
