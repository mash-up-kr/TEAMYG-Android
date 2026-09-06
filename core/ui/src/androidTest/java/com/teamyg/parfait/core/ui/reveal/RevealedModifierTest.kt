package com.teamyg.parfait.core.ui.reveal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LABEL = "그룹 토핑"

@MediumTest
@RunWith(AndroidJUnit4::class)
class RevealedModifierTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun revealed_notRevealedYet_hidesDescendantsFromAccessibility() {
        // Given · When 아직 차례가 오지 않은 자리
        composeTestRule.setContent {
            LabeledBox(revealed = false)
        }

        // Then 화면에 안 보이는 동안 스크린리더가 읽으면 안 된다
        composeTestRule.onNodeWithContentDescription(LABEL).assertDoesNotExist()
    }

    @Test
    fun revealed_revealed_keepsDescendantsReadable() {
        // Given · When 차례가 온 자리
        composeTestRule.setContent {
            LabeledBox(revealed = true)
        }

        // Then 감춤이 풀린다
        composeTestRule.onNodeWithContentDescription(LABEL).assertIsDisplayed()
    }
}

@Composable
private fun LabeledBox(revealed: Boolean) {
    Box(modifier = Modifier.revealed(revealed)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = LABEL },
        )
    }
}
