package com.teamyg.parfait.feature.groups.canvas.impl.model

object VideoTimelineOptions {
    /** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
    const val FRAMES_PER_SECOND: Int = 30

    const val INTRO_FRAMES: Int = 15
    const val POP_FRAMES: Int = 8
    const val HOLD_FRAMES: Int = 4
    const val OUTRO_FRAMES: Int = 30

    /**
     * 되돌아오기 전에 1을 넘기는 정도.
     *
     * easeOutBack 의 정점은 `4c³ / (27(c+1)²)` 이고(c 는 이 상수), 스펙(`scale: 0 → 1.08 → 1.0`)이
     * 정점을 정확히 0.08 로 못 박아 뒀다. `c = 1.5` 를 대입하면 `4·1.5³ / (27·2.5²) = 13.5/168.75 = 0.08`
     * 로 정확히 맞아떨어져 이 값을 쓴다 — 표준 easeOutBack 계수인 1.70158 은 정점이 약 0.100 이 되어
     * 스펙과 어긋난다
     */
    const val POP_OVERSHOOT: Float = 1.5f

    /** 알파는 팝인 앞 절반에서 끝난다 — 크기가 자리 잡기 전에 색이 먼저 서야 튀어 오르는 인상이 난다 */
    const val ALPHA_COMPLETION_POINT: Float = 0.5f
}
