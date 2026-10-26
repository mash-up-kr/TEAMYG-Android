package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

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
