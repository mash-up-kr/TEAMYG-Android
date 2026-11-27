package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/**
 * 새 토핑은 언제나 맨 위다. 목록 크기로 세면 지워진 토핑이 있는 캔버스에서 z 가 겹친다.
 * 이미지와 텍스트가 한 z 축을 쓰므로 둘을 합쳐 센다.
 */
internal fun nextToppingPositionZ(
    toppings: List<CanvasToppingVO>,
    texts: List<CanvasTextToppingVO>,
): Int {
    val maxZ = (toppings.map { it.transform.positionZ } + texts.map { it.transform.positionZ }).maxOrNull()
    return (maxZ ?: 0) + 1
}
