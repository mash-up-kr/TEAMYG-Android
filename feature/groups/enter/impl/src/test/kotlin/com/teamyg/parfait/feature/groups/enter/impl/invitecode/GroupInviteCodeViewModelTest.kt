package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.usecase.group.GetGroupJoinPreviewUseCase
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupInviteCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getGroupJoinPreview: GetGroupJoinPreviewUseCase = mockk()
    private val getMyAccountFlow: GetMyAccountFlowUseCase = mockk()

    private fun viewModel(accountFlow: Flow<MyAccountVO?> = flowOf(ACCOUNT)): GroupInviteCodeViewModel {
        every { getMyAccountFlow() } returns accountFlow
        return GroupInviteCodeViewModel(
            getGroupJoinPreview = getGroupJoinPreview,
            getMyAccountFlow = getMyAccountFlow,
        )
    }

    /** 초대코드를 다 채운 화면을 만든다 — 조회는 코드가 다 차야 나간다 */
    private fun filledViewModel(accountFlow: Flow<MyAccountVO?> = flowOf(ACCOUNT)): GroupInviteCodeViewModel =
        viewModel(accountFlow).apply {
            processIntent(GroupInviteCodeIntent.ChangeText(text = INVITE_CODE, cursor = INVITE_CODE.length))
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
                GroupInviteCodeSideEffect.NavigateToNext(
                    inviteCode = INVITE_CODE,
                    groupName = GROUP_NAME,
                    nickName = NICKNAME,
                ),
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
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABC", cursor = 3))

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
    fun changeText_afterFailure_clearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 사유가 붙은 화면
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = filledViewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
        advanceUntilIdle()

        // When 초대코드를 고친다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCDE", cursor = 5))

        // Then 사유 표시가 사라진다
        assertNull(viewModel.state.value.inviteCodeError)
    }

    @Test
    fun clipboardCodeDetected_whileKeyboardIsShown_showsPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 올라와 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)

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
        viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)

        // When 클립보드에 초대코드가 없음
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(null))

        // Then 붙여넣기 바는 노출되지 않는다
        assertNull(viewModel.state.value.pasteBarInviteCode)
    }

    @Test
    fun clipboardCodeDetected_sameCodeAlreadyTyped_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 같은 코드를 입력한 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = INVITE_CODE, cursor = INVITE_CODE.length))

        // When 같은 코드가 클립보드에서 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(INVITE_CODE))

        // Then 붙여넣기 바는 노출되지 않는다
        assertNull(viewModel.state.value.pasteBarInviteCode)
    }

    @Test
    fun clickPasteInviteCode_fillsCodeAndHidesKeyboard() = runTest(mainDispatcherRule.dispatcher) {
        // Given 붙여넣기 바가 노출된 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(INVITE_CODE))

        // When 붙여넣기 바를 클릭
        viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode)

        // Then 코드가 채워지고 키보드가 내려가며 바가 사라진다
        val state = viewModel.state.value
        assertEquals(INVITE_CODE, state.text)
        assertEquals(LAST_INDEX, state.focusedIndex)
        assertFalse(state.isFocused)
        assertNull(state.clipboardInviteCode)
        assertNull(state.pasteBarInviteCode)
    }

    @Test
    fun clickPasteInviteCode_withoutDetectedCode_keepsState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 클립보드에 초대코드가 없는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)

        // When 붙여넣기 인텐트가 들어옴
        viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode)

        // Then 입력값은 그대로다
        assertEquals("", viewModel.state.value.text)
        assertEquals(0, viewModel.state.value.focusedIndex)
        assertTrue(viewModel.state.value.isFocused)
    }

    @Test
    fun clickNextButton_accountNotEmitted_navigatesWithEmptyNickName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 계정 스트림이 아직 값을 내지 않은 화면
        givenPreviewSucceeds()
        val viewModel = filledViewModel(accountFlow = flowOf(null))

        viewModel.effect.test {
            // When 확인 버튼 클릭
            viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 참여를 막지 않고 닉네임만 비운 채 넘어간다
            assertEquals(
                GroupInviteCodeSideEffect.NavigateToNext(
                    inviteCode = INVITE_CODE,
                    groupName = GROUP_NAME,
                    nickName = "",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun accountStreamEmitsAgain_followsNewNickName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 계정 SSoT 를 구독해 첫 값을 받은 화면
        val accountFlow = MutableStateFlow<MyAccountVO?>(ACCOUNT)
        val viewModel = viewModel(accountFlow)
        advanceUntilIdle()
        assertEquals(NICKNAME, viewModel.state.value.nickName)

        // When 다른 화면에서 닉네임을 바꿔 SSoT 가 새 값을 밀어 준다
        accountFlow.value = ACCOUNT.copy(nickname = GlobalNickname(OTHER_NICKNAME))
        advanceUntilIdle()

        // Then 다음 화면으로 실려 갈 값이 낡지 않도록 구독이 따라간다
        assertEquals(OTHER_NICKNAME, viewModel.state.value.nickName)
    }

    @Test
    fun changeText_deletesLastChar_shortensTextAndMovesCursorBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초대코드를 다 입력한 화면
        val viewModel = filledViewModel()

        // When 마지막 글자를 지운다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCDE", cursor = 5))

        // Then 칸을 직접 누르지 않아도 지워지고, 포커스는 이제 마지막 글자가 된 자리로 물러난다
        assertEquals("ABCDE", viewModel.state.value.text)
        assertEquals(4, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_deletesRepeatedly_emptiesTextWithoutReselectingCells() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초대코드를 다 입력한 화면
        val viewModel = filledViewModel()

        // When 칸을 다시 고르지 않고 지우기만 이어서 누른다
        listOf("ABCDE", "ABCD", "ABC", "AB", "A", "").forEachIndexed { index, remaining ->
            viewModel.processIntent(
                GroupInviteCodeIntent.ChangeText(text = remaining, cursor = INVITE_CODE.length - index - 1),
            )
        }

        // Then 여섯 글자가 모두 지워진다
        assertEquals("", viewModel.state.value.text)
        assertEquals(0, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_deletesMiddleChar_pullsFollowingCharsForward() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초대코드 ABCDEF 를 입력한 화면 — 포커스는 마지막 칸(5)에 있다
        val viewModel = filledViewModel()

        // When 세 번째 글자 C 를 지운다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABDEF", cursor = 2))

        // Then 뒤 글자가 당겨오고, 기존 포커스(5)는 5글자짜리 텍스트엔 더는 없는 자리라
        // 새 마지막 칸(4)으로 당겨진다
        assertEquals("ABDEF", viewModel.state.value.text)
        assertEquals(4, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_tapsMiddleCellThenDeletes_removesThatCharAndHoldsFocusThere() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 세 번째 칸(C)을 직접 눌러 고른 상태
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 2))

            // When 그 칸에서 지우기를 누른다 — 커서가 눌린 칸 뒤에 있어 그 칸 자신이 지워진다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABDEF", cursor = 2))

            // Then 누른 글자(C)가 지워지고 뒤 글자가 당겨오지만, 포커스는 누른 자리(그 뒤로 밀려온
            // D)에 그대로 남는다 — 빈 칸(끝)으로 점프하지 않는다
            assertEquals("ABDEF", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_tapsMiddleCellThenDeletesRepeatedly_holdsFocusUntilTailConsumedThenTracksEnd() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 세 번째 칸(C)을 직접 눌러 고른 상태
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 2))

            // When 칸을 다시 고르지 않고 지우기만 이어서 누른다 — 눌린 자리로 밀려온 글자를
            // 계속 지우는 동안은 포커스가 그 자리에 그대로 있다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABDEF", cursor = 2))
            assertEquals("ABDEF", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABEF", cursor = 2))
            assertEquals("ABEF", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABF", cursor = 2))
            assertEquals("ABF", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)

            // Then 눌린 자리 뒤로 밀려올 글자가 더는 없으면(F까지 지워 "AB"만 남으면), 그때부터는
            // 포커스가 한 칸씩 앞으로 물러나며 끝에서부터 지워진다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "AB", cursor = 2))
            assertEquals("AB", viewModel.state.value.text)
            assertEquals(1, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "A", cursor = 1))
            assertEquals("A", viewModel.state.value.text)
            assertEquals(0, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "", cursor = 0))
            assertEquals("", viewModel.state.value.text)
            assertEquals(0, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_deletesMultipleCharsFromMiddleAtOnce_shrinksCorrectlyWithoutShifting() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 세 번째 칸(C)을 직접 눌러 고른 상태
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 2))

            // When 가운데 세 글자(C·D·E)를 한 번에 선택해서 지운다 — 텍스트 필드가 한 번의
            // 편집으로 여러 글자를 한꺼번에 줄인 결과를 준다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABF", cursor = 2))

            // Then 선택한 세 글자만 사라지고 나머지는 순서 그대로 남는다 — 삽입 판별 로직이
            // 글자 수가 준 경우까지 건드리지 않는다. 포커스는 누른 자리(이제 F)에 그대로 남는다
            assertEquals("ABF", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_dropsCharsOutsideCodeAlphabet() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 초대코드에 쓸 수 없는 글자가 섞여 들어온다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "A1가 b!", cursor = 6))

        // Then 영문·숫자만 남는다
        assertEquals("A1b", viewModel.state.value.text)
    }

    @Test
    fun changeText_dropsCharsBeforeCursor_movesCursorToSurvivingLength() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 커서 앞의 글자 일부가 걸러진다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "AB가", cursor = 3))

        // Then 포커스가 살아남은 글자 뒤 빈 칸에 놓여, 다음 입력이 엉뚱한 칸으로 가지 않는다
        assertEquals("AB", viewModel.state.value.text)
        assertEquals(2, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_longerThanCodeLength_truncatesToCodeLength() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 코드 길이보다 긴 텍스트가 붙여넣기로 들어온다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCDEFGH", cursor = 8))

        // Then 코드 길이만큼만 남고, 더 갈 칸이 없어 포커스가 마지막 칸에 멈춘다
        assertEquals(INVITE_CODE, viewModel.state.value.text)
        assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
    }

    @Test
    fun selectedTextFieldElement_beyondTypedLength_clampsCursorToTextEnd() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세 글자만 입력한 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABC", cursor = 3))

        // When 아직 비어 있는 다섯 번째 칸을 누른다
        viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 5))

        // Then 포커스는 입력한 글자 끝에 멈춘다 — 빈칸 사이에 구멍이 생기지 않는다
        assertEquals(3, viewModel.state.value.focusedIndex)
        assertTrue(viewModel.state.value.isFocused)
    }

    @Test
    fun requestFocus_showsKeyboardAtFirstCell() = runTest(mainDispatcherRule.dispatcher) {
        // Given 막 진입한 화면
        val viewModel = viewModel()

        // When 진입 포커스 요청
        viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)

        // Then 첫 칸에 포커스가 놓이고 키보드가 올라온다
        assertEquals(0, viewModel.state.value.focusedIndex)
        assertTrue(viewModel.state.value.isFocused)
    }

    @Test
    fun changeText_fillsEveryCell_focusStopsAtLastCell() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 마지막 칸까지 채운다
        viewModel.processIntent(
            GroupInviteCodeIntent.ChangeText(text = INVITE_CODE, cursor = INVITE_CODE.length),
        )

        // Then 더 갈 칸이 없으므로 포커스가 마지막 칸에 멈춘다 — 표시가 사라지지 않는다
        assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
    }

    @Test
    fun cursor_focusedCellIsFilled_sitsAfterThatChar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 마지막 칸까지 채운 화면 — 포커스 칸에 글자가 있다
        val viewModel = filledViewModel()

        // When·Then 지우기가 포커스 칸의 글자를 지우도록 커서가 그 뒤에 놓인다
        assertEquals(INVITE_CODE.length, viewModel.state.value.cursor)
    }

    @Test
    fun cursor_focusedCellIsEmpty_sitsAtThatCell() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세 글자만 입력한 화면 — 포커스 칸이 비어 있다
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABC", cursor = 3))

        // When·Then 지우기가 앞 칸의 글자를 지우도록 커서가 포커스 칸에 놓인다
        assertEquals(3, viewModel.state.value.cursor)
    }

    @Test
    fun selectedTextFieldElement_filledCell_cursorSitsAfterThatChar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 한 글자만 입력한 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "A", cursor = 1))

        // When 그 글자가 있는 칸을 누른다
        viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 0))

        // Then 누른 칸의 글자가 지워지도록 커서가 그 뒤에 놓인다 — 앞 글자를 지워 0자로 못 내려가던 문제
        assertEquals(0, viewModel.state.value.focusedIndex)
        assertEquals(1, viewModel.state.value.cursor)
    }

    @Test
    fun changeText_insertsIntoFullCode_dropsOverflowingTail() = runTest(mainDispatcherRule.dispatcher) {
        // Given 마지막 칸까지 채운 화면
        val viewModel = filledViewModel()

        // When 네 번째 자리에 글자를 끼워 넣는다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCXDEF", cursor = 4))

        // Then 끼운 글자는 남고 넘치는 뒤쪽이 잘린다
        assertEquals("ABCXDE", viewModel.state.value.text)
    }

    @Test
    fun changeText_typesOverFilledMiddleCell_replacesOnlyThatCharWithoutShiftingRest() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 세 번째 칸(C)을 다시 누른 상태
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 2))

            // When 그 칸에 새 글자를 입력한다 — 텍스트 필드는 커서 위치에 글자를 끼워 넣으므로
            // 눌린 칸 다음 글자부터 밀린 텍스트가 들어온다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCXDEF", cursor = 4))

            // Then 누른 칸만 새 글자로 바뀌고 뒤 칸은 밀리지 않는다(main 브랜치 정책)
            assertEquals("ABXDEF", viewModel.state.value.text)
            assertEquals(3, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_typesOverFilledCell_whileCodeNotFullyTyped_replacesOnlyThatChar() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 세 글자만 입력한 화면에서 두 번째 칸(B)을 다시 누른 상태
            val viewModel = viewModel()
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABC", cursor = 3))
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 1))

            // When 그 칸에 새 글자를 입력한다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABXC", cursor = 3))

            // Then 코드가 다 차지 않은 상태에서도 누른 칸만 바뀌고 뒤는 밀리지 않는다
            assertEquals("AXC", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_multiCharCommitOverFilledCell_replacesInPlaceWithoutShifting() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 두 번째 칸(B)을 다시 누른 상태
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 1))

            // When 자동완성·스와이프 입력처럼 두 글자("XY")가 한 번의 편집으로 한꺼번에 끼워 넣어진다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABXYCDEF", cursor = 4))

            // Then 두 글자 모두 누른 칸부터 순서대로 그 칸들을 대신하고, 그 뒤는 밀리지 않는다
            assertEquals("AXYDEF", viewModel.state.value.text)
            assertEquals(3, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_multiCharCommitOverflowsRemainingCells_replacesThoseCellsAndDropsOverflow() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 다섯 번째 칸(E)을 다시 누른 상태 — 대신할 칸이 E·F 둘뿐이다
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 4))

            // When 세 글자("XYZ")가 한 번에 끼워 넣어져, 남은 칸 수보다 많다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCDEXYZF", cursor = 8))

            // Then 남은 두 칸(E·F)이 앞에서부터 순서대로 바뀌고, 더 들어갈 칸이 없는 나머지는 잘려 나간다
            assertEquals("ABCDXY", viewModel.state.value.text)
            assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_typesOverMiddleCellRepeatedlyWithoutReselecting_replacesEachCellThroughToTheEnd() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 다 채운 화면에서 세 번째 칸(C)을 다시 누른 상태
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 2))

            // When 칸을 다시 고르지 않고 마지막 칸까지 이어서 타이핑한다 — 텍스트 필드는 매번
            // 그 시점 커서 자리에 새 글자를 끼워 넣은 원본을 준다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCVDEF", cursor = 4))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVDWEF", cursor = 5))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVWEXF", cursor = 6))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVWXFY", cursor = 7))

            // Then 칸마다 순서대로 밀리지 않고 바뀌어 마지막 칸까지 정상 도달한다
            assertEquals("ABVWXY", viewModel.state.value.text)
            assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_continuesTypingAfterReachingLastCell_keepsReplacingLastCellWithoutGrowingOrCorrupting() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 마지막 칸까지 이어서 고친 화면(포커스가 마지막 칸에 멈춰 있다)
            val viewModel = filledViewModel()
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 2))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCVDEF", cursor = 4))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVDWEF", cursor = 5))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVWEXF", cursor = 6))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVWXFY", cursor = 7))

            // When 마지막 칸에서도 칸을 다시 고르지 않고 계속 입력한다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVWXYZ", cursor = 7))
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABVWXZQ", cursor = 7))

            // Then 6자를 넘어가거나 깨지지 않고, 마지막 칸만 계속 새 글자로 바뀐다
            assertEquals("ABVWXQ", viewModel.state.value.text)
            assertEquals(InviteCode.LENGTH, viewModel.state.value.text.length)
            assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_tapsMiddleCellOfPartialCode_replacesThroughTailThenAppendsPastIt() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 세 글자(ABC)만 입력한 화면에서 두 번째 칸(B)을 다시 누른 상태
            val viewModel = viewModel()
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABC", cursor = 3))
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = 1))

            // When 칸을 다시 고르지 않고 이어서 E 를 계속 입력한다 — 처음 두 번은 B, C 가
            // 있던 자리를 교체하고, 그 뒤로는 채울 칸이 없어 끝에 그대로 추가된다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABEC", cursor = 3))
            assertEquals("AEC", viewModel.state.value.text)
            assertEquals(2, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "AECE", cursor = 4))
            assertEquals("AEE", viewModel.state.value.text)
            assertEquals(3, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "AEEE", cursor = 4))
            assertEquals("AEEE", viewModel.state.value.text)
            assertEquals(4, viewModel.state.value.focusedIndex)

            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "AEEEE", cursor = 5))
            assertEquals("AEEEE", viewModel.state.value.text)
            assertEquals(5, viewModel.state.value.focusedIndex)

            // Then 6자가 꽉 차면 더 갈 칸이 없어 포커스가 마지막 칸에 멈춘다
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "AEEEEE", cursor = 6))
            assertEquals("AEEEEE", viewModel.state.value.text)
            assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
        }

    @Test
    fun changeText_typesOverFilledLastCell_replacesOnlyThatChar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 다 채운 화면에서 마지막 칸(F)을 다시 누른 상태
        val viewModel = filledViewModel()
        viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index = LAST_INDEX))

        // When 그 칸에 새 글자를 입력한다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCDEFX", cursor = 7))

        // Then 마지막 칸만 바뀌고 칸 수는 그대로다
        assertEquals("ABCDEX", viewModel.state.value.text)
        assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_headAloneFillsCodeLength_keepsFocusAtLastCell() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 커서 앞부분만으로 이미 코드 길이를 넘기는 텍스트가 들어온다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABCDEFGHI", cursor = 9))

        // Then 포커스가 칸 수를 넘어가지 않는다
        assertEquals(INVITE_CODE, viewModel.state.value.text)
        assertEquals(LAST_INDEX, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_everyCharDropped_emptiesText() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 쓸 수 있는 글자가 하나도 없는 텍스트가 들어온다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "가나다", cursor = 3))

        // Then 아무것도 남지 않고 포커스는 첫 칸이다
        assertEquals("", viewModel.state.value.text)
        assertEquals(0, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_cursorBeyondTextLength_isClamped() = runTest(mainDispatcherRule.dispatcher) {
        // Given 빈 화면
        val viewModel = viewModel()

        // When 텍스트 길이를 넘는 커서가 들어온다
        viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = "ABC", cursor = 99))

        // Then 텍스트도 포커스도 성한다
        assertEquals("ABC", viewModel.state.value.text)
        assertEquals(3, viewModel.state.value.focusedIndex)
    }

    @Test
    fun changeText_everyCharDroppedSoTextIsUnchanged_keepsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 사유가 붙은 화면
        coEvery { getGroupJoinPreview(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = filledViewModel()
        viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton)
        advanceUntilIdle()

        // When 걸러질 글자를 눌러 코드가 그대로다
        viewModel.processIntent(
            GroupInviteCodeIntent.ChangeText(text = INVITE_CODE + "가", cursor = INVITE_CODE.length + 1),
        )

        // Then 코드가 안 바뀌었으므로 사유도 그대로 남는다 — 문구가 사라지며 화면이 튀지 않는다
        assertEquals(INVITE_CODE, viewModel.state.value.text)
        assertEquals(InviteCodeError.NETWORK, viewModel.state.value.inviteCodeError)
    }

    @Test
    fun focusChanged_fieldTakesFocusOnItsOwn_marksFocused() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드를 내린 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.HideKeyboard)

        // When 칸 사이 여백을 눌러 입력칸이 스스로 포커스를 가져간다
        viewModel.processIntent(GroupInviteCodeIntent.FocusChanged(isFocused = true))

        // Then 화면이 그 사실을 따라간다 — 타이핑은 되는데 밑줄만 사라지는 어긋남을 막는다
        assertTrue(viewModel.state.value.isFocused)
    }

    private companion object {
        const val INVITE_CODE = "ABCDEF"
        const val LAST_INDEX = InviteCode.LENGTH - 1
        const val GROUP_NAME = "모카의 파르페"
        const val NICKNAME = "모카"
        const val OTHER_NICKNAME = "바닐라"

        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname(NICKNAME),
        )
    }
}
