package com.teamyg.parfait.core.ui.reveal

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val FAR_INDEX = 99

@MediumTest
@RunWith(AndroidJUnit4::class)
class RememberBatchRevealStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rememberBatchRevealState_everyItemSettled_isShown() {
        // Given · When 성공이든 실패든 전부 결말난 묶음
        lateinit var state: BatchRevealState

        composeTestRule.setContent {
            state = rememberBatchRevealState(settled = listOf(true, true))
        }

        composeTestRule.runOnIdle {
            assertTrue(state.shown)
            assertTrue(state.isRevealed(FAR_INDEX))
        }
    }

    @Test
    fun rememberBatchRevealState_oneStillLoading_isHidden() {
        // Given · When 아직 하나가 안 온 묶음
        lateinit var state: BatchRevealState

        composeTestRule.setContent {
            state = rememberBatchRevealState(settled = listOf(true, false))
        }

        composeTestRule.runOnIdle {
            assertFalse(state.shown)
            assertFalse(state.isRevealed(0))
        }
    }

    @Test
    fun rememberBatchRevealState_nothingToWaitFor_isShown() {
        // Given · When 기다릴 대상이 없는 빈 묶음
        lateinit var state: BatchRevealState

        composeTestRule.setContent {
            state = rememberBatchRevealState(settled = emptyList())
        }

        // Then 빈 화면 위에서 로딩만 돌면 안 된다
        composeTestRule.runOnIdle {
            assertTrue(state.shown)
        }
    }
}
