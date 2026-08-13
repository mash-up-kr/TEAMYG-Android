package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.CreateGroupUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupCreateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(nickName: String = "체리마루") = GroupCreateViewModel(
        nickName = nickName,
        checkNameValid = CheckNameValidUseCase(),
        createGroup = CreateGroupUseCase(),
    )

    private fun readyToCreateViewModel() = viewModel().apply {
        processIntent(GroupCreateIntent.InputGroupName("모카의 파르페"))
        processIntent(GroupCreateIntent.ClickGroupNumber(4))
    }

    @Test
    fun initialState_holdsGivenNickName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이전 화면에서 받은 닉네임으로 진입
        val state = viewModel(nickName = "체리마루").state.value

        // Then 닉네임이 초기 상태에 들어가고 아직 유효하지 않다
        assertEquals("체리마루", state.nickName)
        assertFalse(state.isValid)
    }

    @Test
    fun isValid_requiresGroupNameAndGroupNumber() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹 생성 화면
        val viewModel = viewModel()

        // When 그룹명만 입력
        viewModel.processIntent(GroupCreateIntent.InputGroupName("모카의 파르페"))

        // Then 인원수가 없으면 아직 유효하지 않다
        assertFalse(viewModel.state.value.isValid)

        // When 인원수까지 선택
        viewModel.processIntent(GroupCreateIntent.ClickGroupNumber(4))

        // Then 유효해진다
        assertTrue(viewModel.state.value.isValid)
        assertEquals(4, viewModel.state.value.groupNumber)
    }

    @Test
    fun inputGroupName_clearsPreviousError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 에러가 떠 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        // When 그룹명을 다시 입력
        viewModel.processIntent(GroupCreateIntent.InputGroupName("모카의 파르페"))

        // Then 에러가 지워진다
        assertNull(viewModel.state.value.groupNameError)
    }

    @Test
    fun clickNextButton_invalidGroupName_setsErrorAndKeepsPopupHidden() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앞뒤에 공백이 있는 그룹명
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.InputGroupName(" 모카의 파르페"))

        // When 다음 버튼 클릭
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        // Then 에러가 표시되고 확인 팝업은 뜨지 않는다
        assertEquals(NameValidResult.Error.SpaceAtEdge, viewModel.state.value.groupNameError)
        assertFalse(viewModel.state.value.isConfirmPopupVisible)
    }

    @Test
    fun clickNextButton_validGroupName_showsConfirmPopup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 유효한 그룹명
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.InputGroupName("모카의 파르페"))

        // When 다음 버튼 클릭
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        // Then 확인 팝업이 뜬다
        assertTrue(viewModel.state.value.isConfirmPopupVisible)
        assertNull(viewModel.state.value.groupNameError)
    }

    @Test
    fun clickConfirmPopupCreate_createsGroupAndNavigatesToNext() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹명과 인원수가 채워진 화면
        val viewModel = readyToCreateViewModel()
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        viewModel.effect.test {
            // When 팝업에서 만들기 클릭
            viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
            runCurrent()

            // Then 생성 중 상태가 되었다가
            assertTrue(viewModel.state.value.isCreating)

            advanceUntilIdle()

            // Then 생성이 끝나면 팝업이 닫히고 다음 화면으로 이동한다
            assertEquals(GroupCreateSideEffect.NavigateToNext, awaitItem())
            assertFalse(viewModel.state.value.isCreating)
            assertFalse(viewModel.state.value.isConfirmPopupVisible)
        }
    }

    @Test
    fun clickConfirmPopupCreate_withoutGroupNumber_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        // Given 인원수를 고르지 않은 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.InputGroupName("모카의 파르페"))
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        viewModel.effect.test {
            // When 팝업에서 만들기 클릭
            viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
            advanceUntilIdle()

            // Then 생성을 시작하지 않는다
            assertFalse(viewModel.state.value.isCreating)
            expectNoEvents()
        }
    }

    @Test
    fun clickConfirmPopupCreate_whileCreating_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        // Given 생성 요청이 진행 중인 화면
        val viewModel = readyToCreateViewModel()
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        runCurrent()

        viewModel.effect.test {
            // When 만들기를 한 번 더 클릭
            viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
            advanceUntilIdle()

            // Then 중복 생성 없이 이동 이펙트는 한 번만 나간다
            assertEquals(GroupCreateSideEffect.NavigateToNext, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun dismissConfirmPopup_whileCreating_keepsPopupVisible() = runTest(mainDispatcherRule.dispatcher) {
        // Given 생성 요청이 진행 중인 화면
        val viewModel = readyToCreateViewModel()
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        runCurrent()

        // When 팝업을 닫으려 시도
        viewModel.processIntent(GroupCreateIntent.DismissConfirmPopup)

        // Then 생성 중에는 닫히지 않는다
        assertTrue(viewModel.state.value.isConfirmPopupVisible)
    }

    @Test
    fun dismissConfirmPopup_whenIdle_hidesPopup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 확인 팝업이 떠 있는 화면
        val viewModel = readyToCreateViewModel()
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        // When 팝업을 닫음
        viewModel.processIntent(GroupCreateIntent.DismissConfirmPopup)

        // Then 팝업이 닫힌다
        assertFalse(viewModel.state.value.isConfirmPopupVisible)
    }

    @Test
    fun clickBackButton_emitsNavigateToBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹 생성 화면
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 뒤로가기 클릭
            viewModel.processIntent(GroupCreateIntent.ClickBackButton)

            // Then 뒤로가기 이펙트가 나간다
            assertEquals(GroupCreateSideEffect.NavigateToBack, awaitItem())
        }
    }
}
