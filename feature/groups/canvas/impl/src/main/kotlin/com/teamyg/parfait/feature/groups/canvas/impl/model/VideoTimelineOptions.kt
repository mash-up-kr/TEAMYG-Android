package com.teamyg.parfait.feature.groups.canvas.impl.model

object VideoTimelineOptions {
    /** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
    const val FRAMES_PER_SECOND: Int = 30

    const val INTRO_FRAMES: Int = 15
    const val POP_FRAMES: Int = 8
    const val HOLD_FRAMES: Int = 4
    const val OUTRO_FRAMES: Int = 30

    /**
     * easeOutBack 계수. 정점 `1 + 4c³ / (27(c+1)²)` 이 스펙의 1.08 이 되는 값이다.
     * 표준 계수 1.70158 은 정점이 약 1.10 이라 쓰지 않는다
     */
    const val POP_OVERSHOOT: Float = 1.5f

    /** 알파는 팝인 앞 절반에서 끝난다 — 크기가 자리 잡기 전에 색이 먼저 서야 튀어 오르는 인상이 난다 */
    const val ALPHA_COMPLETION_POINT: Float = 0.5f
}
