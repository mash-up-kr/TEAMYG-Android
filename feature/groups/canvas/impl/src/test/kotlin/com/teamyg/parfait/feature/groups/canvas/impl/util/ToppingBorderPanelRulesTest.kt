package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToppingBorderPanelRulesTest {
    private val border = ToppingBorderStyle(colorArgb = 0xFF112233.toInt(), widthDp = 12f)

    @Test
    fun toppingPanelBackAction_ignoresWhileLoading_evenWhenPanelIsOpen() {
        assertEquals(
            ToppingPanelBackAction.Ignore,
            toppingPanelBackAction(isLoading = true, isBorderPanelOpen = true),
        )
        assertEquals(
            ToppingPanelBackAction.Ignore,
            toppingPanelBackAction(isLoading = true, isBorderPanelOpen = false),
        )
    }

    @Test
    fun toppingPanelBackAction_closesPanel_whenPanelIsOpen() {
        assertEquals(
            ToppingPanelBackAction.ClosePanel,
            toppingPanelBackAction(isLoading = false, isBorderPanelOpen = true),
        )
    }

    @Test
    fun toppingPanelBackAction_showsQuitDialog_whenPanelIsClosed() {
        assertEquals(
            ToppingPanelBackAction.NavigateBack,
            toppingPanelBackAction(isLoading = false, isBorderPanelOpen = false),
        )
    }

    @Test
    fun ignoresToppingTransform_onlyWhilePanelIsOpen() {
        assertTrue(ignoresToppingTransform(isBorderPanelOpen = true))
        assertFalse(ignoresToppingTransform(isBorderPanelOpen = false))
    }

    @Test
    fun resolvePanelBorderWidthDp_isBorderWidth_whenBorderExists() {
        assertEquals(12f, resolvePanelBorderWidthDp(border = border, pendingWidthDp = 20f))
    }

    @Test
    fun resolvePanelBorderWidthDp_isPendingWidth_whenThereIsNoBorder() {
        assertEquals(20f, resolvePanelBorderWidthDp(border = null, pendingWidthDp = 20f))
    }

    @Test
    fun clampPanelBorderWidthDp_coercesIntoRange() {
        val range = TOPPING_BORDER_WIDTH_RANGE_DP
        val inside = (range.start + range.endInclusive) / 2

        assertEquals(range.start, clampPanelBorderWidthDp(range.start - 1f))
        assertEquals(range.endInclusive, clampPanelBorderWidthDp(range.endInclusive + 1f))
        assertEquals(inside, clampPanelBorderWidthDp(inside))
    }

    @Test
    fun panelBorderForColor_isNull_whenColorIsNull() {
        assertNull(panelBorderForColor(colorArgb = null, panelWidthDp = 12f))
    }

    @Test
    fun panelBorderForColor_usesPanelWidth() {
        assertEquals(
            ToppingBorderStyle(colorArgb = 0xFF445566.toInt(), widthDp = 20f),
            panelBorderForColor(colorArgb = 0xFF445566.toInt(), panelWidthDp = 20f),
        )
    }

    @Test
    fun withPanelWidth_changesOnlyWidth_whenBorderExists() {
        assertEquals(border.copy(widthDp = 4f), border.withPanelWidth(4f))
    }

    @Test
    fun withPanelWidth_staysNull_whenThereIsNoBorder() {
        val none: ToppingBorderStyle? = null

        assertNull(none.withPanelWidth(4f))
    }
}
