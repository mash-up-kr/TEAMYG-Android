package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import kotlin.test.Test
import kotlin.test.assertEquals

class TextToppingWireMapperTest {
    @Test
    fun color_roundTrips() {
        TextToppingColor.entries.forEach { assertEquals(it, textToppingColorOf(it.toWireColor())) }
    }

    @Test
    fun color_readsCaseInsensitively() {
        assertEquals(TextToppingColor.Pink, textToppingColorOf("#fa91a2"))
    }

    @Test
    fun color_unknownFallsBackToBlack() {
        assertEquals(TextToppingColor.Black, textToppingColorOf("rgb(1,2,3)"))
    }

    @Test
    fun color_writesUppercaseRgb() {
        assertEquals("#FAFAFA", TextToppingColor.White.toWireColor())
        assertEquals("#FA91A2", TextToppingColor.Pink.toWireColor())
    }

    @Test
    fun type_roundTrips() {
        TextToppingType.entries.forEach {
            assertEquals(it, TextToppingType.of(isFill = it.hasFill, isStroke = it.hasBorder))
        }
    }

    @Test
    fun type_strokeWithoutFill_isDefault() {
        assertEquals(TextToppingType.DEFAULT, TextToppingType.of(isFill = false, isStroke = true))
    }

    @Test
    fun style_carriesDesignFontSizeAndFlags() {
        val style = TextToppingStyle(TextToppingColor.Blue, TextToppingType.FILL_STROKE, 18f)

        assertEquals("#6DBBF2", style.color)
        assertEquals(18.0, style.fontSize)
        assertEquals(true, style.isFill)
        assertEquals(true, style.isStroke)
    }

    @Test
    fun transform_usesBoxCenterRatio() {
        val transform = toTextToppingTransform(
            offsetX = 100.dp,
            offsetY = 50.dp,
            boxSize = DpSize(40.dp, 20.dp),
            canvasSize = DpSize(200.dp, 400.dp),
            scale = 1.5f,
            rotationDegrees = 30f,
            positionZ = 4,
        )

        assertEquals(0.6, transform.positionX, 1e-6)
        assertEquals(0.15, transform.positionY, 1e-6)
        assertEquals(1.5, transform.scale, 1e-6)
        assertEquals(30.0, transform.rotation, 1e-6)
        assertEquals(4, transform.positionZ)
    }
}
