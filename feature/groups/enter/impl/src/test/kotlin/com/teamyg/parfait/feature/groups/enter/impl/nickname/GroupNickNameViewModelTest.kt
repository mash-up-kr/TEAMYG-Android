package com.teamyg.parfait.feature.groups.enter.impl.nickname

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
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

class GroupNickNameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val changeGroupNickname: ChangeGroupNicknameUseCase = mockk()

    private fun viewModel() = GroupNickNameViewModel(
        groupIdValue = GROUP_ID,
        checkNickNameValid = CheckNameValidUseCase(),
        changeGroupNickname = changeGroupNickname,
    )

    private fun viewModelWith(nickName: String): GroupNickNameViewModel = viewModel().apply {
        processIntent(GroupNickNameIntent.InputWord(nickName))
    }

    private fun givenChangeSucceeds() {
        coEvery { changeGroupNickname(any(), any()) } returns Result.success(
            GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname(NICKNAME)),
        )
    }

    @Test
    fun clickNextButton_succeeds_appliesNicknameToJoinedGroup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 입력한 화면
        givenChangeSucceeds()
        val viewModel = viewModelWith(NICKNAME)

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 참여한 그룹 id 와 함께 닉네임이 나가고 다음 화면으로 간다
            coVerify(exactly = 1) {
                changeGroupNickname(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname(NICKNAME))
            }
            assertEquals(GroupNickNameSideEffect.NavigateToNext, awaitItem())
            assertFalse(viewModel.state.value.isEntering)
        }
    }

    @Test
    fun clickNextButton_invalidNickname_doesNotCallServer() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앞뒤 공백이 있는 닉네임
        givenChangeSucceeds()
        val viewModel = viewModelWith(" 모카")

        // When 확인 버튼 클릭
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 요청 전에 걸러지고 입력 오류만 표시된다
        coVerify(exactly = 0) { changeGroupNickname(any(), any()) }
        assertIs<NameValidResult.Error.SpaceAtEdge>(viewModel.state.value.nicknameError)
    }

    @Test
    fun clickNextButton_serverRejectsNickname_showsInvalidError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 400 INVALID_GROUP_NICKNAME 으로 응답(앱 검증과 서버 규칙이 어긋난 경우)
        coEvery { changeGroupNickname(any(), any()) } returns Result.failure(
            AppError.Server(code = "INVALID_GROUP_NICKNAME", statusCode = 400, serverMessage = "…"),
        )
        val viewModel = viewModelWith(NICKNAME)

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 이동하지 않고 입력 자리에 사유가 붙는다
            expectNoEvents()
            assertEquals(GroupNickNameError.INVALID, viewModel.state.value.submitError)
            assertFalse(viewModel.state.value.isEntering)
        }
    }

    @Test
    fun clickNextButton_networkFails_showsNetworkError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연결 실패
        coEvery { changeGroupNickname(any(), any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModelWith(NICKNAME)

        // When 확인 버튼 클릭
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 네트워크 사유가 붙고 다시 시도할 수 있는 상태로 돌아온다
        assertEquals(GroupNickNameError.NETWORK, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.isEntering)
    }

    @Test
    fun clickNextButton_whileEntering_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임 적용 요청이 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { changeGroupNickname(any(), any()) } coAnswers {
            gate.await()
            Result.success(GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname(NICKNAME)))
        }
        val viewModel = viewModelWith(NICKNAME)
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        runCurrent()

        // When 확인 버튼을 한 번 더 클릭(연타)
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        runCurrent()

        // Then 중복 요청이 나가지 않는다
        coVerify(exactly = 1) { changeGroupNickname(any(), any()) }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun inputWord_afterFailure_clearsBothErrors() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버 실패로 사유가 붙은 화면
        coEvery { changeGroupNickname(any(), any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModelWith(NICKNAME)
        viewModel.processIntent(GroupNickNameIntent.ClickNextButton)
        advanceUntilIdle()

        // When 닉네임을 고친다
        viewModel.processIntent(GroupNickNameIntent.InputWord("라떼"))

        // Then 표시된 사유가 사라진다
        val state = viewModel.state.value
        assertNull(state.submitError)
        assertNull(state.nicknameError)
    }

    private companion object {
        const val GROUP_ID = 7L
        const val NICKNAME = "모카"
    }
}
