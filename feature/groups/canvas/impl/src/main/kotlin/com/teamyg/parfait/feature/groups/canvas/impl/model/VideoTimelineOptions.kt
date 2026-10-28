package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 타임랩스 영상의 타임라인 수치. 값의 기준은 스펙 `canvas-topping-timelapse-video` 「수치의 기준」 */
internal object VideoTimelineOptions {
    /** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
    const val FRAMES_PER_SECOND: Int = 30

    /** 영상 길이는 토핑 수와 무관하다 */
    const val DURATION_SECONDS: Float = 2f

    /** 모든 토핑이 이 구간 안에서 나타난다. 남는 시간은 완성본을 보여 준다 */
    const val TOPPING_APPEAR_WINDOW_SECONDS: Float = 1.5f

    const val TOPPING_APPEAR_WINDOW_FRAMES: Int = (TOPPING_APPEAR_WINDOW_SECONDS * FRAMES_PER_SECOND).toInt()

    const val FRAME_COUNT: Int = (DURATION_SECONDS * FRAMES_PER_SECOND).toInt()
}
