package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.CreateGroupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
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

    private val createGroupUseCase: CreateGroupUseCase = mockk()

    private fun viewModel(nickName: String = NICKNAME) = GroupCreateViewModel(
        nickName = nickName,
        checkNameValid = CheckNameValidUseCase(),
        createGroup = createGroupUseCase,
    )

    private val createdGroup = CreatedGroupVO(
        groupId = GroupId(1),
        groupName = GroupName(GROUP_NAME),
        inviteCode = InviteCode("ABC123"),
        memberLimit = MEMBER_LIMIT,
    )

    /** 확인 팝업이 떠 있는 상태까지 몰아간다 — 생성 요청은 이 팝업의 만들기 버튼에서만 나간다 */
    private fun GroupCreateViewModel.fillFormAndOpenConfirmPopup() {
        processIntent(GroupCreateIntent.InputGroupName(GROUP_NAME))
        processIntent(GroupCreateIntent.ClickGroupNumber(MEMBER_LIMIT))
        processIntent(GroupCreateIntent.ClickNextButton)
    }

    @Test
    fun clickConfirmPopupCreate_callsUseCaseWithFormValues() = runTest(mainDispatcherRule.dispatcher) {
        // Given 폼을 채우고 확인 팝업을 띄운 상태
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.success(createdGroup)
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        // When 만들기를 누른다
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        advanceUntilIdle()

        // Then 화면이 들고 있던 그룹명·닉네임·인원수가 그대로 넘어간다
        coVerify(exactly = 1) {
            createGroupUseCase(
                groupName = GroupName(GROUP_NAME),
                groupNickname = GroupNickname(NICKNAME),
                memberLimit = MEMBER_LIMIT,
            )
        }
    }

    @Test
    fun clickConfirmPopupCreate_success_closesPopupAndNavigatesToNext() = runTest(mainDispatcherRule.dispatcher) {
        // Given 생성이 성공하는 서버
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.success(createdGroup)
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        viewModel.effect.test {
            // When 만들기를 누른다
            viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
            advanceUntilIdle()

            // Then 다음 화면으로 가고 팝업·로딩이 정리된다
            assertEquals(GroupCreateSideEffect.NavigateToNext, awaitItem())
            val state = viewModel.state.value
            assertFalse(state.isConfirmPopupVisible)
            assertFalse(state.isCreating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clickConfirmPopupCreate_serverFailure_doesNotNavigate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 비즈니스 에러로 실패
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.failure(
            AppError.Server(code = "INVALID_MEMBER_LIMIT", statusCode = 400, serverMessage = "…"),
        )
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        viewModel.effect.test {
            // When 만들기를 누른다
            viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
            advanceUntilIdle()

            // Then 이동 대신 실패 토스트만 나간다
            assertEquals(GroupCreateSideEffect.ShowError(GroupCreateError.UNKNOWN), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clickConfirmPopupCreate_networkFailure_keepsFormForRetry() = runTest(mainDispatcherRule.dispatcher) {
        // Given 네트워크 단절
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.failure(AppError.Network(null))
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        // When 만들기를 누른다
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        advanceUntilIdle()

        // Then 로딩이 풀리고 입력값이 남아, 확인 버튼으로 팝업을 다시 띄워 재시도할 수 있다
        val state = viewModel.state.value
        assertFalse(state.isConfirmPopupVisible)
        assertFalse(state.isCreating)
        assertEquals(GROUP_NAME, state.groupName)
        assertEquals(MEMBER_LIMIT, state.groupNumber)
    }

    @Test
    fun clickConfirmPopupCreate_useCaseThrows_turnsOffCreatingAndDoesNotNavigate() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given UseCase 가 Result.failure 가 아니라 예외를 그대로 던진다
            coEvery { createGroupUseCase(any(), any(), any()) } throws IllegalStateException("boom")
            val viewModel = viewModel()
            viewModel.fillFormAndOpenConfirmPopup()

            viewModel.effect.test {
                // When 만들기를 누른다
                viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
                advanceUntilIdle()

                // Then 넘어가지 않고, 로딩이 풀려 화면이 오버레이에 갇히지 않는다
                assertEquals(GroupCreateSideEffect.ShowError(GroupCreateError.UNKNOWN), awaitItem())
                assertFalse(viewModel.state.value.isCreating)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun clickConfirmPopupCreate_rapidDoubleClick_requestsOnlyOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 생성이 성공하는 서버
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.success(createdGroup)
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        // When 응답이 오기 전에 두 번 누른다(연타)
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        advanceUntilIdle()

        // Then 그룹이 두 개 만들어지지 않는다
        coVerify(exactly = 1) { createGroupUseCase(any(), any(), any()) }
    }

    @Test
    fun clickConfirmPopupCreate_withoutGroupNumber_doesNotCallUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 인원수를 고르지 않은 상태(멀티터치 등으로 Intent 만 들어온 경우)
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.InputGroupName(GROUP_NAME))

        // When 만들기 Intent 가 들어온다
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
        advanceUntilIdle()

        // Then 요청이 나가지 않는다
        coVerify(exactly = 0) { createGroupUseCase(any(), any(), any()) }
    }

    @Test
    fun clickConfirmPopupCreate_networkFailure_emitsNetworkError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 네트워크 단절
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.failure(AppError.Network(null))
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        viewModel.effect.test {
            // When 만들기를 누른다
            viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)
            advanceUntilIdle()

            // Then 서버 오류와 구분되는 문구를 고를 수 있게 갈래가 실려 나간다
            assertEquals(GroupCreateSideEffect.ShowError(GroupCreateError.NETWORK), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clickConfirmPopupCreate_closesPopupBeforeResponseArrives() = runTest(mainDispatcherRule.dispatcher) {
        // Given 응답이 아직 오지 않은 생성 요청
        coEvery { createGroupUseCase(any(), any(), any()) } returns Result.success(createdGroup)
        val viewModel = viewModel()
        viewModel.fillFormAndOpenConfirmPopup()

        // When 만들기를 누르고 디스패처를 돌리지 않는다
        viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate)

        // Then 팝업은 곧바로 사라지고 로딩만 남는다
        val state = viewModel.state.value
        assertFalse(state.isConfirmPopupVisible)
        assertTrue(state.isCreating)
    }

    @Test
    fun inputNickName_replacesNickNameAndClearsPreviousError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임 규칙을 어겨 에러가 붙은 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.InputNickName("체리!"))
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)
        assertEquals(NameValidResult.Error.InvalidCharacter, viewModel.state.value.nickNameError)

        // When 다시 입력한다
        viewModel.processIntent(GroupCreateIntent.InputNickName("체리"))

        // Then 새 값이 들어가고 에러는 지워진다
        assertEquals("체리", viewModel.state.value.nickName)
        assertNull(viewModel.state.value.nickNameError)
    }

    @Test
    fun clickNextButton_invalidNickName_doesNotOpenPopup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹명·인원수는 멀쩡하고 닉네임만 규칙을 어긴 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupCreateIntent.InputGroupName(GROUP_NAME))
        viewModel.processIntent(GroupCreateIntent.ClickGroupNumber(MEMBER_LIMIT))
        viewModel.processIntent(GroupCreateIntent.InputNickName(" 체리"))

        // When 확인을 누른다
        viewModel.processIntent(GroupCreateIntent.ClickNextButton)

        // Then 팝업 대신 닉네임 자리에 에러가 붙는다
        val state = viewModel.state.value
        assertFalse(state.isConfirmPopupVisible)
        assertEquals(NameValidResult.Error.SpaceAtEdge, state.nickNameError)
    }

    private companion object {
        const val GROUP_NAME = "파르페"
        const val NICKNAME = "체리"
        const val MEMBER_LIMIT = 6
    }
}
