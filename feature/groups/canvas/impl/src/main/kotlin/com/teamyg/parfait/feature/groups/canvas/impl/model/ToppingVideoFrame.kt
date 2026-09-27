package com.teamyg.parfait.feature.groups.canvas.impl.model

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
