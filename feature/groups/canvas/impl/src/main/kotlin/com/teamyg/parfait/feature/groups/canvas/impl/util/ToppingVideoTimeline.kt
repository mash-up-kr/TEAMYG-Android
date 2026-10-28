package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions

/**
 * [frameIndex] 번째 프레임에서 보이는 토핑 수를 낸다. 앞에서부터 이 수만큼 보인다.
 *
 * 토핑 [toppingCount] 개의 등장 시각을 [VideoTimelineOptions.TOPPING_APPEAR_WINDOW_SECONDS] 에
 * 고르게 나눈다. 첫 토핑은 0초에 나타난다 — 배경만 보이는 도입 구간이 없다.
 *
 * 시각을 float 로 비교하면 경계 프레임에서 반올림 오차로 등장이 한 프레임 흔들린다. 그래서 프레임
 * 수의 정수 비교로 센다. 경계는 iOS 와 같아야 한다.
 */
internal fun visibleToppingCount(
    toppingCount: Int,
    frameIndex: Int,
): Int {
    if (toppingCount == 0) return 0
    return minOf(toppingCount, frameIndex * toppingCount / VideoTimelineOptions.TOPPING_APPEAR_WINDOW_FRAMES + 1)
}
