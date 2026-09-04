package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGLoadingOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygLoadingOverlay_composed_isDisplayed() {
        // Given · When 오버레이만 컴포지션
        composeTestRule.setContent {
            YGLoadingOverlay()
        }

        // Then 오버레이가 그려진다
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun ygLoadingOverlay_overContent_swallowsClick() {
        // Given 클릭 가능한 컨텐츠 위에 오버레이를 덮는다
        var clickCount = 0
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(CONTENT_TAG)
                        .clickable { clickCount += 1 },
                )
                YGLoadingOverlay()
            }
        }

        // When 가려진 컨텐츠를 클릭
        composeTestRule.onNodeWithTag(CONTENT_TAG).performClick()

        // Then 클릭이 오버레이에서 멎어 콜백이 불리지 않는다
        composeTestRule.runOnIdle { assertEquals(0, clickCount) }
    }

    @Test
    fun ygLoadingOverlay_defaultArt_drawsAtCommonLoadingSize() {
        // Given 로띠가 무한 반복이라 시계를 손으로 돌린다
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            YGLoadingOverlay()
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Then 공통 로딩은 44×44 그대로다
        composeTestRule
            .onNodeWithTag(YG_LOADING_LOTTIE_TEST_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(44.dp)
            .assertHeightIsEqualTo(44.dp)
    }

    @Test
    fun ygLoadingOverlay_toppingArt_drawsAtItsOwnSize() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            YGLoadingOverlay(art = YGLoadingArt.Topping)
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Then 애셋 원본 크기라 다시 그리는 일이 없다
        composeTestRule
            .onNodeWithTag(YG_LOADING_LOTTIE_TEST_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(90.dp)
            .assertHeightIsEqualTo(106.dp)
    }

    private companion object {
        const val CONTENT_TAG = "content"
    }
}
