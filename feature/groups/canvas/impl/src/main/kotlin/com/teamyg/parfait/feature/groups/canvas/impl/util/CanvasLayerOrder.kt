package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasLayerItem

/**
 * 아래에서 위로: `positionZ` 오름차순, 같으면 이미지가 아래, 그다음 id 오름차순.
 * 편집 쪽 타입도 같은 규칙을 쓰도록 접근자만 받는다.
 */
internal fun <T> canvasLayerComparator(
    positionZ: (T) -> Int,
    isText: (T) -> Boolean,
    id: (T) -> Long,
): Comparator<T> = compareBy<T> { positionZ(it) }.thenBy { isText(it) }.thenBy { id(it) }

internal fun canvasLayerOrder(
    images: List<CanvasToppingVO>,
    texts: List<CanvasTextToppingVO>,
): List<CanvasLayerItem> {
    val items = images.map { CanvasLayerItem.Image(it) } + texts.map { CanvasLayerItem.Text(it) }
    return items.sortedWith(
        canvasLayerComparator(
            positionZ = { it.positionZ },
            isText = { it is CanvasLayerItem.Text },
            id = { item ->
                when (item) {
                    is CanvasLayerItem.Image -> item.topping.parfaitImageId.value
                    is CanvasLayerItem.Text -> item.text.parfaitTextId.value
                }
            },
        ),
    )
}
