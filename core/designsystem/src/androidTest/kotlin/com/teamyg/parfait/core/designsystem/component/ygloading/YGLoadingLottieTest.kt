package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGLoadingLottieTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygLoadingLottie_noModifier_drawsAtAssetSize() {
        // Given 로띠가 무한 반복이라 시계를 손으로 돌린다
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            YGLoadingLottie()
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Then 쓰는 쪽이 크기를 몰라도 애셋 원본대로 그린다
        composeTestRule
            .onNodeWithTag(YG_LOADING_LOTTIE_TEST_TAG)
            .assertWidthIsEqualTo(44.dp)
            .assertHeightIsEqualTo(44.dp)
    }

    @Test
    fun ygLoadingLottie_toppingArt_drawsAtItsOwnSize() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            YGLoadingLottie(art = YGLoadingArt.Topping)
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Then 애셋마다 원본 크기가 다르다
        composeTestRule
            .onNodeWithTag(YG_LOADING_LOTTIE_TEST_TAG)
            .assertWidthIsEqualTo(90.dp)
            .assertHeightIsEqualTo(106.dp)
    }
}
