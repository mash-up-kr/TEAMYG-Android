package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToppingBorderStyleTest {
    @Test
    fun toToppingBorder_null_isNone() =
        assertEquals(ToppingBorder.None, (null as ToppingBorderStyle?).toToppingBorder())

    @Test
    fun toToppingBorder_style_isSolidWithRgbHex() = assertEquals(
        ToppingBorder.Solid(color = "#FCC2CC", width = 12.0),
        ToppingBorderStyle(colorArgb = 0xFFFCC2CC.toInt(), widthDp = 12f).toToppingBorder(),
    )

    @Test
    fun toToppingBorderStyleOrNull_unreadableColor_isNull() =
        assertNull(ToppingBorder.Solid(color = "not-a-color", width = 4.0).toToppingBorderStyleOrNull())

    @Test
    fun toToppingBorderStyleOrNull_none_isNull() = assertNull(ToppingBorder.None.toToppingBorderStyleOrNull())

    @Test
    fun toToppingBorderStyleOrNull_solid_roundTrips() {
        val style = ToppingBorderStyle(colorArgb = 0xFFFCC2CC.toInt(), widthDp = 12f)

        assertEquals(style, style.toToppingBorder().toToppingBorderStyleOrNull())
    }

    @Test
    fun widthRange_matchesDomainRange() {
        assertEquals(2f, TOPPING_BORDER_WIDTH_RANGE_DP.start)
        assertEquals(30f, TOPPING_BORDER_WIDTH_RANGE_DP.endInclusive)
    }
}
