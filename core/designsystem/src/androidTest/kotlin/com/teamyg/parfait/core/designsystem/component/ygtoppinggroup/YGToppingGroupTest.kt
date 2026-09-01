package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.utils.instantlyFailingImageLoader
import com.teamyg.parfait.core.designsystem.utils.instantlySucceedingImageLoader
import com.teamyg.parfait.core.designsystem.utils.neverFinishingImageLoader
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val IMAGE_URL = "https://example.com/topping.png"
private const val WAIT_TIMEOUT_MILLIS = 5_000L
private const val SETTLE_GRACE_FRAMES = 10

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
    fun ygToppingGroup_remoteImageLoaded_reportsSettled() {
        // Given 곧바로 성공하는 로더
        SingletonImageLoader.setUnsafe(instantlySucceedingImageLoader())
        var settled = false

        // When
        composeTestRule.setContent {
            RemoteYGToppingGroup(onImageSettled = { settled = true })
        }

        // Then 목록 전체를 언제 내보낼지 세려면 이 신호가 와야 한다
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) { settled }
    }

    @Test
    fun ygToppingGroup_remoteImageFailed_reportsSettled() {
        // Given 곧바로 실패하는 로더
        SingletonImageLoader.setUnsafe(instantlyFailingImageLoader())
        var settled = false

        // When
        composeTestRule.setContent {
            RemoteYGToppingGroup(onImageSettled = { settled = true })
        }

        // Then 깨진 이미지 한 장이 목록 전체를 붙잡으면 안 되므로 실패도 결말이다
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) { settled }
    }

    @Test
    fun ygToppingGroup_remoteImageLoading_doesNotReportSettled() {
        // Given 끝나지 않는 로더
        SingletonImageLoader.setUnsafe(neverFinishingImageLoader())
        var settled = false

        // When
        composeTestRule.setContent {
            RemoteYGToppingGroup(onImageSettled = { settled = true })
        }
        repeat(SETTLE_GRACE_FRAMES) { composeTestRule.mainClock.advanceTimeByFrame() }

        // Then 아직 오는 중인 이미지를 결말로 세면 목록이 반쯤 빈 채로 드러난다
        assertFalse(settled)
    }

    @Test
    fun ygToppingGroup_templateImage_reportsSettled() {
        // Given 원격 이미지가 없는 템플릿 토핑
        var settled = false

        // When
        composeTestRule.setContent {
            YGCustomTheme {
                YGToppingGroup(
                    image = YGToppingImage.Template(YGToppingTemplate.TEMPLATE_01),
                    name = "잠탈감금",
                    timestamp = "3분전",
                    chipType = YGGrouptagChipType.TYPE_1_2,
                    type = YGToppingGroupType.TYPE_1_LEFT,
                    onImageSettled = { settled = true },
                )
            }
        }

        // Then 기다릴 것이 없으므로 곧바로 결말이다 — 아니면 목록이 영원히 안 뜬다
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) { settled }
    }
}

@Composable
private fun RemoteYGToppingGroup(onImageSettled: () -> Unit) = YGCustomTheme {
    YGToppingGroup(
        image = YGToppingImage.Remote(IMAGE_URL),
        name = "잠탈감금",
        timestamp = "3분전",
        chipType = YGGrouptagChipType.TYPE_1_2,
        type = YGToppingGroupType.TYPE_1_LEFT,
        onImageSettled = onImageSettled,
    )
}
