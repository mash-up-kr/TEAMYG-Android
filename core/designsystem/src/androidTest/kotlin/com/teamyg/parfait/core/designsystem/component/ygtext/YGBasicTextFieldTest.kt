package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 시스템 글자 크기(`fontScale`)가 1.15여도 입력창 레이아웃의 글자 크기가 디자인 dp 값인지 확인한다.
 */
@MediumTest
class YGBasicTextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygBasicTextField_state_textStyleIsFixed() {
        val (layout, density) = layoutOf { onLayout ->
            YGBasicTextField(
                state = rememberTextFieldState("a"),
                textStyle = TextStyle(fontSize = 14.sp),
                onTextLayout = { getResult -> getResult()?.let(onLayout) },
            )
        }

        assertFixed(layout.layoutInput.style.fontSize, density)
    }

    @Test
    fun ygBasicTextField_string_textStyleIsFixed() {
        val (layout, density) = layoutOf { onLayout ->
            var value by remember { mutableStateOf("a") }
            YGBasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = TextStyle(fontSize = 14.sp),
                onTextLayout = onLayout,
            )
        }

        assertFixed(layout.layoutInput.style.fontSize, density)
    }

    @Test
    fun ygBasicTextField_textFieldValue_textStyleIsFixed() {
        val (layout, density) = layoutOf { onLayout ->
            var value by remember { mutableStateOf(TextFieldValue("a")) }
            YGBasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = TextStyle(fontSize = 14.sp),
                onTextLayout = onLayout,
            )
        }

        assertFixed(layout.layoutInput.style.fontSize, density)
    }

    private fun layoutOf(content: @Composable ((TextLayoutResult) -> Unit) -> Unit): Pair<TextLayoutResult, Density> {
        var result: TextLayoutResult? = null
        var scaled: Density? = null
        composeTestRule.setContent {
            val base = LocalDensity.current
            val density = remember(base) { Density(density = base.density, fontScale = FONT_SCALE) }
            scaled = density
            CompositionLocalProvider(LocalDensity provides density) {
                content { result = it }
            }
        }
        composeTestRule.waitForIdle()
        return checkNotNull(result) to checkNotNull(scaled)
    }

    private fun assertFixed(
        fontSize: TextUnit,
        density: Density,
    ) {
        val expectedPx = with(density) { 14.dp.toPx() }
        val actualPx = with(density) { fontSize.toPx() }
        assertEquals(expectedPx, actualPx, 0.01f)
    }

    private companion object {
        const val FONT_SCALE = 1.15f
    }
}
