package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val EMPTY_MESSAGE = "아직 캔버스가 비어 있어요"

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGCanvasTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygCanvas_emptyWithoutBackground_showsEmptyMessage() {
        // Given · When 배경도 토핑도 없는 캔버스
        composeTestRule.setContent {
            EmptyYGCanvas(background = null)
        }

        // Then 회색 안내판의 문구가 보인다
        composeTestRule.onNodeWithText(EMPTY_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun ygCanvas_emptyWithBackground_hidesEmptyMessage() {
        // Given · When 토핑은 없지만 배경을 고른 캔버스
        composeTestRule.setContent {
            EmptyYGCanvas(background = YGCanvasBackground.Solid(Color(0xFFC2E4FC)))
        }

        // Then 고른 배경이 그대로 보여야 하므로 안내 문구는 사라진다
        composeTestRule.onNodeWithText(EMPTY_MESSAGE).assertDoesNotExist()
    }
}

@Composable
private fun EmptyYGCanvas(background: YGCanvasBackground?) = YGCustomTheme {
    YGCanvas(
        date = "May 20",
        day = "(Wed)",
        onDateSelectClick = {},
        addAction = YGCanvasMenuAction(
            text = "토핑 추가",
            iconResource = R.drawable.ic_plus,
            onClick = {},
        ),
        editAction = YGCanvasMenuAction(
            text = "캔버스 편집",
            iconResource = R.drawable.ic_caret_right,
            onClick = {},
        ),
        background = background,
        isEmpty = true,
        emptyMessage = EMPTY_MESSAGE,
    )
}
