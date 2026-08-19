package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.id.ReportId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
import com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.group.LeaveGroupUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.group.ReportGroupUseCase
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getGroupDetail: GetGroupDetailUseCase = mockk()
    private val refreshGroupDetail: RefreshGroupDetailUseCase = mockk()
    private val getMyAccountFlow: GetMyAccountFlowUseCase = mockk()
    private val changeGroupNickname: ChangeGroupNicknameUseCase = mockk()
    private val leaveGroup: LeaveGroupUseCase = mockk()
    private val reportGroup: ReportGroupUseCase = mockk()

    @Before
    fun setUp() {
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(DETAIL)
        coEvery { refreshGroupDetail(GroupId(GROUP_ID)) } returns Result.success(Unit)
        every { getMyAccountFlow() } returns flowOf(MY_ACCOUNT)
        coEvery { changeGroupNickname(any(), any()) } returns Result.success(
            GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname(NEW_NICKNAME)),
        )
    }

    /** [DETAIL] 에서 정원·멤버만 바꾼 상세. 나머지 필드는 기본 픽스처 그대로다 */
    private fun detailOf(
        memberLimit: Int = 12,
        members: List<ParfaitGroupMemberVO> = DETAIL.members,
    ): ParfaitGroupDetailVO = DETAIL.copy(memberLimit = memberLimit, members = members)

    /** 조회가 끝나기 전 구간을 보는 테스트를 위해 시간을 흘리지 않고 만든다 */
    private fun newViewModel(): GroupSettingViewModel = GroupSettingViewModel(
        groupIdValue = GROUP_ID,
        checkNameValid = CheckNameValidUseCase(),
        getGroupDetail = getGroupDetail,
        refreshGroupDetail = refreshGroupDetail,
        getMyAccountFlow = getMyAccountFlow,
        changeGroupNickname = changeGroupNickname,
        leaveGroup = leaveGroup,
        reportGroup = reportGroup,
    )

    /** 만들자마자 상세 조회가 끝난 상태로 넘긴다 — 화면이 실제로 서 있는 지점이 여기다 */
    private fun TestScope.viewModel(): GroupSettingViewModel = newViewModel().also { advanceUntilIdle() }

    @Test
    fun inputNickname_validName_updatesInputAndClearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 유효한 닉네임을 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname("새닉네임"))

        // Then 입력값이 반영되고 에러가 없다
        assertEquals("새닉네임", viewModel.state.value.nicknameInput)
        assertNull(viewModel.state.value.nicknameError)
    }

    @Test
    fun inputNickname_invalidCharacter_setsInvalidCharacterError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 허용되지 않는 문자를 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname("닉네임!"))

        // Then 문자 규칙 위반 에러가 붙는다
        assertEquals(
            NameValidResult.Error.InvalidCharacter,
            viewModel.state.value.nicknameError,
        )
    }

    @Test
    fun inputNickname_leadingSpace_setsSpaceAtEdgeError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 앞에 공백이 있는 닉네임을 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname(" 닉네임"))

        // Then 가장자리 공백 에러가 붙는다
        assertEquals(
            NameValidResult.Error.SpaceAtEdge,
            viewModel.state.value.nicknameError,
        )
    }

    @Test
    fun inputNickname_duplicatedSpace_setsDuplicatedSpaceError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 단어 사이에 공백이 연속된 닉네임을 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname("가  나"))

        // Then 연속 공백 에러가 붙는다
        assertEquals(
            NameValidResult.Error.DuplicatedSpace,
            viewModel.state.value.nicknameError,
        )
    }

    @Test
    fun inputNickname_emptyName_setsEmptyStringError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 전부 지움
        viewModel.processIntent(GroupSettingIntent.InputNickname(""))

        // Then 빈 값 에러가 붙고 확인이 비활성이다
        assertEquals(
            NameValidResult.Error.EmptyString,
            viewModel.state.value.nicknameError,
        )
        assertFalse(viewModel.state.value.isConfirmEnabled)
    }

    @Test
    fun isConfirmEnabled_nicknameUnchanged_isFalse() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname.value

        // When 고쳤다가 원래 값으로 되돌림
        viewModel.processIntent(GroupSettingIntent.InputNickname("잠깐바꿈"))
        viewModel.processIntent(GroupSettingIntent.InputNickname(original))

        // Then 바뀐 것이 없으므로 확인은 비활성
        assertFalse(viewModel.state.value.isConfirmEnabled)
    }

    @Test
    fun changeNicknameFocus_focused_entersEditing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 입력 필드가 포커스를 얻음
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))

        // Then 편집 모드로 들어간다
        assertTrue(viewModel.state.value.isEditing)
    }

    @Test
    fun changeNicknameFocus_unfocused_cancelsEditingAndRestoresInput() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중 무효한 입력으로 고친 상태
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname.value
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("고치던 값!"))
        assertEquals(
            NameValidResult.Error.InvalidCharacter,
            viewModel.state.value.nicknameError,
        )

        // When 포커스를 잃음
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = false))

        // Then 편집이 취소되고 입력값이 원래대로 돌아가며 에러도 초기화된다
        assertFalse(viewModel.state.value.isEditing)
        assertEquals(original, viewModel.state.value.nicknameInput)
        assertNull(viewModel.state.value.nicknameError)
    }

    @Test
    fun confirmNickname_validChange_commitsAndSyncsMemberList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중 유효한 새 닉네임을 입력한 상태. 캐시는 저장소가 서버에서 다시 받아 온
        // 값으로 새 닉네임을 낸다
        val detail = MutableStateFlow<ParfaitGroupDetailVO?>(DETAIL)
        every { getGroupDetail(GroupId(GROUP_ID)) } returns detail
        coEvery { changeGroupNickname(any(), any()) } coAnswers {
            val newNickname = GroupNickname(NEW_NICKNAME)
            detail.value = DETAIL.copy(
                groupNickname = newNickname,
                members = DETAIL.members.map { member ->
                    if (member.memberId == MY_ACCOUNT.memberId) member.copy(groupNickname = newNickname) else member
                },
            )
            Result.success(GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = newNickname))
        }
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname(NEW_NICKNAME))

        // When 확정
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        advanceUntilIdle()

        // Then 내 닉네임이 바뀌고 편집이 끝나며 그룹원 목록의 내 항목도 따라 바뀐다
        val state = viewModel.state.value
        assertEquals(NEW_NICKNAME, state.myNickname.value)
        assertFalse(state.isEditing)
        assertEquals(NEW_NICKNAME, state.members.first { it.isMe }.nickname)
    }

    @Test
    fun confirmNickname_invalidNickname_keepsPreviousNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 유효성을 통과하지 못한 입력
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname.value
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("잘못된 닉네임!"))

        // When 확정을 시도(키보드 엔터 포함 같은 경로)
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        advanceUntilIdle()

        // Then 아무 것도 확정되지 않고 편집 상태가 유지된다
        assertEquals(original, viewModel.state.value.myNickname.value)
        assertTrue(viewModel.state.value.isEditing)
    }

    @Test
    fun confirmNickname_thenLosesFocus_keepsConfirmedNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중 유효한 새 닉네임을 입력한 상태. 캐시는 저장소가 서버에서 다시 받아 온
        // 값으로 새 닉네임을 낸다
        val detail = MutableStateFlow<ParfaitGroupDetailVO?>(DETAIL)
        every { getGroupDetail(GroupId(GROUP_ID)) } returns detail
        coEvery { changeGroupNickname(any(), any()) } coAnswers {
            val newNickname = GroupNickname(NEW_NICKNAME)
            detail.value = DETAIL.copy(groupNickname = newNickname)
            Result.success(GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = newNickname))
        }
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname(NEW_NICKNAME))

        // When 확정 직후 포커스 상실이 이어짐
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        advanceUntilIdle()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = false))

        // Then 확정된 닉네임이 되돌아가지 않는다
        val state = viewModel.state.value
        assertEquals(NEW_NICKNAME, state.myNickname.value)
        assertEquals(NEW_NICKNAME, state.nicknameInput)
        assertFalse(state.isEditing)
    }

    @Test
    fun clickBack_whileEditing_cancelsEditingWithoutNavigating() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중인 상태
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname.value
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("고치던값"))

        // When 뒤로가기
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ClickBack)

            // Then 화면을 닫지 않고 편집만 취소한다
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isEditing)
        assertEquals(original, viewModel.state.value.nicknameInput)
    }

    @Test
    fun clickBack_notEditing_emitsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중이 아닌 상태
        val viewModel = viewModel()

        // When 뒤로가기
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ClickBack)

            // Then 화면을 닫는 SideEffect가 나간다
            assertEquals(GroupSettingSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun clickCopyInviteCode_marksCopiedAndEmitsCopyEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()
        val inviteCode = viewModel.state.value.inviteCode.value

        // When 복사 버튼
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)

            // Then 클립보드 SideEffect가 코드와 함께 나가고 카드 문구가 복사됨으로 바뀐다
            assertEquals(GroupSettingSideEffect.CopyInviteCode(inviteCode), awaitItem())
        }
        assertTrue(viewModel.state.value.isCodeCopied)
    }

    @Test
    fun clickCopyInviteCode_afterTwoSeconds_resetsIsCodeCopied() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 복사 버튼을 누르고 2초 직전까지 시간이 흐름
        viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)
        advanceTimeBy(1_999)

        // Then 아직 복사됨 상태가 유지된다
        assertTrue(viewModel.state.value.isCodeCopied)

        // When 나머지 시간이 흘러 2초를 채움
        advanceTimeBy(1)
        runCurrent()

        // Then 복사됨 상태가 풀리고 원래 문구로 돌아간다
        assertFalse(viewModel.state.value.isCodeCopied)
    }

    @Test
    fun clickCopyInviteCode_rapidReclickBeforeTimeout_resetsTimerToLatestClick() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 복사 버튼을 누르고 1초가 지난 상태
            val viewModel = viewModel()
            viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)
            advanceTimeBy(1_000)

            // When 1초 시점에 다시 눌러 타이머를 리셋한 뒤 1.5초가 더 지남
            viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)
            advanceTimeBy(1_500)

            // Then 첫 타이머가 취소되었으므로(첫 타이머 기준으로는 이미 지났을 시점) 여전히 복사됨 상태다
            assertTrue(viewModel.state.value.isCodeCopied)

            // When 두 번째 클릭 기준 2초를 마저 채움
            advanceTimeBy(500)
            runCurrent()

            // Then 복사됨 상태가 풀린다
            assertFalse(viewModel.state.value.isCodeCopied)
        }

    @Test
    fun clickLeaveGroup_showsLeaveDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팝업이 떠 있지 않은 초기 화면
        val viewModel = viewModel()
        assertNull(viewModel.state.value.visibleDialog)

        // When 그룹 나가기를 누름
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)

        // Then 나가기 확인 팝업이 뜬다
        assertEquals(GroupSettingDialog.Leave, viewModel.state.value.visibleDialog)
    }

    @Test
    fun clickReportGroup_showsReportDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팝업이 떠 있지 않은 초기 화면
        val viewModel = viewModel()

        // When 그룹 신고하기를 누름
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // Then 신고 확인 팝업이 뜬다
        assertEquals(GroupSettingDialog.Report, viewModel.state.value.visibleDialog)
    }

    @Test
    fun confirmLeaveGroup_succeeds_hidesDialogAndGoesToGroupList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 탈퇴를 받아 주는 상태에서 나가기 확인 팝업이 떠 있다
        coEvery { leaveGroup(GroupId(GROUP_ID)) } returns Result.success(GroupId(GROUP_ID))
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)

        // When 팝업의 나가기를 누름
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)

            // Then 되돌아갈 수 없는 그룹이므로 목록으로 보낸다
            assertEquals(GroupSettingSideEffect.NavigateToGroupList, awaitItem())
        }

        // Then 팝업도 닫힌다
        assertNull(viewModel.state.value.visibleDialog)
        coVerify(exactly = 1) { leaveGroup(GroupId(GROUP_ID)) }
    }

    @Test
    fun confirmLeaveGroup_fails_showsErrorAndStaysOnScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 탈퇴 요청이 실패하는 상태에서 나가기 확인 팝업이 떠 있다
        coEvery { leaveGroup(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)

        // When 팝업의 나가기를 누름
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)

            // Then 나가지 못한 이유를 토스트로 알릴 뿐, 목록으로 보내지 않는다
            assertEquals(GroupSettingSideEffect.ShowError(GroupSettingError.NETWORK), awaitItem())
            expectNoEvents()
        }

        // Then 덮개도 걷혀 화면을 다시 쓸 수 있다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun confirmLeaveGroup_tappedTwiceWhileInFlight_callsApiOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 요청이 아직 끝나지 않은 상태
        val pending = CompletableDeferred<Result<GroupId>>()
        coEvery { leaveGroup(any()) } coAnswers { pending.await() }
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)

        // When 응답을 기다리는 사이 한 번 더 누름
        viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)
        runCurrent()

        // Then 왕복 동안 팝업은 이미 닫혀 있고 화면은 덮여 있다
        assertNull(viewModel.state.value.visibleDialog)
        assertTrue(viewModel.state.value.isLoading)

        viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)
        runCurrent()
        pending.complete(Result.success(GroupId(GROUP_ID)))
        advanceUntilIdle()

        // Then 탈퇴는 한 번만 나간다
        coVerify(exactly = 1) { leaveGroup(any()) }
    }

    @Test
    fun confirmLeaveGroup_whenDialogNotVisible_doesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 어떤 팝업도 떠 있지 않은 초기 상태
        val viewModel = viewModel()
        val before = viewModel.state.value

        // When 팝업 없이 나가기 확인 Intent가 들어옴(멀티터치로 취소와 동시에 발화한 경우 등)
        viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)

        // Then 아무 것도 바뀌지 않는다
        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun confirmLeaveGroup_whileReportDialogVisible_keepsReportDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 신고 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 멀티터치 등으로 나가기 확인 Intent가 잘못 들어옴(교차 확인 방어)
        viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)

        // Then 신고 확인 팝업이 그대로 남아 있다
        assertEquals(GroupSettingDialog.Report, viewModel.state.value.visibleDialog)
    }

    @Test
    fun confirmReportGroup_succeeds_hidesDialogAndGoesToGroupList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 신고를 받아 주는 상태에서 신고 확인 팝업이 떠 있다
        coEvery { reportGroup(groupId = GroupId(GROUP_ID), reason = any()) } returns Result.success(REPORTED_GROUP)
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 팝업의 신고하기를 누름
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ConfirmReportGroup)

            // Then 신고와 함께 그룹에서도 빠지므로 나가기와 같은 자리로 보낸다
            assertEquals(GroupSettingSideEffect.NavigateToGroupList, awaitItem())
        }

        // Then 팝업도 닫힌다
        assertNull(viewModel.state.value.visibleDialog)
    }

    @Test
    fun confirmReportGroup_sendsNonBlankReason() = runTest(mainDispatcherRule.dispatcher) {
        // Given 신고 확인 팝업이 떠 있는 상태
        val reason = slot<String>()
        coEvery { reportGroup(groupId = any(), reason = capture(reason)) } returns Result.success(REPORTED_GROUP)
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 팝업의 신고하기를 누름
        viewModel.processIntent(GroupSettingIntent.ConfirmReportGroup)
        advanceUntilIdle()

        // Then 사유를 고르는 UI 가 없어도 서버가 요구하는 사유는 채워 보낸다
        assertTrue(reason.captured.isNotBlank())
    }

    @Test
    fun confirmReportGroup_fails_showsErrorAndStaysOnScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 신고 요청이 실패하는 상태에서 신고 확인 팝업이 떠 있다
        coEvery { reportGroup(groupId = any(), reason = any()) } returns
            Result.failure(AppError.Server(code = "INVALID_GROUP_REPORT_REASON", statusCode = 400, serverMessage = "…"))
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 팝업의 신고하기를 누름
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ConfirmReportGroup)

            // Then 닉네임 규칙과 무관한 코드라 일반 문구로 떨어지고, 목록으로 보내지 않는다
            assertEquals(GroupSettingSideEffect.ShowError(GroupSettingError.UNKNOWN), awaitItem())
            expectNoEvents()
        }

        // Then 덮개도 걷혀 화면을 다시 쓸 수 있다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun dismissDialog_hidesDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 신고 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 그만두기 또는 바깥 탭으로 닫기를 요청
        viewModel.processIntent(GroupSettingIntent.DismissDialog)

        // Then 팝업이 닫힌다
        assertNull(viewModel.state.value.visibleDialog)
    }

    @Test
    fun dialogIntents_whileEditing_keepEditingState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 편집 중인 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("고치던닉네임"))

        // When 나가기 팝업을 열었다 닫음
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)
        viewModel.processIntent(GroupSettingIntent.DismissDialog)

        // Then 편집 상태와 입력값이 그대로 남는다
        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("고치던닉네임", state.nicknameInput)
        assertNull(state.visibleDialog)
    }

    @Test
    fun init_loadsGroupDetailIntoState() = runTest(mainDispatcherRule.dispatcher) {
        // Given·When 화면이 열려 상세 조회가 끝남
        val viewModel = viewModel()

        // Then 그룹명·내 닉네임·초대코드·그룹원이 서버 값으로 채워지고, 내 항목이 표시된다
        val state = viewModel.state.value
        assertEquals("그룹이름", state.groupName.value)
        assertEquals(MY_NICKNAME, state.myNickname.value)
        assertEquals(MY_NICKNAME, state.nicknameInput)
        assertEquals("WDIDCJ", state.inviteCode.value)
        assertEquals(3, state.members.size)
        assertEquals(MY_NICKNAME, state.members.single { it.isMe }.nickname)
    }

    @Test
    fun detailArrives_memberChipsFollowTheServerAssignment() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 멤버 셋에게 목록 순서와 무관한 칩을 배정했다(기본 픽스처가 그렇다)

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 인덱스가 아니라 배정된 값을 쓴다 — 멤버가 빠져도 남은 사람 색이 안 밀린다
        assertEquals(
            listOf(
                YGColorChipType.NametagChip8,
                YGColorChipType.NametagChip3,
                YGColorChipType.NametagChip11,
            ),
            viewModel.state.value.members
                .map { it.colorChipType },
        )
    }

    @Test
    fun detailArrives_missingChipFallsBackToDefault() = runTest(mainDispatcherRule.dispatcher) {
        // Given 칩을 알 수 없는 멤버가 섞여 있다 — 이 목록에 DEFAULT 가 제 뜻으로 오지는 않지만
        // 매퍼가 모르는 값을 그리로 접으므로 볼 수 있다
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(
            detailOf(
                members = listOf(
                    ParfaitGroupMemberVO(
                        MemberId(1L),
                        GroupNickname(MY_NICKNAME),
                        NametagChipType.DEFAULT,
                    ),
                ),
            ),
        )

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 아무 색이나 돌리지 않고 중립 칩으로 그린다
        assertEquals(
            YGColorChipType.Default,
            viewModel.state.value.members
                .single()
                .colorChipType,
        )
    }

    @Test
    fun detailArrives_remainingCountIsLimitMinusMembers() = runTest(mainDispatcherRule.dispatcher) {
        // Given 정원 12 인 그룹에 멤버가 셋 있다(기본 픽스처)

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 남은 자리를 실제로 센다 — 그전엔 고정 1 이었다
        assertEquals(9, viewModel.state.value.remainingCount)
    }

    @Test
    fun detailArrives_moreMembersThanLimit_clampsRemainingToZero() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시와 서버가 어긋나 멤버가 정원보다 많다
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(detailOf(memberLimit = 1))

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 음수를 내보내지 않는다 — "-1명 남음"은 읽는 사람에게 뜻이 없다
        assertEquals(0, viewModel.state.value.remainingCount)
    }

    @Test
    fun init_myAccountUnknown_marksNobodyAsMe() = runTest(mainDispatcherRule.dispatcher) {
        // Given 계정 정보를 아직 못 받은 상태
        every { getMyAccountFlow() } returns flowOf(null)

        // When 화면이 열림
        val viewModel = viewModel()

        // Then 이름이 겹칠 수 있으므로 남을 나로 표시하지 않는다
        val members = viewModel.state.value.members
        assertTrue(members.none { it.isMe })
    }

    @Test
    fun init_myAccountReadThrows_doesNotCrashAndShowsDetail() = runTest(mainDispatcherRule.dispatcher) {
        // Given 계정 정보 조회가 저장소 IO 실패로 예외를 던진다(회귀 방지 - 예전엔 이 구독이
        // BaseViewModel.launch 가드 안에 있어 잡혔지만, viewModelScope.launch 직접 호출로
        // 바뀌며 잡히지 않게 됐었다)
        every { getMyAccountFlow() } returns flow { throw IllegalStateException("디스크 IO 실패") }

        // When 화면이 열린다 — 예외가 앱을 죽이지 않아야 한다
        val viewModel = viewModel()

        // Then 계정 id 를 못 읽었어도 상세는 그대로 채워진다. 다만 아무도 나로 표시되지 않는다
        val state = viewModel.state.value
        assertEquals(DETAIL.groupName, state.groupName)
        assertEquals(DETAIL.groupNickname, state.myNickname)
        assertEquals(3, state.members.size)
        assertTrue(state.members.none { it.isMe })
    }

    @Test
    fun confirmNickname_serverRejectsName_keepsPreviousNicknameAndShowsReason() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 서버가 닉네임을 거절한다
            coEvery { changeGroupNickname(any(), any()) } returns Result.failure(
                AppError.Server(
                    code = ServerErrorCode.ParfaitGroup.INVALID_GROUP_NICKNAME,
                    statusCode = 400,
                    serverMessage = "사용할 수 없는 닉네임입니다",
                ),
            )
            val viewModel = viewModel()
            viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
            viewModel.processIntent(GroupSettingIntent.InputNickname(NEW_NICKNAME))

            // When 확정
            viewModel.effect.test {
                viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
                advanceUntilIdle()

                // Then 사유가 토스트로 나간다
                assertEquals(
                    GroupSettingSideEffect.ShowError(GroupSettingError.INVALID_NICKNAME),
                    awaitItem(),
                )
            }

            // Then 서버가 받아 준 이름만 남으므로 이전 닉네임 그대로다
            val state = viewModel.state.value
            assertEquals(MY_NICKNAME, state.myNickname.value)
            assertFalse(state.isSubmittingNickname)
        }

    @Test
    fun confirmNickname_networkFails_showsNetworkReason() = runTest(mainDispatcherRule.dispatcher) {
        // Given 네트워크가 끊긴 상태
        coEvery { changeGroupNickname(any(), any()) } returns Result.failure(AppError.Network(Exception()))
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname(NEW_NICKNAME))

        // When 확정
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
            advanceUntilIdle()

            // Then 네트워크 사유가 토스트로 나간다
            assertEquals(GroupSettingSideEffect.ShowError(GroupSettingError.NETWORK), awaitItem())
        }
    }

    @Test
    fun init_detailFails_stopsLoadingAndTellsWhy() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비었고 갱신도 실패한다
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(null)
        coEvery { refreshGroupDetail(GroupId(GROUP_ID)) } returns Result.failure(AppError.Network(Exception()))

        // When 화면이 열림
        val viewModel = newViewModel()

        viewModel.effect.test {
            advanceUntilIdle()

            // Then 사유가 토스트로 나가고, 로딩이 걸린 채 남지 않는다
            assertEquals(GroupSettingSideEffect.ShowError(GroupSettingError.NETWORK), awaitItem())
        }
        val state = viewModel.state.value
        assertEquals("", state.groupName.value)
        assertTrue(state.members.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun init_beforeDetailArrives_isLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 상세 조회가 아직 끝나지 않은 화면
        val viewModel = newViewModel()

        // Then 보여 줄 값이 없으므로 화면을 덮는다
        assertTrue(viewModel.state.value.isLoading)

        // When 조회가 끝나면
        advanceUntilIdle()

        // Then 덮개가 걷힌다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun confirmNickname_whileSubmitting_isLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버 응답이 아직 오지 않은 닉네임 변경
        val pending = CompletableDeferred<Result<GroupNicknameVO>>()
        coEvery { changeGroupNickname(any(), any()) } coAnswers { pending.await() }
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname(NEW_NICKNAME))

        // When 확정
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        advanceUntilIdle()

        // Then 왕복이 끝날 때까지 입력 필드를 더 고치지 못하게 덮는다
        assertTrue(viewModel.state.value.isLoading)

        // When 응답이 도착하면
        pending.complete(
            Result.success(GroupNicknameVO(GroupId(GROUP_ID), GroupNickname(NEW_NICKNAME))),
        )
        advanceUntilIdle()

        // Then 덮개가 걷힌다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun init_cacheHasDetail_showsWithoutLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 상세가 이미 있고, 서버 갱신 왕복은 아직 응답하지 않는다
        val pending = CompletableDeferred<Result<Unit>>()
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(DETAIL)
        coEvery { refreshGroupDetail(GroupId(GROUP_ID)) } coAnswers { pending.await() }
        every { getMyAccountFlow() } returns flowOf(MY_ACCOUNT)

        // When 화면이 열리지만, 갱신 왕복은 아직 끝나지 않은 시점이다
        val viewModel = newViewModel()
        runCurrent()

        // Then 갱신이 끝나길 기다리지 않고, 캐시의 첫 값만으로 이미 화면을 덮지 않는다
        val state = viewModel.state.value
        assertFalse(state.isLoadingDetail)
        assertEquals(DETAIL.groupName, state.groupName)
        assertEquals(DETAIL.groupNickname, state.myNickname)

        // 갱신 왕복을 마저 끝내 다음 테스트로 코루틴이 새지 않게 한다
        pending.complete(Result.success(Unit))
        advanceUntilIdle()
    }

    @Test
    fun init_refreshFails_showsErrorEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비었고 갱신이 실패한다
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(null)
        coEvery { refreshGroupDetail(GroupId(GROUP_ID)) } returns Result.failure(AppError.Network(cause = null))
        every { getMyAccountFlow() } returns flowOf(MY_ACCOUNT)

        val viewModel = newViewModel()

        // When/Then 실패가 토스트로 나간다
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertIs<GroupSettingSideEffect.ShowError>(effect)
        }
    }

    @Test
    fun confirmNickname_succeeds_takesNewValueFromCache() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 변경 후 새 닉네임을 낸다
        val detail = MutableStateFlow<ParfaitGroupDetailVO?>(DETAIL)
        every { getGroupDetail(GroupId(GROUP_ID)) } returns detail
        coEvery { refreshGroupDetail(GroupId(GROUP_ID)) } returns Result.success(Unit)
        every { getMyAccountFlow() } returns flowOf(MY_ACCOUNT)
        coEvery { changeGroupNickname(GroupId(GROUP_ID), GroupNickname("라떼")) } coAnswers {
            detail.value = DETAIL.copy(groupNickname = GroupNickname("라떼"))
            Result.success(GroupNicknameVO(groupId = GroupId(GROUP_ID), groupNickname = GroupNickname("라떼")))
        }

        val viewModel = viewModel()
        advanceUntilIdle()

        // When 닉네임을 바꾼다
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("라떼"))
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        advanceUntilIdle()

        // Then 화면 값은 캐시 방출로 바뀐다 — ViewModel 이 손으로 고치지 않는다
        val state = viewModel.state.value
        assertEquals(GroupNickname("라떼"), state.myNickname)
        assertEquals("라떼", state.nicknameInput)
        assertFalse(state.isEditing)
    }

    private companion object {
        const val GROUP_ID = 7L
        const val MY_NICKNAME = "잠탈전용닉네임2"
        const val NEW_NICKNAME = "확정될닉네임"

        val MY_ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("전역닉네임"),
        )

        val DETAIL = ParfaitGroupDetailVO(
            groupId = GroupId(GROUP_ID),
            groupName = GroupName("그룹이름"),
            groupNickname = GroupNickname(MY_NICKNAME),
            inviteCode = InviteCode("WDIDCJ"),
            memberLimit = 12,
            members = listOf(
                ParfaitGroupMemberVO(MemberId(1L), GroupNickname(MY_NICKNAME), NametagChipType.TYPE8),
                ParfaitGroupMemberVO(MemberId(2L), GroupNickname("체리마루"), NametagChipType.TYPE3),
                ParfaitGroupMemberVO(MemberId(3L), GroupNickname("푸딩왕자"), NametagChipType.TYPE11),
            ),
        )

        val REPORTED_GROUP = ReportedGroupVO(
            groupId = GroupId(GROUP_ID),
            reportId = ReportId(1L),
        )
    }
}
