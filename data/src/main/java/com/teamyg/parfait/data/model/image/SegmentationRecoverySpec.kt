package com.teamyg.parfait.data.model.image

internal object SegmentationRecoverySpec {
    /** ML Kit 가이드가 "at least 512x512" 를 적는다 */
    const val DETECTION_MIN_SHORT_SIDE = 512

    /** 문서 근거가 아니라 자원에서 나온 값이다. 판을 네 번 추론하므로 피크를 여기서 막는다 */
    const val DETECTION_MAX_LONG_SIDE = 2048

    /** 테스트가 구현의 오차에 맞춰지지 않게 여기서 고정한다 */
    const val ROUND_TRIP_TOLERANCE_PX = 1

    const val FOCUS_MARGIN_RATIO = 0.20f

    /** 정수로 비교한다. 부동소수 비율이면 정확히 70% 인 경계가 반올림 오차로 흔들린다 */
    const val FOCUS_SHRINK_CEILING_PERCENT = 70

    const val CENTER_CROP_RATIO = 0.70f
}
