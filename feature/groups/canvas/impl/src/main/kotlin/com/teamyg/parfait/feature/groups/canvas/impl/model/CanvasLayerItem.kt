package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/** 캔버스에 겹쳐 그려지는 한 층. 이미지와 텍스트를 한 줄로 세우기 위한 합타입이다 */
internal sealed interface CanvasLayerItem {
    val key: CanvasToppingKey
    val positionZ: Int

    data class Image(val topping: CanvasToppingVO) : CanvasLayerItem {
        override val key get() = CanvasToppingKey.Image(topping.parfaitImageId.value)
        override val positionZ get() = topping.transform.positionZ
    }

    data class Text(val text: CanvasTextToppingVO) : CanvasLayerItem {
        override val key get() = CanvasToppingKey.Text(text.parfaitTextId.value)
        override val positionZ get() = text.transform.positionZ
    }
}
