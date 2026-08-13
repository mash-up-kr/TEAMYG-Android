package com.teamyg.parfait.feature.intro.impl.splash

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.splash.SplashInitialUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = SplashViewModel(SplashInitialUseCase())

    @Test
    fun init_beforeInitialFinishes_keepsLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 스플래시 진입
        val viewModel = viewModel()

        // When 초기화가 끝나기 전
        runCurrent()

        // Then 로딩 상태를 유지한다
        assertEquals(LoadingStatus.Loading, viewModel.state.value.loadingStatus)
    }

    @Test
    fun init_afterInitialFinishes_becomesSuccess() = runTest(mainDispatcherRule.dispatcher) {
        // Given 스플래시 진입
        val viewModel = viewModel()

        // When 초기화가 끝날 때까지 진행
        advanceUntilIdle()

        // Then 성공 상태가 된다
        assertEquals(LoadingStatus.Success, viewModel.state.value.loadingStatus)
    }
}
