package com.teamyg.parfait.feature.intro.impl.splash

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.android.APP_VERSION_NAME
import com.teamyg.parfait.domain.model.appversion.AppUpdateCheck
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.usecase.appversion.CheckForceUpdateUseCase
import com.teamyg.parfait.domain.usecase.session.BootstrapSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bootstrapSession: BootstrapSessionUseCase = mockk()
    private val checkForceUpdate: CheckForceUpdateUseCase = mockk {
        coEvery { this@mockk(any()) } returns AppUpdateCheck.NotRequired
    }

    private fun viewModel() = SplashViewModel(bootstrapSession, checkForceUpdate)

    @Test
    fun bootstrapReturnsToLogin_afterAnimationFinishes_emitsNavigateToLogin() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장된 세션이 없어 부트스트랩이 로그인으로 결정한다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToLogin

        val viewModel = viewModel()

        viewModel.effect.test {
            advanceUntilIdle()

            // When 로띠까지 끝난다
            viewModel.processIntent(SplashIntent.AnimationFinished)

            // Then 로그인으로 보내는 이펙트가 나간다
            assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
        }
        coVerify(exactly = 1) { bootstrapSession() }
    }

    @Test
    fun bootstrapReturnsToGroupList_afterAnimationFinishes_emitsNavigateToGroupList() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 살아있는 세션이 있어 부트스트랩이 자동로그인을 성립시킨다
            coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList

            val viewModel = viewModel()

            viewModel.effect.test {
                advanceUntilIdle()

                // When 로띠까지 끝난다
                viewModel.processIntent(SplashIntent.AnimationFinished)

                // Then 그룹 목록으로 보내는 이펙트가 나간다
                assertEquals(SplashSideEffect.NavigateToGroupList, awaitItem())
            }
            coVerify(exactly = 1) { bootstrapSession() }
        }

    @Test
    fun bootstrapFinishesFirst_animationStillPlaying_staysOnSplash() = runTest(mainDispatcherRule.dispatcher) {
        // Given 부트스트랩은 끝났지만 로띠는 아직 돌고 있다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 응답만 도착한다
            advanceUntilIdle()

            // Then 아직 넘어가지 않는다 — 로띠를 자르고 화면을 바꾸지 않는다
            expectNoEvents()
        }
    }

    @Test
    fun animationFinishesFirst_bootstrapStillRunning_staysOnSplashThenNavigates() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 응답이 아직 오지 않은 상태에서 로띠가 먼저 끝난다
            val gate = CompletableDeferred<SessionBootstrap>()
            coEvery { bootstrapSession() } coAnswers { gate.await() }
            val viewModel = viewModel()
            runCurrent()

            viewModel.effect.test {
                viewModel.processIntent(SplashIntent.AnimationFinished)
                runCurrent()

                // Then 목적지를 모르니 넘어가지 않는다
                expectNoEvents()

                // When 응답이 도착한다
                gate.complete(SessionBootstrap.ToLogin)
                advanceUntilIdle()

                // Then 그때 넘어간다 — 나중에 끝난 쪽이 이동을 일으킨다
                assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
            }
        }

    @Test
    fun animationFinishedTwice_emitsNavigationOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 응답이 이미 도착해 로띠만 기다리는 상태
        coEvery { bootstrapSession() } returns SessionBootstrap.ToLogin
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            // When 컴포지션이 다시 서서 같은 신호가 두 번 온다
            viewModel.processIntent(SplashIntent.AnimationFinished)
            viewModel.processIntent(SplashIntent.AnimationFinished)
            advanceUntilIdle()

            // Then 이펙트는 한 번뿐이다 — 두 번이면 백스택을 두 번 갈아 끼운다
            assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
            expectNoEvents()
        }
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

                // When 원래 job 이 끝나고 로띠도 끝나면
                gate.complete(SessionBootstrap.ToLogin)
                advanceUntilIdle()
                viewModel.processIntent(SplashIntent.AnimationFinished)

                // Then 이펙트도 정확히 한 번만 나가고, 조회도 끝까지 한 번뿐이다
                assertEquals(SplashSideEffect.NavigateToLogin, awaitItem())
                coVerify(exactly = 1) { bootstrapSession() }
            }
        }

    @Test
    fun forceUpdate_afterAllSignals_showsModalWithoutNavigation() = runTest(mainDispatcherRule.dispatcher) {
        // Given 강제 업데이트 대상이고 부트스트랩은 그룹 목록으로 결정한다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } returns AppUpdateCheck.Required("market://x")
        val viewModel = viewModel()

        viewModel.effect.test {
            advanceUntilIdle()

            // When 로띠까지 끝난다
            viewModel.processIntent(SplashIntent.AnimationFinished)
            advanceUntilIdle()

            // Then 이동하지 않고 모달을 띄운다
            expectNoEvents()
        }
        assertTrue(viewModel.state.value.isForceUpdateVisible)
    }

    @Test
    fun forceUpdate_beforeAnimationFinishes_hidesModal() = runTest(mainDispatcherRule.dispatcher) {
        // Given 강제 업데이트 대상이고 부트스트랩도 끝났다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } returns AppUpdateCheck.Required("market://x")

        // When 로띠는 아직 돌고 있다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 모달은 아직 뜨지 않는다
        assertFalse(viewModel.state.value.isForceUpdateVisible)
    }

    @Test
    fun versionCheckPending_afterBootstrapAndAnimation_staysOnSplash() = runTest(mainDispatcherRule.dispatcher) {
        // Given 부트스트랩은 끝났지만 버전 체크 응답이 아직 오지 않는다
        val gate = CompletableDeferred<AppUpdateCheck>()
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } coAnswers { gate.await() }
        val viewModel = viewModel()
        runCurrent()

        viewModel.effect.test {
            // When 로띠가 끝난다
            viewModel.processIntent(SplashIntent.AnimationFinished)
            runCurrent()

            // Then 판정을 모르니 넘어가지 않는다
            expectNoEvents()

            // When 통과 판정이 도착한다
            gate.complete(AppUpdateCheck.NotRequired)
            runCurrent()

            // Then 그때 넘어간다
            assertEquals(SplashSideEffect.NavigateToGroupList, awaitItem())
        }
    }

    @Test
    fun versionCheckTimesOut_navigatesAsNotRequired() = runTest(mainDispatcherRule.dispatcher) {
        // Given 버전 체크가 끝나지 않는다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } coAnswers { awaitCancellation() }
        val viewModel = viewModel()
        runCurrent()

        viewModel.effect.test {
            viewModel.processIntent(SplashIntent.AnimationFinished)
            runCurrent()
            expectNoEvents()

            // When 상한 3초 직전까지는 기다린다
            advanceTimeBy(2_999)
            expectNoEvents()

            // And 상한 3초를 넘긴다
            advanceTimeBy(2)

            // Then 통과로 보고 넘어간다 — 스플래시에 갇히지 않는다
            assertEquals(SplashSideEffect.NavigateToGroupList, awaitItem())
        }
    }

    @Test
    fun versionCheck_receivesAppVersionName() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { bootstrapSession() } returns SessionBootstrap.ToLogin

        viewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { checkForceUpdate(APP_VERSION_NAME) }
    }

    @Test
    fun updateClick_emitsOpenStoreWithStoreUrl() = runTest(mainDispatcherRule.dispatcher) {
        // Given 강제 업데이트 모달이 떠 있다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } returns AppUpdateCheck.Required("market://x")
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(SplashIntent.AnimationFinished)

        viewModel.effect.test {
            // When [업데이트]를 누른다
            viewModel.processIntent(SplashIntent.UpdateClick)

            // Then 서버가 준 스토어 주소로 연다
            assertEquals(SplashSideEffect.OpenStore("market://x"), awaitItem())
        }
    }

    @Test
    fun quitClick_emitsQuitApp() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } returns AppUpdateCheck.Required("market://x")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            // When [종료]를 누른다
            viewModel.processIntent(SplashIntent.QuitClick)

            // Then 앱을 끝내는 이펙트가 나간다
            assertEquals(SplashSideEffect.QuitApp, awaitItem())
        }
    }

    @Test
    fun versionCheckThrows_navigatesAsNotRequired() = runTest(mainDispatcherRule.dispatcher) {
        // Given 버전 체크가 예상 못 한 예외로 끝난다
        coEvery { bootstrapSession() } returns SessionBootstrap.ToGroupList
        coEvery { checkForceUpdate(any()) } throws AppError.Unexpected(RuntimeException())
        val viewModel = viewModel()

        viewModel.effect.test {
            advanceUntilIdle()

            // When 로띠까지 끝난다
            viewModel.processIntent(SplashIntent.AnimationFinished)

            // Then 통과로 보고 넘어간다 — 스플래시에 갇히지 않는다
            assertEquals(SplashSideEffect.NavigateToGroupList, awaitItem())
        }
    }
}
