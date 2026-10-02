package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PANEL_TAG = "topping_border_panel"
private const val TOGGLE_TAG = "topping_border_panel_toggle"
private const val SLIDER_TAG = "topping_border_slider"
private const val CHIP_TAG_PREFIX = "topping_border_chip_"
private const val CHIP_NONE_TAG = "topping_border_chip_none"

/** 칩 아홉 개가 스크롤 없이 한 번에 놓이는 폭 */
private val PANEL_WIDTH = 480.dp

@MediumTest
@RunWith(AndroidJUnit4::class)
class ToppingBorderPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var toggleCount = 0
    private val selected = mutableListOf<Int?>()
    private var lowerLayerDownCount = 0

    @Test
    fun collapsed_showsLabelAndTogglesOnBarTap() {
        setPanel(isOpen = false)

        composeTestRule.onNodeWithText("테두리 설정").assertIsDisplayed()
        composeTestRule.onNodeWithTag(SLIDER_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TOGGLE_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, toggleCount) }
    }

    @Test
    fun collapsed_disabled_doesNotToggle() {
        setPanel(isOpen = false, isEnabled = false)

        composeTestRule.onNodeWithTag(TOGGLE_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(0, toggleCount) }
    }

    @Test
    fun open_showsSliderAndNineChips_andTogglesOnArrow() {
        setPanel(isOpen = true)

        composeTestRule.onNodeWithText("테두리 굵기").assertIsDisplayed()
        composeTestRule.onNodeWithTag(SLIDER_TAG).assertIsDisplayed()
        composeTestRule.onAllNodes(hasTestTagPrefix(CHIP_TAG_PREFIX)).assertCountEquals(9)
        composeTestRule.onNodeWithContentDescription("테두리 설정 닫기").performClick()

        composeTestRule.runOnIdle { assertEquals(1, toggleCount) }
    }

    @Test
    fun open_selectingNoneChip_emitsNull() {
        setPanel(isOpen = true, selectedColorArgb = 0xFF0E0E0E.toInt())

        composeTestRule.onNodeWithTag(CHIP_NONE_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf<Int?>(null), selected) }
    }

    @Test
    fun open_tapOnBlankArea_isConsumedByPanel() {
        setPanel(isOpen = true)

        composeTestRule.onNodeWithTag(PANEL_TAG).performTouchInput { click(topLeft + Offset(4f, 4f)) }

        composeTestRule.runOnIdle {
            assertEquals(0, lowerLayerDownCount)
            assertEquals(0, toggleCount)
        }
    }

    private fun setPanel(
        isOpen: Boolean,
        isEnabled: Boolean = true,
        selectedColorArgb: Int? = null,
    ) {
        composeTestRule.setContent {
            YGCustomTheme {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // 화면이 PANEL_WIDTH 보다 좁으면 넘친 쪽이 화면 밖이라 눌리지 않는다. 밀도를 낮춰 통째로 들인다
                    val base = LocalDensity.current
                    val fit = (maxWidth / PANEL_WIDTH).coerceAtMost(1f)
                    CompositionLocalProvider(
                        LocalDensity provides Density(base.density * fit, base.fontScale),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            lowerLayerDownCount++
                                        }
                                    },
                            )
                            ToppingBorderPanel(
                                isOpen = isOpen,
                                selectedColorArgb = selectedColorArgb,
                                widthDp = TOPPING_BORDER_WIDTH_RANGE_DP.start,
                                widthRange = TOPPING_BORDER_WIDTH_RANGE_DP,
                                onClickToggle = { toggleCount++ },
                                onSelectColor = { selected += it },
                                onChangeWidth = {},
                                isEnabled = isEnabled,
                                modifier = Modifier
                                    .width(PANEL_WIDTH)
                                    .align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("TestTag startsWith '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }
}
