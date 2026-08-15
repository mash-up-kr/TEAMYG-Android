package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
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
import kotlin.test.assertTrue

class AppSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val logout: LogoutUseCase = mockk()

    private fun viewModel() = AppSettingViewModel(logout = logout)

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
    fun confirmWithdraw_whenDialogNotVisible_doesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 탈퇴 확인 팝업이 떠 있지 않은 초기 상태
        val viewModel = viewModel()
        val before = viewModel.state.value

        // When 팝업 없이 확인 Intent가 들어옴(멀티터치로 취소와 동시에 발화한 경우 등)
        viewModel.processIntent(AppSettingIntent.ConfirmWithdraw)

        // Then 아무 것도 바뀌지 않는다
        assertEquals(before, viewModel.state.value)
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

    @Test
    fun clickLogout_succeeds_navigatesToLogin() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그아웃이 성공하는 화면
        coEvery { logout() } returns Result.success(Unit)
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 로그아웃을 누른다
            viewModel.processIntent(AppSettingIntent.ClickLogout)
            advanceUntilIdle()

            // Then 로그인 화면으로 간다
            assertEquals(AppSettingSideEffect.NavigateToLogin, awaitItem())
            coVerify(exactly = 1) { logout() }
        }
    }

    @Test
    fun clickLogout_whileRequestInFlight_marksLoggingOutUntilItFinishes() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그아웃 요청이 끝나지 않게 붙잡아 둔다
        val gate = CompletableDeferred<Unit>()
        coEvery { logout() } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.isLoggingOut)

        // When 로그아웃을 누른다
        viewModel.processIntent(AppSettingIntent.ClickLogout)
        runCurrent()

        // Then 요청 중이라 버튼이 비활성이다
        assertTrue(viewModel.state.value.isLoggingOut)

        // When 요청이 끝난다
        gate.complete(Unit)
        advanceUntilIdle()

        // Then 플래그가 내려간다
        assertFalse(viewModel.state.value.isLoggingOut)
    }

    @Test
    fun clickLogout_throws_clearsLoggingOut() = runTest(mainDispatcherRule.dispatcher) {
        // Given 예상 못 한 예외로 로그아웃이 터진다
        coEvery { logout() } throws IllegalStateException("예상 못 한 실패")
        val viewModel = viewModel()

        // When 로그아웃을 누른다
        viewModel.processIntent(AppSettingIntent.ClickLogout)
        advanceUntilIdle()

        // Then 버튼이 영구 비활성으로 남지 않는다
        assertFalse(viewModel.state.value.isLoggingOut)
    }

    @Test
    fun clickLogout_whileLoggingOut_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그아웃 요청이 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { logout() } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = viewModel()
        viewModel.processIntent(AppSettingIntent.ClickLogout)
        runCurrent()

        // When 한 번 더 누른다
        viewModel.processIntent(AppSettingIntent.ClickLogout)
        runCurrent()

        // Then 중복 요청이 나가지 않는다
        coVerify(exactly = 1) { logout() }

        gate.complete(Unit)
        advanceUntilIdle()
    }
}
