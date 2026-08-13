package com.teamyg.parfait.feature.groups.enter.impl.nickname

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.EnterGroupUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupNickNameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = GroupNickNameViewModel(
        checkNickNameValid = CheckNameValidUseCase(),
        enterGroup = EnterGroupUseCase(),
    )

    @Test
    fun inputWord_updatesNickNameAndClearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 에러가 떠 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)

        // When 닉네임을 입력
        viewModel.processIntent(GroupNickNameIntent.InputWord("체리마루"))

        // Then 입력값이 반영되고 에러가 지워진다
        assertEquals("체리마루", viewModel.state.value.nickName)
        assertNull(viewModel.state.value.nicknameError)
    }

    @Test
    fun clickNextButton_emptyNickName_setsEmptyStringError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 비워둔 화면
        val viewModel = viewModel()

        // When 다음 버튼 클릭
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)

        // Then 빈 값 에러가 표시되고 참여를 시작하지 않는다
        assertEquals(NameValidResult.Error.EmptyString, viewModel.state.value.nicknameError)
        assertFalse(viewModel.state.value.isEntering)
    }

    @Test
    fun clickNextButton_invalidCharacter_setsInvalidCharacterError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 특수문자가 들어간 닉네임
        val viewModel = viewModel()
        viewModel.processIntent(GroupNickNameIntent.InputWord("체리@마루"))

        // When 다음 버튼 클릭
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)

        // Then 문자 종류 에러가 표시된다
        assertEquals(NameValidResult.Error.InvalidCharacter, viewModel.state.value.nicknameError)
    }

    @Test
    fun clickNextButton_validNickName_entersGroupAndNavigatesToNext() = runTest(mainDispatcherRule.dispatcher) {
        // Given 유효한 닉네임
        val viewModel = viewModel()
        viewModel.processIntent(GroupNickNameIntent.InputWord("체리마루"))

        viewModel.effect.test {
            // When 다음 버튼 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
            runCurrent()

            // Then 참여 중 상태가 되었다가
            assertTrue(viewModel.state.value.isEntering)

            advanceUntilIdle()

            // Then 참여가 끝나면 다음 화면으로 이동한다
            assertEquals(GroupNickNameSideEffect.NavigateToNext, awaitItem())
            assertFalse(viewModel.state.value.isEntering)
            assertNull(viewModel.state.value.nicknameError)
        }
    }

    @Test
    fun clickNextButton_whileEntering_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        // Given 참여 요청이 진행 중인 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupNickNameIntent.InputWord("체리마루"))
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        runCurrent()

        viewModel.effect.test {
            // When 다음 버튼을 한 번 더 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 중복 요청 없이 이동 이펙트는 한 번만 나간다
            assertEquals(GroupNickNameSideEffect.NavigateToNext, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun clickBackButton_emitsNavigateToBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임 입력 화면
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 뒤로가기 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickBackButton)

            // Then 뒤로가기 이펙트가 나간다
            assertEquals(GroupNickNameSideEffect.NavigateToBack, awaitItem())
        }
    }
}
