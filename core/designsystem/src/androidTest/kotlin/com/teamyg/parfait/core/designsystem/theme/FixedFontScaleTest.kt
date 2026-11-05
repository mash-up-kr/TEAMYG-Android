package com.teamyg.parfait.core.designsystem.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@MediumTest
class FixedFontScaleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygCustomTheme_enlargedFontScale_providesFontScaleOneWithSameDensity() {
        // Given 글자 크기를 2배로 키운 기기
        var captured: Density? = null

        // When 테마로 감싼 컨텐츠를 컴포지션
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 3f, fontScale = 2f)) {
                YGCustomTheme(darkTheme = false) {
                    captured = LocalDensity.current
                }
            }
        }

        // Then fontScale 만 1 이 되고 density 는 그대로다
        composeTestRule.waitForIdle()
        assertEquals(1f, captured?.fontScale)
        assertEquals(3f, captured?.density)
    }

    @Test
    fun dialog_insideTheme_revertsToSystemDensityUnlessProvidedAgain() {
        // Given 기기 값과 겹치지 않는 density
        val system = InstrumentationRegistry.getInstrumentation().targetContext.resources
        var dialogDensity: Density? = null
        var wrappedDensity: Density? = null

        // When 테마 안에서 Dialog 를 연다
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 7f, fontScale = 2f)) {
                YGCustomTheme(darkTheme = false) {
                    Dialog(onDismissRequest = {}) {
                        dialogDensity = LocalDensity.current
                        ProvideFixedFontScale {
                            wrappedDensity = LocalDensity.current
                        }
                    }
                }
            }
        }

        // Then Dialog 는 기기 값으로 돌아가고, 다시 감싸야 fontScale 이 1 이 된다
        composeTestRule.waitForIdle()
        assertEquals(system.displayMetrics.density, dialogDensity?.density)
        assertEquals(system.configuration.fontScale, dialogDensity?.fontScale)
        assertEquals(1f, wrappedDensity?.fontScale)
        assertEquals(system.displayMetrics.density, wrappedDensity?.density)
    }
}
