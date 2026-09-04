package com.teamyg.parfait.core.ui.reveal

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val STEP_MILLIS = 400L
private const val TOTAL = 3

@MediumTest
@RunWith(AndroidJUnit4::class)
class RememberStaggeredRevealStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rememberStaggeredRevealState_staggerOff_showsEveryItemAtOnce() {
        // Given · When 재진입처럼 순차를 끈 자리
        lateinit var state: RevealState

        composeTestRule.setContent {
            state = rememberStaggeredRevealState(
                total = TOTAL,
                stepMillis = STEP_MILLIS,
                staggered = false,
            )
        }

        // Then 기다리지 않고 마지막 자리까지 나온다
        composeTestRule.runOnIdle {
            assertTrue(state.isRevealed(TOTAL - 1))
        }
    }
}
