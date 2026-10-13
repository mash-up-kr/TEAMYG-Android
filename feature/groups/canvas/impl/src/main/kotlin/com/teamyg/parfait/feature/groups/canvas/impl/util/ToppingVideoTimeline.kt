package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
const val CANVAS_VIDEO_FRAMES_PER_SECOND: Int = 30

/** 영상 길이는 토핑 수와 무관하게 2초다 — iOS `CanvasImageExporter.videoDuration` 과 같은 값 */
private const val VIDEO_DURATION_SECONDS = 2f

/** 모든 토핑이 이 구간 안에서 등장을 **시작**한다. 남는 시간은 완성본을 보여 준다 */
private const val TOPPING_APPEAR_WINDOW_SECONDS = 1.5f

/** 토핑 하나가 튀어나오는 데 걸리는 시간. 토핑이 많으면 앞뒤 토핑의 팝인이 겹친다 */
private const val TOPPING_POP_DURATION_SECONDS = 0.25f

/** 등장을 시작할 때의 배율. 0 이 아니라 여기서부터 커진다 */
private const val POP_START_SCALE = 0.6f

/** 표준 easeOutBack 계수. iOS 저장본과 같은 곡선이어야 해서 값을 맞춘다 */
private const val POP_OVERSHOOT = 1.70158f

const val CANVAS_VIDEO_FRAME_COUNT: Int = (VIDEO_DURATION_SECONDS * CANVAS_VIDEO_FRAMES_PER_SECOND).toInt()

/**
 * [frameIndex] 번째 프레임에서 토핑별 등장 진행도(0~1)를 낸다. 순서는 토핑 목록과 같다.
 *
 * 토핑 [toppingCount] 개의 시작 시각을 [TOPPING_APPEAR_WINDOW_SECONDS] 에 고르게 나눈다.
 * 첫 토핑은 0초에 시작한다 — 배경만 보이는 도입 구간이 없다.
 */
fun toppingPopProgresses(
    toppingCount: Int,
    frameIndex: Int,
): List<Float> {
    val time = frameIndex.toFloat() / CANVAS_VIDEO_FRAMES_PER_SECOND

    return List(toppingCount) { order ->
        val startTime = TOPPING_APPEAR_WINDOW_SECONDS * order / toppingCount
        ((time - startTime) / TOPPING_POP_DURATION_SECONDS).coerceIn(0f, 1f)
    }
}

/**
 * 팝인 진행도를 배율로 옮긴다. [POP_START_SCALE] 에서 시작해 1을 넘겼다 돌아온다.
 *
 * 1에서 정확히 1이어야 한다 — 끝값이 어긋나면 마지막 프레임이 이미지 저장물과 다른 크기로 앉는다.
 */
fun toppingPopScale(popProgress: Float): Float {
    val offset = popProgress.coerceIn(0f, 1f) - 1f
    val eased = 1f + (POP_OVERSHOOT + 1f) * offset * offset * offset + POP_OVERSHOOT * offset * offset
    return POP_START_SCALE + (1f - POP_START_SCALE) * eased
}

/** 알파는 팝인 전체에 걸쳐 선형으로 오른다 */
fun toppingPopAlpha(popProgress: Float): Float = popProgress.coerceIn(0f, 1f)
