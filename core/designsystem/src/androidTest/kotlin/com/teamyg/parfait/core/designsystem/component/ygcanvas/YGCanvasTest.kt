package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygskeleton.YG_SKELETON_TEST_TAG
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.utils.instantlySucceedingImageLoader
import com.teamyg.parfait.core.designsystem.utils.neverFinishingImageLoader
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val EMPTY_MESSAGE = "아직 캔버스가 비어 있어요"
private const val BACKGROUND_URL = "https://example.com/background.png"
private const val WAIT_TIMEOUT_MILLIS = 5_000L

@OptIn(DelicateCoilApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class YGCanvasTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
    }

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

    @Test
    fun ygCanvas_backgroundImageLoading_showsSkeleton() {
        // Given 끝나지 않는 로더라 배경은 로딩 상태에 머문다.
        // 스켈레톤이 무한 애니메이션이라 시계를 손으로 돌린다 — 자동이면 대기가 끝나지 않는다
        composeTestRule.mainClock.autoAdvance = false
        SingletonImageLoader.setUnsafe(neverFinishingImageLoader())

        // When
        composeTestRule.setContent {
            EmptyYGCanvas(background = YGCanvasBackground.Image(BACKGROUND_URL))
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Then 투명한 자리 대신 스켈레톤이 깔린다
        composeTestRule.onNodeWithTag(YG_SKELETON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun ygCanvas_backgroundImageLoaded_hidesSkeleton() {
        // Given 곧바로 성공하는 로더
        composeTestRule.mainClock.autoAdvance = false
        SingletonImageLoader.setUnsafe(instantlySucceedingImageLoader())

        // When
        composeTestRule.setContent {
            EmptyYGCanvas(background = YGCanvasBackground.Image(BACKGROUND_URL))
        }

        // Then 배경이 뜨고 나면 스켈레톤은 남지 않는다
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithTag(YG_SKELETON_TEST_TAG).fetchSemanticsNodes().isEmpty()
        }
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
