package com.teamyg.parfait.core.designsystem.component.ygtoast

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGToastHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygToastHost_showsTwiceWithSameTag_replacesPreviousToast() {
        // Given 토스트 정책을 테스트가 쥐고 호스트를 띄운다
        val toastPolicy = YGToastPolicy()
        setContentWith(toastPolicy)

        // When 같은 태그로 두 번 띄운다
        showAndAdvance(toastPolicy, FIRST_TEXT, SPOTLIGHT_TAG)
        showAndAdvance(toastPolicy, SECOND_TEXT, SPOTLIGHT_TAG)

        // Then 이전 것은 사라지고 나중 것만 남는다
        composeTestRule.onNodeWithText(FIRST_TEXT).assertDoesNotExist()
        composeTestRule.onNodeWithText(SECOND_TEXT).assertIsDisplayed()
    }

    @Test
    fun ygToastHost_showsTwiceWithoutTag_keepsBothToasts() {
        // Given 토스트 정책을 테스트가 쥐고 호스트를 띄운다
        val toastPolicy = YGToastPolicy()
        setContentWith(toastPolicy)

        // When 태그 없이 두 번 띄운다
        showAndAdvance(toastPolicy, FIRST_TEXT, tag = null)
        showAndAdvance(toastPolicy, SECOND_TEXT, tag = null)

        // Then 둘 다 남는다 — 태그 없는 토스트는 Toast 공통 정책대로 쌓인다
        composeTestRule.onNodeWithText(FIRST_TEXT).assertExists()
        composeTestRule.onNodeWithText(SECOND_TEXT).assertExists()
    }

    @Test
    fun ygToastHost_showsTwiceWithDifferentTags_keepsBothToasts() {
        // Given 토스트 정책을 테스트가 쥐고 호스트를 띄운다
        val toastPolicy = YGToastPolicy()
        setContentWith(toastPolicy)

        // When 서로 다른 태그로 두 번 띄운다
        showAndAdvance(toastPolicy, FIRST_TEXT, SPOTLIGHT_TAG)
        showAndAdvance(toastPolicy, SECOND_TEXT, OTHER_TAG)

        // Then 교체는 같은 태그끼리만 일어나므로 둘 다 남는다
        composeTestRule.onNodeWithText(FIRST_TEXT).assertExists()
        composeTestRule.onNodeWithText(SECOND_TEXT).assertExists()
    }

    /** 자동 소멸(2000ms)이 단언보다 먼저 오지 않도록 가상 시간을 손으로 굴린다 */
    private fun setContentWith(toastPolicy: YGToastPolicy) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            YGCustomTheme {
                YGToastHost(
                    policy = toastPolicy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    private fun showAndAdvance(
        toastPolicy: YGToastPolicy,
        text: String,
        tag: String?,
    ) {
        composeTestRule.runOnUiThread {
            toastPolicy.show(YGToastType.InviteCode(text), replaceTag = tag)
        }
        composeTestRule.mainClock.advanceTimeBy(TOAST_ENTER_ANIMATION_MILLIS)
    }

    private companion object {
        const val FIRST_TEXT = "첫 번째 토스트"
        const val SECOND_TEXT = "두 번째 토스트"
        const val SPOTLIGHT_TAG = "spotlight"
        const val OTHER_TAG = "other"

        /** 진입 애니메이션(300ms)보다 크고 자동 소멸(2000ms)보다 작아야 한다 */
        const val TOAST_ENTER_ANIMATION_MILLIS = 500L
    }
}
