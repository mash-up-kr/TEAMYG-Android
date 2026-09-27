package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 토핑 하나가 나타나는 데 쓰는 단계 수. 값이 클수록 더 부드럽지만 프레임(=인코딩 비용)이 늘어난다. */
internal const val CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING = 6

/** 캡처한 프레임 하나가 완성된 동영상에서 머무는 시간. */
internal const val CANVAS_VIDEO_FRAME_DURATION_MS = 60L

/** 마지막 토핑까지 다 나타난 뒤, 화면이 바로 끝나지 않도록 더 유지하는 정지 프레임 수. */
internal const val CANVAS_VIDEO_FINAL_HOLD_FRAMES = 10

/**
 * 토핑 하나가 나타나는 동안 캡처할 진행도 목록. 0은 포함하지 않는다 — 그 상태는 이전 토핑의
 * 마지막 프레임(또는 배경만 있는 시작 프레임)이 이미 담고 있다.
 */
internal fun canvasVideoRevealSteps(stepsPerTopping: Int): List<Float> =
    (1..stepsPerTopping).map { step -> step / stepsPerTopping.toFloat() }
