package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.usecase.group.GetGroupJoinPreviewUseCase
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
import kotlin.test.assertNull

class GroupInviteCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getGroupJoinPreview: GetGroupJoinPreviewUseCase = mockk()

    private fun viewModel() = GroupInviteCodeViewModel(getGroupJoinPreview = getGroupJoinPreview)

    /** 초대코드를 다 채운 화면을 만든다 — 조회는 코드가 다 차야 나간다 */
    private fun filledViewModel(): GroupInviteCodeViewModel = viewModel().apply {
        processIntent(GroupInviteCodeIntent.InputWord(index = 0, word = INVITE_CODE))
    }

    private fun givenPreviewSucceeds() {
        coEvery { getGroupJoinPreview(any()) } returns Result.success(GroupName(GROUP_NAME))
    }

    @Test
    fun clickNextButton_previewSucceeds_navigatesWithCodeAndGroupName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초대코드를 다 입력한 화면
        givenPreviewSucceeds()
        val viewModel = filledViewModel()

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 참여는 다음 화면에서 하므로, 초대코드와 그룹명을 들고 바로 넘어간다
            assertEquals(
                GroupInviteCodeSideEffect.NavigateToNext(inviteCode = INVITE_CODE, groupName = GROUP_NAME),
                awaitItem(),
            )
            assertNull(viewModel.state.value.inviteCodeError)
            coVerify(exactly = 1) { getGroupJoinPreview(InviteCode(INVITE_CODE)) }
        }
    }

    @Test
    fun clickNextButton_codeNotFilled_doesNotCallServer() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초대코드를 덜 입력한 화면
        givenPreviewSucceeds()
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.InputWord(index = 0, word = "ABC"))

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 조회 요청 자체가 나가지 않고 화면도 그대로다
            coVerify(exactly = 0) { getGroupJoinPreview(any()) }
            expectNoEvents()
        }
    }

    @Test
    fun clickNextButton_invalidInviteCode_showsInvalidCodeError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 404 INVALID_INVITE_CODE 로 응답
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(
            AppError.Server(code = "INVALID_INVITE_CODE", statusCode = 404, serverMessage = "…"),
        )
        val viewModel = filledViewModel()

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 넘어가지 않고 입력 자리에 사유가 붙는다
            expectNoEvents()
            assertEquals(InviteCodeError.INVALID_CODE, viewModel.state.value.inviteCodeError)
        }
    }

    @Test
    fun clickNextButton_alreadyJoined_showsAlreadyJoinedError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 409 GROUP_ALREADY_JOINED 로 응답
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(
            AppError.Server(code = "GROUP_ALREADY_JOINED", statusCode = 409, serverMessage = "…"),
        )
        val viewModel = filledViewModel()

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 참여를 시도하기 전에 여기서 막힌다
            expectNoEvents()
            assertEquals(InviteCodeError.ALREADY_JOINED, viewModel.state.value.inviteCodeError)
        }
    }

    @Test
    fun clickNextButton_memberLimitReached_showsMemberLimitError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 정원이 찬 그룹이라 409 GROUP_MEMBER_LIMIT_REACHED 로 응답
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(
            AppError.Server(code = "GROUP_MEMBER_LIMIT_REACHED", statusCode = 409, serverMessage = "…"),
        )
        val viewModel = filledViewModel()

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 정원 초과 사유가 붙는다
            expectNoEvents()
            assertEquals(InviteCodeError.MEMBER_LIMIT_REACHED, viewModel.state.value.inviteCodeError)
        }
    }

    @Test
    fun clickNextButton_networkFails_showsNetworkError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연결 실패
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = filledViewModel()

        // When 확인 버튼 클릭
        viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 네트워크 사유가 붙고 버튼은 다시 눌릴 수 있다
        assertEquals(InviteCodeError.NETWORK, viewModel.state.value.inviteCodeError)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun clickNextButton_whileLoading_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { getGroupJoinPreview(any()) } coAnswers {
            gate.await()
            Result.success(GroupName(GROUP_NAME))
        }
        val viewModel = filledViewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
        runCurrent()

        // When 확인 버튼을 한 번 더 클릭(연타)
        viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
        runCurrent()

        // Then 중복 요청이 나가지 않는다
        coVerify(exactly = 1) { getGroupJoinPreview(any()) }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun inputWord_afterFailure_clearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 사유가 붙은 화면
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = filledViewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
        advanceUntilIdle()

        // When 초대코드를 고친다
        viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 0))
        viewModel.processIntent(GroupInviteCodeIntent.InputWord(index = 0, word = "Z"))

        // Then 사유 표시가 사라진다
        assertNull(viewModel.state.value.inviteCodeError)
    }

    @Test
    fun clipboardCodeDetected_whileKeyboardIsShown_showsPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 올라와 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)

        // When 클립보드에서 초대코드를 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(INVITE_CODE))

        // Then 붙여넣기 바가 노출된다
        assertEquals(INVITE_CODE, viewModel.state.value.pasteBarInviteCode)
    }

    @Test
    fun clipboardCodeDetected_whileKeyboardIsHidden_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 내려가 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.HideKeyboard)

        // When 클립보드에서 초대코드를 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(INVITE_CODE))

        // Then 붙여넣기 바는 노출되지 않는다
        assertNull(viewModel.state.value.pasteBarInviteCode)
    }

    @Test
    fun clipboardCodeDetected_null_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 올라와 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)

        // When 클립보드에 초대코드가 없음
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(null))

        // Then 붙여넣기 바는 노출되지 않는다
        assertNull(viewModel.state.value.pasteBarInviteCode)
    }

    @Test
    fun clipboardCodeDetected_sameCodeAlreadyTyped_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 같은 코드를 입력한 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)
        viewModel.processIntent(GroupInviteCodeIntent.InputWord(index = 0, word = INVITE_CODE))

        // When 같은 코드가 클립보드에서 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(INVITE_CODE))

        // Then 붙여넣기 바는 노출되지 않는다
        assertNull(viewModel.state.value.pasteBarInviteCode)
    }

    @Test
    fun clickPasteInviteCode_fillsCodeAndHidesKeyboard() = runTest(mainDispatcherRule.dispatcher) {
        // Given 붙여넣기 바가 노출된 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(INVITE_CODE))

        // When 붙여넣기 바를 클릭
        viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode)

        // Then 코드가 채워지고 키보드가 내려가며 바가 사라진다
        val state = viewModel.state.value
        assertEquals(INVITE_CODE, state.text)
        assertNull(state.focusedIndex)
        assertEquals(InputMode.ADD, state.inputMode)
        assertNull(state.clipboardInviteCode)
        assertNull(state.pasteBarInviteCode)
    }

    @Test
    fun clickPasteInviteCode_withoutDetectedCode_keepsState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 클립보드에 초대코드가 없는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)

        // When 붙여넣기 인텐트가 들어옴
        viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode)

        // Then 입력값은 그대로다
        assertEquals("", viewModel.state.value.text)
        assertEquals(0, viewModel.state.value.focusedIndex)
    }

    private companion object {
        const val INVITE_CODE = "ABCDEF"
        const val GROUP_NAME = "모카의 파르페"
    }
}
