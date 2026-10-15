package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import com.teamyg.parfait.feature.groups.canvas.impl.writeTestToppingPng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val INPUT_TAG = "topping_place_input"
private const val PANEL_TAG = "topping_border_panel"

/** 좌우 여백 20dp 씩을 빼면 캔버스 폭이 300dp 다 */
private val SCREEN_WIDTH = 340.dp

private const val IMAGE_READY_TIMEOUT_MILLIS = 5_000L

@MediumTest
@RunWith(AndroidJUnit4::class)
class CanvasToppingPlaceScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var toppingImagePath: String

    private var isPanelOpen by mutableStateOf(false)
    private var imageReady = false
    private var confirmCount = 0
    private var clickToppingCount = 0
    private var toggleCount = 0
    private var dismissCount = 0
    private var transformCount = 0

    @Before
    fun writeToppingImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        toppingImagePath = writeTestToppingPng(context, "place.png")
    }

    @Test
    fun header_showsBackTitleAndClose_andBottomButton() {
        setScreen()

        composeTestRule.onNodeWithText("배치").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("닫기").assertIsDisplayed()
        composeTestRule.onNodeWithText("캔버스에 쌓기").performClick()

        composeTestRule.runOnIdle { assertEquals(1, confirmCount) }
    }

    @Test
    fun panelClosed_tapOnTopping_requestsOpen() {
        setScreen()
        awaitImageReady()

        composeTestRule.onNodeWithTag(INPUT_TAG).performTouchInput { click(center) }

        composeTestRule.runOnIdle { assertEquals(1, clickToppingCount) }
    }

    @Test
    fun panelClosed_tapOnEmptyCanvas_doesNothing() {
        setScreen()
        awaitImageReady()

        composeTestRule.onNodeWithTag(INPUT_TAG).performTouchInput {
            click(Offset(10.dp.toPx(), 10.dp.toPx()))
        }

        composeTestRule.runOnIdle {
            assertEquals(0, clickToppingCount)
            assertEquals(0, transformCount)
            assertEquals(0, dismissCount)
            assertEquals(0, toggleCount)
        }
    }

    /** [panelOpen_dragOnCanvas_onlyDismisses] 의 대조군. 같은 드래그가 패널이 닫혀 있으면 토핑을 옮긴다 */
    @Test
    fun panelClosed_dragOnTopping_transforms() {
        setScreen()
        awaitImageReady()

        composeTestRule.onNodeWithTag(INPUT_TAG).performTouchInput {
            down(center)
            moveBy(Offset(40.dp.toPx(), 0f))
            moveBy(Offset(40.dp.toPx(), 0f))
            up()
        }

        composeTestRule.runOnIdle {
            assertTrue(transformCount > 0)
            assertEquals(0, dismissCount)
            assertEquals(0, clickToppingCount)
        }
    }

    @Test
    fun panelOpen_dragOnCanvas_onlyDismisses() {
        isPanelOpen = true
        setScreen()
        awaitImageReady()

        composeTestRule.onNodeWithTag(INPUT_TAG).performTouchInput { down(center) }
        // 패널이 닫힌 상태로 리컴포지션이 끝난 뒤에 드래그가 이어진다
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(INPUT_TAG).performTouchInput {
            moveBy(Offset(40.dp.toPx(), 0f))
            moveBy(Offset(40.dp.toPx(), 0f))
            up()
        }

        composeTestRule.runOnIdle {
            assertEquals(false, isPanelOpen)
            assertEquals(1, dismissCount)
            assertEquals(0, transformCount)
            assertEquals(0, clickToppingCount)
        }
    }

    @Test
    fun panelOpen_tapOnPanelBlankArea_keepsPanelOpen() {
        isPanelOpen = true
        setScreen()
        awaitImageReady()

        composeTestRule.onNodeWithTag(PANEL_TAG).performTouchInput { click(topLeft + Offset(4f, 4f)) }

        composeTestRule.runOnIdle {
            assertEquals(true, isPanelOpen)
            assertEquals(0, dismissCount)
            assertEquals(0, toggleCount)
            assertEquals(0, clickToppingCount)
            assertEquals(0, transformCount)
        }
    }

    /** 그림이 뜨기 전에는 판정 대상이 없다 */
    private fun awaitImageReady() {
        composeTestRule.waitUntil(IMAGE_READY_TIMEOUT_MILLIS) { imageReady }
    }

    private fun setScreen() {
        composeTestRule.setContent {
            YGCustomTheme {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CanvasToppingPlaceScreen(
                        // 토핑이 캔버스 한가운데 놓인다
                        uiState = CanvasToppingPlaceUiState(
                            toppingImagePath = toppingImagePath,
                            isDraftLoaded = true,
                            isToppingImageReady = true,
                            canvasSize = DpSize(300.dp, 534.dp),
                            toppingBaseSize = DpSize(100.dp, 100.dp),
                            offsetX = 100.dp,
                            offsetY = 217.dp,
                            scale = 1f,
                            hasUserAdjustedPlacement = true,
                            isBorderPanelOpen = isPanelOpen,
                        ),
                        onClickBack = {},
                        onClickClose = {},
                        onClickConfirm = { confirmCount++ },
                        onClickTopping = { clickToppingCount++ },
                        onToggleBorderPanel = { toggleCount++ },
                        onDismissBorderPanel = {
                            dismissCount++
                            isPanelOpen = false
                        },
                        onSelectBorderColor = {},
                        onChangeBorderWidth = {},
                        onToppingTransform = { _, _, _ -> transformCount++ },
                        onCanvasMeasured = {},
                        onToppingBaseSizeMeasured = {},
                        onToppingImageReadyChanged = { isReady -> imageReady = isReady },
                        modifier = Modifier
                            .requiredWidth(SCREEN_WIDTH)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}
