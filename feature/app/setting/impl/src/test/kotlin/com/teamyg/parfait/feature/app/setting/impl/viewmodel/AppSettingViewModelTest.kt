package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = AppSettingViewModel()

    @Test
    fun clickWithdraw_showsWithdrawDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팝업이 떠 있지 않은 초기 화면
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.isWithdrawDialogVisible)

        // When 서비스 탈퇴하기를 누름
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)

        // Then 탈퇴 확인 팝업이 뜬다
        assertTrue(viewModel.state.value.isWithdrawDialogVisible)
    }

    @Test
    fun confirmWithdraw_hidesWithdrawDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 탈퇴 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)

        // When 팝업의 탈퇴하기를 누름
        viewModel.processIntent(AppSettingIntent.ConfirmWithdraw)

        // Then 팝업이 닫힌다
        assertFalse(viewModel.state.value.isWithdrawDialogVisible)
    }

    @Test
    fun dismissWithdrawDialog_hidesWithdrawDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 탈퇴 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)

        // When 그만두기 또는 바깥 탭으로 닫기를 요청
        viewModel.processIntent(AppSettingIntent.DismissWithdrawDialog)

        // Then 팝업이 닫힌다
        assertFalse(viewModel.state.value.isWithdrawDialogVisible)
    }

    @Test
    fun clickWithdraw_doesNotChangeProfileState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 화면의 프로필 값
        val viewModel = viewModel()
        val before = viewModel.state.value

        // When 팝업을 열었다 닫음
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)
        viewModel.processIntent(AppSettingIntent.DismissWithdrawDialog)

        // Then 팝업 외 상태는 그대로다
        val after = viewModel.state.value
        assertEquals(before.nickname, after.nickname)
        assertEquals(before.loginProvider, after.loginProvider)
        assertEquals(before.version, after.version)
        assertFalse(after.isWithdrawDialogVisible)
    }
}
