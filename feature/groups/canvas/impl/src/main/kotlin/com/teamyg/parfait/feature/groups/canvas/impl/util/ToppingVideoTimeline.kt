package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingVideoFrame
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions.ALPHA_COMPLETION_POINT
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions.HOLD_FRAMES
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions.INTRO_FRAMES
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions.OUTRO_FRAMES
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions.POP_FRAMES
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions.POP_OVERSHOOT

/**
 * 토핑 [toppingCount] 개가 하나씩 등장하는 영상의 전체 프레임을 만든다.
 *
 * 마지막 프레임은 **모든 토핑이 팝인을 끝낸 상태**다. 이미지 저장물과 같은 장면이어야 하기 때문이다.
 */
fun toppingVideoFrames(toppingCount: Int): List<ToppingVideoFrame> {
    val frames = ArrayList<ToppingVideoFrame>(
        INTRO_FRAMES + (POP_FRAMES + HOLD_FRAMES) * toppingCount + OUTRO_FRAMES,
    )

    repeat(INTRO_FRAMES) { frames += ToppingVideoFrame(visibleCount = 0, popProgress = 0f) }

    for (index in 0 until toppingCount) {
        val visibleCount = index + 1
        repeat(POP_FRAMES) { step ->
            frames += ToppingVideoFrame(
                visibleCount = visibleCount,
                popProgress = step.toFloat() / POP_FRAMES,
            )
        }
        repeat(HOLD_FRAMES) { frames += ToppingVideoFrame(visibleCount = visibleCount, popProgress = 1f) }
    }

    // visibleCount=0 이면 popProgress=0 이라는 ToppingVideoFrame 불변식을 지킨다
    val outroPopProgress = if (toppingCount > 0) 1f else 0f
    repeat(OUTRO_FRAMES) { frames += ToppingVideoFrame(visibleCount = toppingCount, popProgress = outroPopProgress) }

    return frames
}

/**
 * 팝인 진행도를 배율로 옮긴다(easeOutBack).
 *
 * 1에서 정확히 1이어야 한다 — 어긋나면 마지막 프레임이 이미지 저장물과 다른 크기로 앉는다.
 */
fun toppingPopScale(popProgress: Float): Float {
    val clamped = popProgress.coerceIn(0f, 1f)
    val offset = clamped - 1f
    return 1f + (POP_OVERSHOOT + 1f) * offset * offset * offset + POP_OVERSHOOT * offset * offset
}

fun toppingPopAlpha(popProgress: Float): Float =
    (popProgress.coerceIn(0f, 1f) / ALPHA_COMPLETION_POINT).coerceAtMost(1f)
