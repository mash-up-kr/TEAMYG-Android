package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedFontScaleTest {
    private fun density(fontScale: Float) = Density(density = 2.75f, fontScale = fontScale)

    private fun assertRendersDesignDp(
        d: Density,
        converted: TextUnit,
        designValue: Float,
    ) {
        val expected = with(d) { designValue.dp.toPx() }
        val actual = with(d) { converted.toPx() }
        assertEquals(expected, actual, absoluteTolerance = 0.01f)
    }

    @Test
    fun toFixedFontScale_fontScaleOne_isIdentity() {
        assertEquals(14.sp, 14.sp.toFixedFontScale(density(1.0f)))
    }

    @Test
    fun toFixedFontScale_fontScale115_rendersDesignDp() {
        val d = density(1.15f)
        assertRendersDesignDp(d, 14.sp.toFixedFontScale(d), 14f)
    }

    @Test
    fun toFixedFontScale_nonLinearScale_rendersDesignDp() {
        // Given 26dp는 2.0 변환표 안의 점이다 (14sp = 26dp)
        val d = density(2.0f)
        val converted = 26.sp.toFixedFontScale(d)
        // Then 선형 변환(26 / 2.0 = 13)이었다면 여기서 어긋난다
        assertEquals(14f, converted.value, absoluteTolerance = 0.01f)
        assertRendersDesignDp(d, converted, 26f)
    }

    @Test
    fun toFixedFontScale_nonLinearScale115_rendersDesignDp() {
        // Given 26dp는 1.15 변환표 안의 점이다 (25sp = 26dp)
        val d = density(1.15f)
        val converted = 26.sp.toFixedFontScale(d)
        // Then 선형 변환(26 / 1.15 = 22.6)이었다면 여기서 어긋난다
        assertEquals(25f, converted.value, absoluteTolerance = 0.01f)
        assertRendersDesignDp(d, converted, 26f)
    }

    @Test
    fun toFixedFontScale_emAndUnspecified_unchanged() {
        val d = density(1.15f)
        assertEquals(1.4.em, 1.4.em.toFixedFontScale(d))
        assertEquals(TextUnit.Unspecified, TextUnit.Unspecified.toFixedFontScale(d))
    }

    @Test
    fun textStyle_toFixedFontScale_convertsSpFields() {
        val d = density(1.15f)
        val style =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            ).toFixedFontScale(d)

        assertRendersDesignDp(d, style.fontSize, 14f)
        // 렌더러는 sp lineHeight를 fontSize 대비 비율로 그리므로 절대값이 아니라 비율을 본다
        assertEquals(20f / 14f, style.lineHeight.value / style.fontSize.value, absoluteTolerance = 0.0001f)
        assertRendersDesignDp(d, style.letterSpacing, 1f)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun textStyle_toFixedFontScale_lineHeightWithEmFontSize_fallsBackToAbsolute() {
        val d = density(1.15f)
        val style = TextStyle(fontSize = 1.2.em, lineHeight = 20.sp).toFixedFontScale(d)

        assertEquals(with(d) { 20.dp.toSp() }, style.lineHeight)
    }

    @Test
    fun annotatedString_toFixedFontScale_convertsSpans() {
        val d = density(1.15f)
        val source =
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 12.sp)) { append("a") }
                withStyle(ParagraphStyle(lineHeight = 18.sp)) { append("b") }
            }

        val result = source.toFixedFontScale(d)

        assertEquals("ab", result.text)
        val span = result.spanStyles.single()
        assertRendersDesignDp(d, span.item.fontSize, 12f)
        assertEquals(0, span.start)
        assertEquals(1, span.end)
        val paragraph = result.paragraphStyles.single()
        assertRendersDesignDp(d, paragraph.item.lineHeight, 18f)
        assertEquals(1, paragraph.start)
        assertEquals(2, paragraph.end)
    }

    @Test
    fun annotatedString_toFixedFontScale_keepsStringAnnotations() {
        val source =
            buildAnnotatedString {
                append("abc")
                addStringAnnotation("tag", "value", 1, 3)
            }

        val result = source.toFixedFontScale(density(1.15f))

        val annotation = result.getStringAnnotations("tag", 0, 3).single()
        assertEquals("value", annotation.item)
        assertEquals(1, annotation.start)
        assertEquals(3, annotation.end)
    }

    @Test
    fun annotatedString_toFixedFontScale_convertsLinkStyles() {
        val d = density(1.15f)
        val source =
            buildAnnotatedString {
                append("abc")
                addLink(
                    LinkAnnotation.Url("https://example.com", TextLinkStyles(style = SpanStyle(fontSize = 14.sp))),
                    0,
                    3,
                )
            }

        val link = source
            .toFixedFontScale(d)
            .getLinkAnnotations(0, 3)
            .single()
            .item

        val url = link as LinkAnnotation.Url
        assertEquals("https://example.com", url.url)
        assertRendersDesignDp(d, url.styles!!.style!!.fontSize, 14f)
    }

    @Test
    fun textStyle_toFixedFontScale_unspecifiedFontSize_fallsBackToRendererDefault() {
        val d = density(1.15f)
        val style = TextStyle().toFixedFontScale(d)
        assertRendersDesignDp(d, style.fontSize, 14f)
    }

    @Test
    fun textStyle_toFixedFontScale_unspecifiedFontSizeAtFontScaleOne_is14sp() {
        assertEquals(14.sp, TextStyle().toFixedFontScale(density(1.0f)).fontSize)
    }
}
