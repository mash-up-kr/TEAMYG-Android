package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DELTA = 1e-4f

class TextToppingSizingTest {
    @Test
    fun fontSize_designCanvasWidth_keepsDesignValue() {
        assertEquals(24f, textToppingFontSizeSp(designFontSize = 24f, canvasWidth = 335.dp), DELTA)
    }

    @Test
    fun fontSize_scalesWithCanvasWidth() {
        // 670 = 335 × 2 → 디자인 값의 두 배
        assertEquals(24f, textToppingFontSizeSp(designFontSize = 12f, canvasWidth = 670.dp), DELTA)
    }

    @Test
    fun wrapWidth_maxDesignFontSize_isCanvasWidth() {
        assertEquals(360f, textToppingWrapWidth(designFontSize = 24f, canvasWidth = 360.dp).value, DELTA)
    }

    @Test
    fun wrapWidth_shrinksWithDesignFontSize() {
        // 12 / 24 = 절반
        assertEquals(180f, textToppingWrapWidth(designFontSize = 12f, canvasWidth = 360.dp).value, DELTA)
    }
}
