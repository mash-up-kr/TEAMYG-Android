package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 타임랩스 영상의 타임라인 수치. iOS 저장본(`CanvasImageExporter`)과 같은 값이어야 한다 */
internal object VideoTimelineOptions {
    /** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
    const val FRAMES_PER_SECOND: Int = 30

    /** 영상 길이는 토핑 수와 무관하다 */
    const val DURATION_SECONDS: Float = 2f

    /** 모든 토핑이 이 구간 안에서 등장을 **시작**한다. 남는 시간은 완성본을 보여 준다 */
    const val TOPPING_APPEAR_WINDOW_SECONDS: Float = 1.5f

    /** 토핑이 많으면 앞뒤 토핑의 팝인이 겹친다 */
    const val TOPPING_POP_DURATION_SECONDS: Float = 0.25f

    /** 등장을 시작할 때의 배율. 0 이 아니라 여기서부터 커진다 */
    const val POP_START_SCALE: Float = 0.6f

    /** 표준 easeOutBack 계수 */
    const val POP_OVERSHOOT: Float = 1.70158f

    const val FRAME_COUNT: Int = (DURATION_SECONDS * FRAMES_PER_SECOND).toInt()
}
