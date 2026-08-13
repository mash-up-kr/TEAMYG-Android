package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.group.CheckInviteCodeValidUseCase
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

    private fun viewModel() = GroupInviteCodeViewModel(CheckInviteCodeValidUseCase())

    @Test
    fun clipboardCodeDetected_whileKeyboardIsShown_showsPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 올라와 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)

        // When 클립보드에서 초대코드를 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected("E54W1A"))

        // Then 붙여넣기 바가 노출된다
        assertEquals("E54W1A", viewModel.state.value.clipboardInviteCode)
        assertTrue(viewModel.state.value.isPasteBarVisible)
    }

    @Test
    fun clipboardCodeDetected_whileKeyboardIsHidden_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 내려가 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.HideKeyboard)

        // When 클립보드에서 초대코드를 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected("E54W1A"))

        // Then 붙여넣기 바는 노출되지 않는다
        assertFalse(viewModel.state.value.isPasteBarVisible)
    }

    @Test
    fun clipboardCodeDetected_null_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 키보드가 올라와 있는 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)

        // When 클립보드에 초대코드가 없음
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(null))

        // Then 붙여넣기 바는 노출되지 않는다
        assertFalse(viewModel.state.value.isPasteBarVisible)
    }

    @Test
    fun clipboardCodeDetected_sameCodeAlreadyTyped_hidesPasteBar() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 같은 코드를 입력한 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)
        viewModel.processIntent(GroupInviteCodeIntent.InputWord(index = 0, word = "E54W1A"))

        // When 같은 코드가 클립보드에서 발견
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected("E54W1A"))

        // Then 붙여넣기 바는 노출되지 않는다
        assertFalse(viewModel.state.value.isPasteBarVisible)
    }

    @Test
    fun clickPasteInviteCode_fillsCodeAndHidesKeyboard() = runTest(mainDispatcherRule.dispatcher) {
        // Given 붙여넣기 바가 노출된 화면
        val viewModel = viewModel()
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)
        viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected("E54W1A"))

        // When 붙여넣기 바를 클릭
        viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode)

        // Then 코드가 채워지고 키보드가 내려가며 바가 사라진다
        val state = viewModel.state.value
        assertEquals("E54W1A", state.text)
        assertNull(state.focusedIndex)
        assertEquals(InputMode.ADD, state.inputMode)
        assertNull(state.clipboardInviteCode)
        assertFalse(state.isPasteBarVisible)
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
}
