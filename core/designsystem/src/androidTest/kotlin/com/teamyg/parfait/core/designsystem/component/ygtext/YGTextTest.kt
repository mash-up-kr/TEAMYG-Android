package com.teamyg.parfait.core.designsystem.component.ygtext

import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * 시스템 글자 크기(`fontScale`)가 1.15여도 레이아웃에 들어간 글자 크기가 디자인 dp 값인지 확인한다.
 */
@MediumTest
class YGTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygText_styleFontSize_isFixed() {
        val layout = layoutOf { YGText("a", style = TextStyle(fontSize = 14.sp), onTextLayout = it) }

        assertFixed(layout.layoutInput.style.fontSize)
    }

    @Test
    fun ygText_fontSizeParam_isFixed() {
        val layout = layoutOf { YGText("a", fontSize = 14.sp, onTextLayout = it) }

        assertFixed(layout.layoutInput.style.fontSize)
    }

    @Test
    fun ygTextAnnotated_spanFontSize_isFixed() {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 14.sp)) { append("a") }
        }
        val layout = layoutOf { YGText(text, onTextLayout = it) }

        assertFixed(
            layout.layoutInput.text.spanStyles
                .first()
                .item.fontSize,
        )
    }

    @Test
    fun ygBasicText_styleFontSize_isFixed() {
        val layout = layoutOf {
            YGBasicText(AnnotatedString("a"), style = TextStyle(fontSize = 14.sp), onTextLayout = it)
        }

        assertFixed(layout.layoutInput.style.fontSize)
    }

    @Test
    fun ygText_styleLineHeight_rendersDesignDpPitch() {
        val layout = nonLinearLayoutOf {
            YGText("a\nb\nc", style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp), onTextLayout = it)
        }

        assertLinePitch(layout)
    }

    @Test
    fun ygText_lineHeightParam_rendersDesignDpPitch() {
        val layout = nonLinearLayoutOf {
            YGText("a\nb\nc", lineHeight = 20.sp, style = TextStyle(fontSize = 14.sp), onTextLayout = it)
        }

        assertLinePitch(layout)
    }

    // 렌더러는 fontScale > 1.05 에서 sp lineHeight를 글자 크기 비율로 그린다.
    // 비선형 변환은 API 34부터라 그 아래에선 절대값 변환으로 되돌려도 통과하므로 건너뛴다
    private fun nonLinearLayoutOf(
        content: @androidx.compose.runtime.Composable ((TextLayoutResult) -> Unit) -> Unit,
    ): TextLayoutResult {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { fontScale = FONT_SCALE }
        density = Density(context.createConfigurationContext(configuration))
        var result: TextLayoutResult? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                content { result = it }
            }
        }
        composeTestRule.waitForIdle()
        return checkNotNull(result)
    }

    // 첫·끝 줄은 LineHeightStyle trim 영향을 받을 수 있어 가운데 줄 높이를 본다
    private fun assertLinePitch(layout: TextLayoutResult) {
        val expectedPx = with(density) { 20.dp.toPx() }
        assertEquals(expectedPx, layout.getLineTop(2) - layout.getLineTop(1), 1f)
    }

    private fun layoutOf(
        content: @androidx.compose.runtime.Composable ((TextLayoutResult) -> Unit) -> Unit,
    ): TextLayoutResult {
        var result: TextLayoutResult? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides scaledDensity(LocalDensity.current)) {
                content { result = it }
            }
        }
        composeTestRule.waitForIdle()
        return checkNotNull(result)
    }

    private fun scaledDensity(base: Density): Density {
        density = Density(density = base.density, fontScale = FONT_SCALE)
        return density
    }

    private lateinit var density: Density

    private fun assertFixed(fontSize: TextUnit) {
        val expectedPx = with(density) { 14.dp.toPx() }
        val actualPx = with(density) { fontSize.toPx() }
        assertEquals(expectedPx, actualPx, 0.01f)
    }

    private companion object {
        const val FONT_SCALE = 1.15f
    }
}
