package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.teamyg.parfait.core.designsystem.component.ygskeleton.YG_SKELETON_TEST_TAG
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.utils.instantlySucceedingImageLoader
import com.teamyg.parfait.core.designsystem.utils.neverFinishingImageLoader
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val IMAGE_URL = "https://example.com/topping.png"
private const val WAIT_TIMEOUT_MILLIS = 5_000L

@OptIn(DelicateCoilApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class YGToppingGroupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
    }

    @Test
    fun ygToppingGroup_remoteImageLoading_showsSkeleton() {
        // Given 끝나지 않는 로더라 화면은 로딩 상태에 머문다.
        // 스켈레톤이 무한 애니메이션이라 시계를 손으로 돌린다 — 자동이면 대기가 끝나지 않는다
        composeTestRule.mainClock.autoAdvance = false
        SingletonImageLoader.setUnsafe(neverFinishingImageLoader())

        // When
        composeTestRule.setContent { RemoteYGToppingGroup() }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Then 빈 자리 대신 스켈레톤이 깔린다
        composeTestRule.onNodeWithTag(YG_SKELETON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun ygToppingGroup_remoteImageLoaded_hidesSkeleton() {
        // Given 곧바로 성공하는 로더
        SingletonImageLoader.setUnsafe(instantlySucceedingImageLoader())

        // When
        composeTestRule.setContent { RemoteYGToppingGroup() }

        // Then 이미지가 뜨고 나면 스켈레톤은 남지 않는다
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithTag(YG_SKELETON_TEST_TAG).fetchSemanticsNodes().isEmpty()
        }
    }
}

@Composable
private fun RemoteYGToppingGroup() = YGCustomTheme {
    YGToppingGroup(
        image = YGToppingImage.Remote(IMAGE_URL),
        name = "잠탈감금",
        timestamp = "3분전",
        chipType = YGGrouptagChipType.TYPE_1_2,
        type = YGToppingGroupType.TYPE_1_LEFT,
    )
}
