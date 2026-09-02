package com.teamyg.parfait.feature.groups.enter.impl.nickname

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
import com.teamyg.parfait.domain.usecase.group.JoinGroupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupNickNameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val joinGroup: JoinGroupUseCase = mockk()
    private val changeGroupNickname: ChangeGroupNicknameUseCase = mockk()

    private fun viewModel() = GroupNickNameViewModel(
        inviteCodeValue = INVITE_CODE,
        groupName = GROUP_NAME,
        checkNickNameValid = CheckNameValidUseCase(),
        joinGroup = joinGroup,
        changeGroupNickname = changeGroupNickname,
    )

    private fun viewModelWith(nickName: String): GroupNickNameViewModel = viewModel().apply {
        processIntent(GroupNickNameIntent.InputWord(nickName))
    }

    /** 확인 버튼까지 눌러 참여 확인 팝업이 떠 있는 화면을 만든다 */
    private fun confirmingViewModel(nickName: String = NICKNAME): GroupNickNameViewModel =
        viewModelWith(nickName).apply {
            processIntent(GroupNickNameIntent.ClickNextButton)
        }

    private fun givenJoinSucceeds() {
        coEvery { joinGroup(any()) } returns Result.success(
            JoinedGroupVO(groupId = GroupId(GROUP_ID), groupName = GroupName(GROUP_NAME)),
        )
    }

    private fun givenChangeSucceeds() {
        coEvery { changeGroupNickname(any(), any()) } returns Result.success(
            GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname(NICKNAME)),
        )
    }

    @Test
    fun clickNextButton_validNickname_showsConfirmPopupWithoutRequest() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 입력한 화면
        val viewModel = viewModelWith(NICKNAME)

        // When 확인 버튼 클릭
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 팝업만 뜨고 참여 요청은 아직 나가지 않는다
        val state = viewModel.state.value
        assertTrue(state.isConfirmPopupVisible)
        assertEquals(GROUP_NAME, state.groupName)
        coVerify(exactly = 0) { joinGroup(any()) }
        coVerify(exactly = 0) { changeGroupNickname(any(), any()) }
    }

    @Test
    fun clickNextButton_invalidNickname_doesNotShowPopup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앞뒤 공백이 있는 닉네임
        val viewModel = viewModelWith(" 모카")

        // When 확인 버튼 클릭
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 팝업까지 가지 않고 입력 오류만 표시된다
        assertFalse(viewModel.state.value.isConfirmPopupVisible)
        assertIs<NameValidResult.Error.SpaceAtEdge>(viewModel.state.value.nicknameError)
    }

    @Test
    fun clickConfirmPopupEnter_succeeds_joinsThenAppliesNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 참여 확인 팝업이 뜬 화면
        givenJoinSucceeds()
        givenChangeSucceeds()
        val viewModel = confirmingViewModel()

        viewModel.effect.test {
            // When 팝업의 참여하기 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
            advanceUntilIdle()

            // Then 참여로 받은 그룹 id 에 닉네임이 붙고 다음 화면으로 간다
            coVerify(exactly = 1) { joinGroup(InviteCode(INVITE_CODE)) }
            coVerify(exactly = 1) {
                changeGroupNickname(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname(NICKNAME))
            }
            assertEquals(
                GroupNickNameSideEffect.NavigateToNext(groupId = GROUP_ID, groupName = GROUP_NAME),
                awaitItem(),
            )
            assertFalse(viewModel.state.value.isConfirmPopupVisible)
            assertFalse(viewModel.state.value.isEntering)
        }
    }

    @Test
    fun clickConfirmPopupEnter_joinFails_closesPopupAndShowsReason() = runTest(mainDispatcherRule.dispatcher) {
        // Given 미리보기 이후 정원이 차 409 GROUP_MEMBER_LIMIT_REACHED 로 응답
        coEvery { joinGroup(any()) } returns Result.failure(
            AppError.Server(code = "GROUP_MEMBER_LIMIT_REACHED", statusCode = 409, serverMessage = "…"),
        )
        val viewModel = confirmingViewModel()

        viewModel.effect.test {
            // When 팝업의 참여하기 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
            advanceUntilIdle()

            // Then 닉네임은 보내지 않고 사유만 알린 뒤 화면에 머문다
            coVerify(exactly = 0) { changeGroupNickname(any(), any()) }
            assertEquals(
                GroupNickNameSideEffect.ShowError(GroupNickNameError.MEMBER_LIMIT_REACHED),
                awaitItem(),
            )
            expectNoEvents()
            assertFalse(viewModel.state.value.isConfirmPopupVisible)
        }
    }

    @Test
    fun clickConfirmPopupEnter_nicknameFails_tellsNicknameKeptBeforeMovingOn() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 참여는 됐지만 서버가 400 INVALID_GROUP_NICKNAME 으로 응답
            givenJoinSucceeds()
            coEvery { changeGroupNickname(any(), any()) } returns Result.failure(
                AppError.Server(code = "INVALID_GROUP_NICKNAME", statusCode = 400, serverMessage = "…"),
            )
            val viewModel = confirmingViewModel()

            viewModel.effect.test {
                // When 팝업의 참여하기 클릭
                viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
                runCurrent()

                // Then 안내를 띄우고, 그것이 사라지기 전에는 화면을 넘기지 않는다
                assertEquals(
                    GroupNickNameSideEffect.ShowError(GroupNickNameError.NICKNAME_NOT_APPLIED),
                    awaitItem(),
                )
                expectNoEvents()

                // 그리고 참여 자체는 끝났으므로 전역 닉네임을 그대로 둔 채 다음 화면으로 간다
                advanceUntilIdle()
                assertEquals(
                    GroupNickNameSideEffect.NavigateToNext(groupId = GROUP_ID, groupName = GROUP_NAME),
                    awaitItem(),
                )
                assertFalse(viewModel.state.value.isConfirmPopupVisible)
            }
        }

    @Test
    fun clickConfirmPopupEnter_networkFails_showsNetworkError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연결 실패
        coEvery { joinGroup(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = confirmingViewModel()

        viewModel.effect.test {
            // When 팝업의 참여하기 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
            advanceUntilIdle()

            // Then 네트워크 사유를 알리고 다시 시도할 수 있는 상태로 돌아온다
            assertEquals(GroupNickNameSideEffect.ShowError(GroupNickNameError.NETWORK), awaitItem())
            assertFalse(viewModel.state.value.isEntering)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clickConfirmPopupEnter_joinThrows_showsUnknownError() = runTest(mainDispatcherRule.dispatcher) {
        // Given UseCase 가 Result.failure 가 아니라 예외를 그대로 던진다
        coEvery { joinGroup(any()) } throws IllegalStateException("boom")
        val viewModel = confirmingViewModel()

        viewModel.effect.test {
            // When 팝업의 참여하기 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
            advanceUntilIdle()

            // Then 로딩만 걷히고 끝나지 않도록 사유를 알린다
            assertEquals(GroupNickNameSideEffect.ShowError(GroupNickNameError.UNKNOWN), awaitItem())
            assertFalse(viewModel.state.value.isEntering)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clickConfirmPopupEnter_closesPopupBeforeResponseArrives() = runTest(mainDispatcherRule.dispatcher) {
        // Given 응답이 아직 오지 않은 참여 요청
        val gate = CompletableDeferred<Unit>()
        coEvery { joinGroup(any()) } coAnswers {
            gate.await()
            Result.success(JoinedGroupVO(groupId = GroupId(GROUP_ID), groupName = GroupName(GROUP_NAME)))
        }
        givenChangeSucceeds()
        val viewModel = confirmingViewModel()

        // When 참여하기를 누른다
        viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
        runCurrent()

        // Then 팝업은 곧바로 사라지고 로딩만 남는다
        assertFalse(viewModel.state.value.isConfirmPopupVisible)
        assertTrue(viewModel.state.value.isEntering)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun clickConfirmPopupEnter_whileEntering_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 참여 요청이 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { joinGroup(any()) } coAnswers {
            gate.await()
            Result.success(JoinedGroupVO(groupId = GroupId(GROUP_ID), groupName = GroupName(GROUP_NAME)))
        }
        givenChangeSucceeds()
        val viewModel = confirmingViewModel()
        viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
        runCurrent()

        // When 참여하기를 한 번 더 클릭(연타)
        viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter)
        runCurrent()

        // Then 중복 요청이 나가지 않는다
        coVerify(exactly = 1) { joinGroup(any()) }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun inputWord_afterInvalidNickname_clearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 규칙을 어겨 입력칸에 사유가 붙은 화면
        val viewModel = viewModelWith(" 모카")
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)

        // When 닉네임을 고친다
        viewModel.processIntent(GroupNickNameIntent.InputWord(OTHER_NICKNAME))

        // Then 표시된 사유가 사라진다
        assertNull(viewModel.state.value.nicknameError)
    }

    private companion object {
        const val INVITE_CODE = "ABCDEF"
        const val GROUP_NAME = "모카의 파르페"
        const val GROUP_ID = 7L
        const val NICKNAME = "모카"
        const val OTHER_NICKNAME = "라떼"
    }
}
