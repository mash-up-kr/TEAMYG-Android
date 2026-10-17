package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions

/**
 * [frameIndex] 번째 프레임에서 토핑별 등장 진행도(0~1)를 낸다. 순서는 토핑 목록과 같다.
 *
 * 토핑 [toppingCount] 개의 시작 시각을 [VideoTimelineOptions.TOPPING_APPEAR_WINDOW_SECONDS] 에
 * 고르게 나눈다. 첫 토핑은 0초에 시작한다 — 배경만 보이는 도입 구간이 없다.
 */
internal fun toppingPopProgresses(
    toppingCount: Int,
    frameIndex: Int,
): List<Float> {
    val time = frameIndex.toFloat() / VideoTimelineOptions.FRAMES_PER_SECOND

    return List(toppingCount) { order ->
        val startTime = VideoTimelineOptions.TOPPING_APPEAR_WINDOW_SECONDS * order / toppingCount
        ((time - startTime) / VideoTimelineOptions.TOPPING_POP_DURATION_SECONDS).coerceIn(0f, 1f)
    }
}

/**
 * 팝인 진행도를 배율로 옮긴다. [VideoTimelineOptions.POP_START_SCALE] 에서 시작해 1을 넘겼다 돌아온다.
 *
 * 1에서 정확히 1이어야 한다 — 끝값이 어긋나면 마지막 프레임이 이미지 저장물과 다른 크기로 앉는다.
 */
internal fun toppingPopScale(popProgress: Float): Float {
    val overshoot = VideoTimelineOptions.POP_OVERSHOOT
    val offset = popProgress.coerceIn(0f, 1f) - 1f
    val eased = 1f + (overshoot + 1f) * offset * offset * offset + overshoot * offset * offset
    return VideoTimelineOptions.POP_START_SCALE + (1f - VideoTimelineOptions.POP_START_SCALE) * eased
}

/** 알파는 팝인 전체에 걸쳐 선형으로 오른다 */
internal fun toppingPopAlpha(popProgress: Float): Float = popProgress.coerceIn(0f, 1f)
