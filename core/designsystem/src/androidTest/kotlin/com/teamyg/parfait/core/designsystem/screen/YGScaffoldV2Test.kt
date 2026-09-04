package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.core.designsystem.component.ygloading.YG_LOADING_OVERLAY_TEST_TAG
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val COVERED_BUTTON_DESCRIPTION = "덮개 아래 버튼"
private const val CUSTOM_OVERLAY_TAG = "custom_overlay"

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGScaffoldV2Test {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygScaffoldV2_isLoadingTrue_showsOverlay() {
        // Given · When 로딩을 켠 채 컴포지션
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(isLoading = true) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }

        // Then 오버레이가 보인다
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun ygScaffoldV2_isLoadingTrue_hidesContentDescendantsFromAccessibility() {
        // Given · When 덮개 아래에 누를 수 있는 것이 있다
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(isLoading = true) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .semantics {
                                role = Role.Button
                                contentDescription = COVERED_BUTTON_DESCRIPTION
                                onClick { true }
                            },
                    )
                }
            }
        }

        // Then 덮개가 터치를 막는 동안 스크린리더만 아래에 닿으면 안 된다
        composeTestRule.onNodeWithContentDescription(COVERED_BUTTON_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun ygScaffoldV2_customLoadingOverlay_isUsedInsteadOfDefault() {
        // Given · When 화면이 자기 덮개를 넘긴다
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(
                    isLoading = true,
                    loadingOverlay = {
                        Box(modifier = Modifier.fillMaxSize().testTag(CUSTOM_OVERLAY_TAG))
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }

        // Then 넘긴 것이 그려지고 기본 덮개는 그려지지 않는다
        composeTestRule.onNodeWithTag(CUSTOM_OVERLAY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun ygScaffoldV2_isLoadingFalse_hidesOverlay() {
        // Given · When 로딩을 끈 채 컴포지션
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(isLoading = false) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }

        // Then 오버레이가 없다
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun ygScaffoldV2_isLoadingTrue_hidesContentFromAccessibility() {
        // Given · When 로딩을 켠 채, 컨텐츠 안에 태그를 단 노드를 넣어 컴포지션
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(isLoading = true) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag(CONTENT_TAG),
                    )
                }
            }
        }

        // Then 컨텐츠가 시맨틱 트리에서 사라진다
        composeTestRule.onNodeWithTag(CONTENT_TAG).assertDoesNotExist()
    }

    @Test
    fun ygScaffoldV2_isLoadingFalse_keepsContentVisibleToAccessibility() {
        // Given · When 로딩을 끈 채, 컨텐츠 안에 태그를 단 노드를 넣어 컴포지션
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(isLoading = false) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag(CONTENT_TAG),
                    )
                }
            }
        }

        // Then 컨텐츠 노드가 그대로 보인다
        composeTestRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun ygScaffoldV2_showErrorWhileLoading_displaysFailToast() {
        // Given 토스트 정책을 테스트가 쥐고, 로딩을 켠 채 컴포지션한다
        val toastPolicy = YGToastPolicy()
        // 토스트는 2초 뒤 스스로 사라진다 — 가상 시간이 저절로 흐르면 단언 전에 없어진다
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            YGCustomTheme {
                YGScaffoldV2(isLoading = true, toastPolicy = toastPolicy) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }

        // When 실패 토스트를 띄우고 진입 애니메이션만큼만 시간을 진행시킨다
        composeTestRule.runOnUiThread { toastPolicy.showError(ERROR_TEXT) }
        composeTestRule.mainClock.advanceTimeBy(TOAST_ENTER_ANIMATION_MILLIS)

        // Then 로딩 오버레이 위로 문구가 보인다
        composeTestRule.onNodeWithText(ERROR_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertIsDisplayed()
    }

    private companion object {
        const val CONTENT_TAG = "content"
        const val ERROR_TEXT = "실패했어요"

        /** `YGToastPolicy` 의 진입 애니메이션(300ms)보다 크고 자동 소멸(2000ms)보다 작아야 한다 */
        const val TOAST_ENTER_ANIMATION_MILLIS = 500L
    }
}
