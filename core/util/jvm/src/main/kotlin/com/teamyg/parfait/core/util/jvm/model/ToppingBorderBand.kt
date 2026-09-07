package com.teamyg.parfait.core.util.jvm.model

/**
 * 테두리 한 겹이 차지하는 구간.
 *
 * @param outsetPx 실루엣에서 이 겹의 바깥 끝까지 거리. 겹은 아래 겹을 감싸며 쌓이므로
 *   자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다
 */
data class ToppingBorderBand(
    val outsetPx: Float,
    val colorArgb: Int,
)
