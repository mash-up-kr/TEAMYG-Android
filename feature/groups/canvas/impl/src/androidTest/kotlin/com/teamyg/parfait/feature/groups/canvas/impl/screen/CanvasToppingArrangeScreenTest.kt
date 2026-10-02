package com.teamyg.parfait.feature.groups.canvas.impl.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.feature.groups.canvas.impl.hasTestTagPrefix
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeUiState
import com.teamyg.parfait.feature.groups.canvas.impl.writeTestToppingPng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val INPUT_TAG = "topping_arrange_input"
private const val TOGGLE_TAG = "topping_border_panel_toggle"
private const val TOPPING_TAG_PREFIX = "editable_topping_"

/** 좌우 여백 20dp 씩을 빼면 캔버스 폭이 300dp 다 */
private val SCREEN_WIDTH = 340.dp

private const val MY_FOCUSED_ID = 1L
private const val MY_OTHER_ID = 2L
private const val OTHERS_ID = 3L

private val MY_FOCUSED_AT = Offset(0.25f, 0.2f)
private val MY_OTHER_AT = Offset(0.75f, 0.2f)
private val OTHERS_AT = Offset(0.5f, 0.6f)

/** 닫힌 패널 바 바로 위. 더 내려가면 바가 터치를 가져간다 */
private val EMPTY_AT = Offset(0.5f, 0.85f)

@MediumTest
@RunWith(AndroidJUnit4::class)
class CanvasToppingArrangeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var toppingImageUrl: String

    private val clicked = mutableListOf<EditableTopping>()
    private var emptyCount = 0
    private var confirmCount = 0
    private var toggleCount = 0
    private var dismissCount = 0

    @Before
    fun writeToppingImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        toppingImageUrl = File(writeTestToppingPng(context, "arrange.png")).toUri().toString()
    }

    @Test
    fun header_showsTitleAndCloseOnly() {
        setScreen()

        composeTestRule.onNodeWithText("배치").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("닫기").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertDoesNotExist()
    }

    @Test
    fun focusedTopping_showsDeleteButton_andNoEditButton() {
        setScreen()

        composeTestRule.onNodeWithContentDescription("삭제").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("편집").assertDoesNotExist()
    }

    @Test
    fun tapOnFocusedTopping_reportsThatTopping() {
        setScreen()

        tapCanvas(MY_FOCUSED_AT)

        composeTestRule.runOnIdle {
            assertEquals(listOf(MY_FOCUSED_ID), clicked.map { it.parfaitImageId })
            assertEquals(0, emptyCount)
        }
    }

    @Test
    fun tapOnOthersTopping_reportsThatTopping() {
        setScreen()

        tapCanvas(OTHERS_AT)

        composeTestRule.runOnIdle {
            assertEquals(listOf(OTHERS_ID), clicked.map { it.parfaitImageId })
            assertEquals(0, emptyCount)
        }
    }

    @Test
    fun tapOnEmptyCanvas_reportsEmpty() {
        setScreen()

        tapCanvas(EMPTY_AT)

        composeTestRule.runOnIdle {
            assertEquals(1, emptyCount)
            assertTrue(clicked.isEmpty())
            assertEquals(0, toggleCount)
        }
    }

    @Test
    fun bottomButton_reportsConfirm() {
        setScreen()

        composeTestRule.onNodeWithText("캔버스에 쌓기").performClick()

        composeTestRule.runOnIdle { assertEquals(1, confirmCount) }
    }

    @Test
    fun ownToppingOverOthers_winsTheHit() {
        setScreen(uiState = uiState(myOtherAt = OTHERS_AT))

        tapCanvas(OTHERS_AT)

        composeTestRule.runOnIdle { assertEquals(listOf(MY_OTHER_ID), clicked.map { it.parfaitImageId }) }
    }

    @Test
    fun withoutFocus_panelBarDoesNotToggle() {
        setScreen(uiState = uiState(focusedToppingId = null))

        composeTestRule.onNodeWithTag(TOGGLE_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(0, toggleCount) }
    }

    /** 패널을 열어 둔 채로 두어, 세 터치가 저마다 닫기만 부르는지 본다 */
    @Test
    fun panelOpen_touchOnCanvas_onlyDismisses() {
        setScreen(uiState = uiState(isBorderPanelOpen = true))

        tapCanvas(MY_OTHER_AT)
        tapCanvas(OTHERS_AT)
        tapCanvas(Offset(0.5f, 0.05f))

        composeTestRule.runOnIdle {
            assertEquals(3, dismissCount)
            assertTrue(clicked.isEmpty())
            assertEquals(0, emptyCount)
        }
    }

    @Test
    fun panelOpen_focusedToppingIsDrawnLast() {
        setScreen(uiState = uiState(isBorderPanelOpen = true))

        assertEquals(
            listOf("editable_topping_3", "editable_topping_2", "editable_topping_1"),
            toppingTagsInNodeOrder(),
        )
    }

    /** [panelOpen_focusedToppingIsDrawnLast] 의 대조군 */
    @Test
    fun panelClosed_toppingsKeepListOrder() {
        setScreen(uiState = uiState(isBorderPanelOpen = false))

        assertEquals(
            listOf("editable_topping_3", "editable_topping_1", "editable_topping_2"),
            toppingTagsInNodeOrder(),
        )
    }

    private fun toppingTagsInNodeOrder(): List<String> = composeTestRule
        .onAllNodes(hasTestTagPrefix(TOPPING_TAG_PREFIX))
        .fetchSemanticsNodes()
        .map { it.config[SemanticsProperties.TestTag] }

    /** @param fraction 캔버스 대비 비율 */
    private fun tapCanvas(fraction: Offset) {
        composeTestRule.onNodeWithTag(INPUT_TAG).performTouchInput {
            click(Offset(width * fraction.x, height * fraction.y))
        }
    }

    /** 목록 순서는 `[남, 본인 포커스, 본인 다른 것]` 이다 — 포커스된 토핑이 다른 본인 토핑보다 아래다 */
    private fun uiState(
        focusedToppingId: Long? = MY_FOCUSED_ID,
        isBorderPanelOpen: Boolean = false,
        myOtherAt: Offset = MY_OTHER_AT,
    ) = CanvasToppingArrangeUiState(
        toppings = listOf(
            topping(id = OTHERS_ID, isMine = false, at = OTHERS_AT),
            topping(id = MY_FOCUSED_ID, isMine = true, at = MY_FOCUSED_AT),
            topping(id = MY_OTHER_ID, isMine = true, at = myOtherAt),
        ),
        focusedToppingId = focusedToppingId,
        isBorderPanelOpen = isBorderPanelOpen,
    )

    private fun topping(
        id: Long,
        isMine: Boolean,
        at: Offset,
    ) = EditableTopping(
        parfaitImageId = id,
        isMine = isMine,
        imageUrl = toppingImageUrl,
        positionX = at.x,
        positionY = at.y,
        scale = 0.5f,
    )

    private fun setScreen(uiState: CanvasToppingArrangeUiState = uiState()) {
        composeTestRule.setContent {
            YGCustomTheme {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CanvasToppingArrangeScreen(
                        uiState = uiState,
                        onClickClose = {},
                        onClickConfirm = { confirmCount++ },
                        onClickTopping = { topping -> clicked += topping },
                        onClickEmptyCanvas = { emptyCount++ },
                        onToggleBorderPanel = { toggleCount++ },
                        onDismissBorderPanel = { dismissCount++ },
                        onSelectBorderColor = {},
                        onChangeBorderWidth = {},
                        onToppingTransform = { _, _, _, _ -> },
                        onClickDeleteTopping = {},
                        onDeleteToppingDialogConfirm = {},
                        onDeleteToppingDialogCancel = {},
                        modifier = Modifier
                            .requiredWidth(SCREEN_WIDTH)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}
