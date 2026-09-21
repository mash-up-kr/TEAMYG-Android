package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
const val CANVAS_VIDEO_FRAMES_PER_SECOND: Int = 30

private const val INTRO_FRAMES = 15
private const val POP_FRAMES = 8
private const val HOLD_FRAMES = 4
private const val OUTRO_FRAMES = 30

/**
 * 되돌아오기 전에 1을 넘기는 정도.
 *
 * easeOutBack 의 정점은 `4c³ / (27(c+1)²)` 이고(c 는 이 상수), 스펙(`scale: 0 → 1.08 → 1.0`)이
 * 정점을 정확히 0.08 로 못 박아 뒀다. `c = 1.5` 를 대입하면 `4·1.5³ / (27·2.5²) = 13.5/168.75 = 0.08`
 * 로 정확히 맞아떨어져 이 값을 쓴다 — 표준 easeOutBack 계수인 1.70158 은 정점이 약 0.100 이 되어
 * 스펙과 어긋난다
 */
private const val POP_OVERSHOOT = 1.5f

/** 알파는 팝인 앞 절반에서 끝난다 — 크기가 자리 잡기 전에 색이 먼저 서야 튀어 오르는 인상이 난다 */
private const val ALPHA_COMPLETION_POINT = 0.5f

/**
 * 한 프레임의 상태.
 *
 * @param visibleCount 이 프레임에 그려지는 토핑 개수. 등장 순서대로 앞에서 센다
 * @param popProgress 마지막으로 등장한 토핑의 팝인 진행도(0~1). [visibleCount] 가 0이면 0이다
 */
data class ToppingVideoFrame(
    val visibleCount: Int,
    val popProgress: Float,
)

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

    // 토핑이 하나도 없으면 팝인 자체가 없었으므로 "진행도"라는 값이 의미를 갖지 않는다 — 0으로 둬야
    // ToppingVideoFrame 의 KDoc 불변식(visibleCount=0 이면 popProgress=0)이 outro 구간에서도 성립한다
    val outroPopProgress = if (toppingCount > 0) 1f else 0f
    repeat(OUTRO_FRAMES) { frames += ToppingVideoFrame(visibleCount = toppingCount, popProgress = outroPopProgress) }

    return frames
}

/**
 * 팝인 진행도를 배율로 옮긴다. 1을 넘겼다 돌아오는 곡선이라 튀어 오르는 인상이 난다.
 *
 * 0에서 정확히 0, 1에서 정확히 1이어야 한다 — 끝값이 어긋나면 마지막 프레임이 이미지 저장물과
 * 다른 크기로 앉는다.
 */
fun toppingPopScale(popProgress: Float): Float {
    val clamped = popProgress.coerceIn(0f, 1f)
    // easeOutBack — t=0 에서 0, t=1 에서 정확히 1이면서 중간에 1을 넘긴다. 끝값이 어긋나면
    // 마지막 프레임이 이미지 저장물과 다른 크기로 앉는다
    val offset = clamped - 1f
    return 1f + (POP_OVERSHOOT + 1f) * offset * offset * offset + POP_OVERSHOOT * offset * offset
}

/** 알파는 [ALPHA_COMPLETION_POINT] 에서 이미 1이다 */
fun toppingPopAlpha(popProgress: Float): Float =
    (popProgress.coerceIn(0f, 1f) / ALPHA_COMPLETION_POINT).coerceAtMost(1f)
