package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ToppingPanelFocusTest {
    @Test
    fun panelFocusCenter_isHorizontalCenterLiftedAboveVerticalCenter() {
        val center = panelFocusCenter(DpSize(335.dp, 596.dp))

        assertEquals(167.5.dp, center.x)
        assertEquals(279.5.dp, center.y)
    }
}
