package com.teamyg.parfait.core.designsystem.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * Compose 테스트 배선이 살아 있는지 확인하는 스모크 테스트.
 *
 * `createComposeRule()` 이 `androidx.activity.ComponentActivity` 를 띄우므로
 * `ui-test-manifest` 가 `debugImplementation` 에 제대로 걸려 있지 않으면 여기서
 * `ActivityNotFoundException` 이 난다.
 */
@MediumTest
class YGThemeSmokeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygCustomTheme_lightTheme_providesColorSchemeToContent() {
        // Given 컴포지션 결과를 담을 변수
        var capturedBackground: Any? = null

        // When 라이트 테마로 감싼 컨텐츠를 컴포지션
        composeTestRule.setContent {
            YGCustomTheme(darkTheme = false) {
                capturedBackground = YGTheme.colorScheme
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(SMOKE_TAG),
                )
            }
        }

        // Then CompositionLocal 이 채워지고 컨텐츠가 그려진다
        composeTestRule.onNodeWithTag(SMOKE_TAG).assertIsDisplayed()
        assertNotNull(capturedBackground)
    }

    @Test
    fun composeTestRule_clickOnTaggedNode_updatesState() {
        // Given 클릭할 때마다 증가하는 상태
        composeTestRule.setContent {
            var count by remember { mutableStateOf(0) }

            YGCustomTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("$COUNTER_TAG$count"),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(SMOKE_TAG)
                        .clickable { count += 1 },
                )
            }
        }

        // When 노드를 한 번 클릭
        composeTestRule.onNodeWithTag(SMOKE_TAG).performClick()

        // Then 재컴포지션이 일어나 태그가 바뀐다
        composeTestRule.onNodeWithTag("${COUNTER_TAG}1").assertIsDisplayed()
    }

    private companion object {
        const val SMOKE_TAG = "smoke"
        const val COUNTER_TAG = "counter-"
    }
}
