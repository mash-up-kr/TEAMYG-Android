package com.teamyg.parfait.feature.intro.impl.splash

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.usecase.session.BootstrapSessionUseCase
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

class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bootstrapSession: BootstrapSessionUseCase = mockk()

    private fun viewModel() = SplashViewModel(bootstrapSession)

    @Test
    fun init_bootstrapReturnsToLogin_emitsNavigateToLogin() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장된 세션이 없어 부트스트랩이 로그인으로 결정한다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToLogin

        val viewModel = viewModel()

        viewModel.effect.test {
            // When 부트스트랩이 끝나면
            advanceUntilIdle()

            // Then 로그인으로 보내는 이펙트가 나간다
            assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
        }
        coVerify(exactly = 1) { bootstrapSession() }
    }

    @Test
    fun init_bootstrapReturnsToGroupList_emitsNavigateToGroupList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 살아있는 세션이 있어 부트스트랩이 자동로그인을 성립시킨다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList

        val viewModel = viewModel()

        viewModel.effect.test {
            // When 부트스트랩이 끝나면
            advanceUntilIdle()

            // Then 그룹 목록으로 보내는 이펙트가 나간다
            assertEquals(SplashSideEffect.NavigateToGroupList, awaitItem())
        }
        coVerify(exactly = 1) { bootstrapSession() }
    }

    @Test
    fun processIntent_initAgainWhileFirstStillRunning_doesNotInvokeUseCaseTwice() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 부트스트랩이 곧바로 끝나지 않는다 — init 이 이미 launch(key) 로 job 하나를 띄운 상태
            val gate = CompletableDeferred<SessionBootstrap>()
            coEvery { bootstrapSession() } coAnswers { gate.await() }
            val viewModel = viewModel()
            runCurrent()
            coVerify(exactly = 1) { bootstrapSession() }

            viewModel.effect.test {
                // When 같은 의도를 한 번 더 보낸다
                // (포그라운드 복귀처럼 향후 두 번째 진입점이 생기는 상황을 흉내낸다)
                viewModel.processIntent(SplashIntent.Init)
                runCurrent()

                // Then guard 에 막혀 조회가 다시 나가지 않는다
                coVerify(exactly = 1) { bootstrapSession() }

                // When 원래 job 이 끝나면
                gate.complete(SessionBootstrap.ToLogin)
                advanceUntilIdle()

                // Then 이펙트도 정확히 한 번만 나가고, 조회도 끝까지 한 번뿐이다
                assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
                coVerify(exactly = 1) { bootstrapSession() }
            }
        }
}
