package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.graphics.toArgb
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.android.extension.toRgbHexString
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle

internal const val DEFAULT_TOPPING_BORDER_WIDTH_DP = 10f

internal val TOPPING_BORDER_WIDTH_RANGE_DP: ClosedFloatingPointRange<Float> =
    ToppingBorder.WIDTH_RANGE_DP.start.toFloat()..ToppingBorder.WIDTH_RANGE_DP.endInclusive.toFloat()

internal fun ToppingBorderStyle?.toToppingBorder(): ToppingBorder = if (this == null) {
    ToppingBorder.None
} else {
    ToppingBorder.Solid(color = colorArgb.toRgbHexString(), width = widthDp.toDouble())
}

/** 서버가 준 색을 못 읽으면 null 이다. 못 읽는 색에 임의의 색을 입히지 않는다 */
internal fun ToppingBorder.toToppingBorderStyleOrNull(): ToppingBorderStyle? = when (this) {
    ToppingBorder.None -> null

    is ToppingBorder.Solid -> color.toColorOrNull()?.let {
        ToppingBorderStyle(colorArgb = it.toArgb(), widthDp = width.toFloat())
    }
}
