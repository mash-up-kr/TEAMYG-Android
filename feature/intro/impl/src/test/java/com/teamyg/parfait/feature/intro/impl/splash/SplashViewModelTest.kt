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
    fun init_bootstrapSlow_doesNotStartSecondJobAndEmitsOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 부트스트랩이 곧바로 끝나지 않는다 — launch(key) 가드가 지키는 job 하나가 떠 있다
        val gate = CompletableDeferred<SessionBootstrap>()
        coEvery { bootstrapSession() } coAnswers { gate.await() }

        val viewModel = viewModel()

        viewModel.effect.test {
            // When 아직 응답 전인 상태로 재구성 등으로 여러 번 idle 이 돌아도
            runCurrent()
            runCurrent()

            // Then 이펙트는 아직 없고, 조회는 한 번만 나간 채로 대기 중이다
            expectNoEvents()
            coVerify(exactly = 1) { bootstrapSession() }

            // When 부트스트랩이 마침내 끝나면
            gate.complete(SessionBootstrap.ToLogin)
            advanceUntilIdle()

            // Then 이펙트가 정확히 한 번만 나가고, 조회도 끝까지 한 번뿐이다
            assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
            coVerify(exactly = 1) { bootstrapSession() }
        }
    }
}
