package com.teamyg.parfait.feature.intro.impl.termagree

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.feature.intro.impl.termagree.model.TermContent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TermAgreeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = TermAgreeViewModel()

    @Test
    fun initialState_nothingSelected() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 동의 화면 진입
        val state = viewModel().state.value

        // Then 약관 개수만큼 미선택 상태이고 다음으로 갈 수 없다
        assertEquals(state.termContentList.size, state.selectedList.size)
        assertTrue(state.selectedList.none { it })
        assertFalse(state.isAllSelected)
        assertFalse(state.isAvailable)
    }

    @Test
    fun clickTermAgree_selectsOnlyThatIndex() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 동의 화면
        val viewModel = viewModel()

        // When 첫 번째 약관만 동의
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(index = 0, newSelected = true))

        // Then 해당 항목만 선택된다
        val state = viewModel.state.value
        assertTrue(state.selectedList[0])
        assertTrue(state.selectedList.drop(1).none { it })
    }

    @Test
    fun clickTermAgree_deselect_revertsThatIndex() = runTest(mainDispatcherRule.dispatcher) {
        // Given 전체 동의된 화면
        val viewModel = viewModel()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        // When 첫 번째 약관 동의를 해제
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(index = 0, newSelected = false))

        // Then 해당 항목만 해제되고 전체 동의가 풀린다
        val state = viewModel.state.value
        assertFalse(state.selectedList[0])
        assertTrue(state.selectedList.drop(1).all { it })
        assertFalse(state.isAllSelected)
    }

    @Test
    fun clickAgreeAllTerm_selectsEveryTerm() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 동의 화면
        val viewModel = viewModel()

        // When 전체 동의
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        // Then 모두 선택되고 다음으로 갈 수 있다
        val state = viewModel.state.value
        assertTrue(state.selectedList.all { it })
        assertTrue(state.isAllSelected)
        assertTrue(state.isAvailable)
    }

    @Test
    fun clickAgreeAllTerm_deselect_clearsEveryTerm() = runTest(mainDispatcherRule.dispatcher) {
        // Given 전체 동의된 화면
        val viewModel = viewModel()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        // When 전체 동의를 해제
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = false))

        // Then 모두 해제된다
        val state = viewModel.state.value
        assertTrue(state.selectedList.none { it })
        assertFalse(state.isAvailable)
    }

    @Test
    fun clickTermLandingUrl_emitsNavigateToUrlWithSameUrl() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 동의 화면
        val viewModel = viewModel()

        // When 약관 상세 링크를 클릭
        viewModel.effect.test {
            viewModel.processIntent(TermAgreeIntent.ClickTermLandingUrl("https://example.com/terms"))

            // Then 클릭한 URL 그대로 이동 이펙트가 나간다
            assertEquals(TermAgreeSideEffect.NavigateToUrl("https://example.com/terms"), awaitItem())
        }
    }

    @Test
    fun clickBackButton_emitsNavigateToBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 동의 화면
        val viewModel = viewModel()

        // When 뒤로가기 클릭
        viewModel.effect.test {
            viewModel.processIntent(TermAgreeIntent.ClickBackButton)

            // Then 뒤로가기 이펙트가 나간다
            assertEquals(TermAgreeSideEffect.NavigateToBack, awaitItem())
        }
    }

    @Test
    fun clickNextButton_emitsNavigateToNext() = runTest(mainDispatcherRule.dispatcher) {
        // Given 전체 동의된 화면
        val viewModel = viewModel()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        // When 다음 버튼 클릭
        viewModel.effect.test {
            viewModel.processIntent(TermAgreeIntent.ClickNextButton)

            // Then 다음 화면 이펙트가 나간다
            assertEquals(TermAgreeSideEffect.NavigateToNext, awaitItem())
        }
    }

    @Test
    fun isAvailable_optionalTermUnchecked_staysAvailable() {
        // Given 필수 약관 하나와 선택 약관 하나
        val state = TermAgreeState(
            termContentList = listOf(
                TermContent(isRequired = true, title = "필수 약관", landingUrl = ""),
                TermContent(isRequired = false, title = "선택 약관", landingUrl = ""),
            ),
            selectedList = listOf(true, false),
        )

        // Then 선택 약관은 미동의여도 다음으로 갈 수 있다
        assertTrue(state.isAvailable)
        assertFalse(state.isAllSelected)
    }

    @Test
    fun isAvailable_requiredTermUnchecked_isNotAvailable() {
        // Given 필수 약관만 미동의
        val state = TermAgreeState(
            termContentList = listOf(
                TermContent(isRequired = true, title = "필수 약관", landingUrl = ""),
                TermContent(isRequired = false, title = "선택 약관", landingUrl = ""),
            ),
            selectedList = listOf(false, true),
        )

        // Then 다음으로 갈 수 없다
        assertFalse(state.isAvailable)
    }
}
