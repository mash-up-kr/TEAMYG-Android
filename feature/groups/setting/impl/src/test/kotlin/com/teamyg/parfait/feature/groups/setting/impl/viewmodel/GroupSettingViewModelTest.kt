package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = GroupSettingViewModel(CheckNameValidUseCase())

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
        // Given 편집 중 유효한 새 닉네임을 입력한 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("확정될닉네임"))

        // When 확정
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)

        // Then 내 닉네임이 바뀌고 편집이 끝나며 그룹원 목록의 내 항목도 따라 바뀐다
        val state = viewModel.state.value
        assertEquals("확정될닉네임", state.myNickname.value)
        assertFalse(state.isEditing)
        assertEquals("확정될닉네임", state.members.first { it.isMe }.nickname)
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

        // Then 아무 것도 확정되지 않고 편집 상태가 유지된다
        assertEquals(original, viewModel.state.value.myNickname.value)
        assertTrue(viewModel.state.value.isEditing)
    }

    @Test
    fun confirmNickname_thenLosesFocus_keepsConfirmedNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중 유효한 새 닉네임을 입력한 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("확정될닉네임"))

        // When 확정 직후 포커스 상실이 이어짐
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = false))

        // Then 확정된 닉네임이 되돌아가지 않는다
        val state = viewModel.state.value
        assertEquals("확정될닉네임", state.myNickname.value)
        assertEquals("확정될닉네임", state.nicknameInput)
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
}
