package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 화면이 들고 있는 테두리. "테두리 없음"은 이 값이 null 인 것으로 나타내서, 색 없이 굵기만 실린
 * 상태는 만들 수 없다.
 */
data class ToppingBorderStyle(
    val colorArgb: Int,
    val widthDp: Float,
)
