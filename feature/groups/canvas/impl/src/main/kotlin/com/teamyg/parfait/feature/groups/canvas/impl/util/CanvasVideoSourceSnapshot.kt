package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/**
 * 동영상 프레임 캡처에 필요한 배경·토핑 스냅샷. [toppings] 는 `positionZ` 오름차순이어야 한다 —
 * [CanvasVideoSourceHolder] 를 채우는 쪽이 그 순서를 보장한다.
 */
internal data class CanvasVideoSourceSnapshot(
    val background: YGCanvasBackground?,
    val toppings: List<CanvasToppingVO>,
)
