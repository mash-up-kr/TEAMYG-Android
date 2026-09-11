package com.teamyg.parfait.data.source.common.mapper

import com.teamyg.parfait.domain.model.topping.ToppingBorder

private const val BORDER_TYPE_SOLID = "SOLID"

/**
 * SOLID 인데 색이나 두께가 빈 행이 이미 저장돼 있을 수 있어, 던지지 않고 테두리 없음으로 접는다.
 */
internal fun toToppingBorder(
    borderType: String?,
    borderColor: String?,
    borderWidth: Double?,
): ToppingBorder {
    if (borderType != BORDER_TYPE_SOLID) return ToppingBorder.None
    val color = borderColor ?: return ToppingBorder.None
    val width = borderWidth ?: return ToppingBorder.None
    return ToppingBorder.solidClamped(color = color, width = width)
}
