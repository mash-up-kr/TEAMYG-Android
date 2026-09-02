package com.teamyg.parfait.feature.login.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.usecase.auth.LoginWithKakaoUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginWithKakaoUseCase: LoginWithKakaoUseCase = mockk()

    private fun viewModel() = LoginViewModel(loginWithKakaoUseCase)

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-1"),
        refreshToken = RefreshToken("refresh-1"),
        expiresIn = 3600.seconds,
    )

    @Test
    fun loginWithKakao_firstClick_requestsSdkLoginAndTurnsOnLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 카카오 버튼을 누른다
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()

            // Then SDK 로그인 요청이 나가고 로딩이 켜진다
            assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())
            assertTrue(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginWithKakao_clickedWhileLoading_doesNotRequestSdkLoginAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 로그인이 진행 중
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()
            assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())

            // When 한 번 더 누른다(연타)
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()

            // Then 두 번째 요청은 나가지 않는다 — 카카오 창이 두 번 뜨면 안 된다
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginSuccess_existingMember_navigatesToGroupList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 기존 회원으로 응답
        coEvery { loginWithKakaoUseCase(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 그룹 목록으로 간다
            assertEquals(LoginSideEffect.NavigateToGroupList, awaitItem())
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginSuccess_newUser_navigatesToTermAgreeWithRegistrationToken() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 신규 회원으로 응답
        coEvery { loginWithKakaoUseCase(any(), any()) } returns
            Result.success(KakaoLoginVO.NewUser(RegistrationToken("reg-1")))
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 가입 토큰을 들고 약관 화면으로 간다
            assertEquals(LoginSideEffect.NavigateToTermAgree("reg-1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailure_serverError_showsErrorAndDoesNotNavigate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 401 로 실패
        coEvery { loginWithKakaoUseCase(any(), any()) } returns Result.failure(
            AppError.Server(code = "INVALID_ID_TOKEN", statusCode = 401, serverMessage = "…"),
        )
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 내비게이션 대신 실패 안내가 나가고 로딩이 풀린다
            assertEquals(LoginSideEffect.ShowError(LoginError.INVALID_ID_TOKEN), awaitItem())
            expectNoEvents()
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailure_kakaoServerUnavailable_showsKakaoUnavailableError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 카카오 서버 연결 불가(503) — 502 공개키 조회 실패와 같은 갈래로 묶인다
        coEvery { loginWithKakaoUseCase(any(), any()) } returns Result.failure(
            AppError.Server(code = "KAKAO_SERVER_UNAVAILABLE", statusCode = 503, serverMessage = "…"),
        )
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 카카오 쪽 문제로 안내한다
            assertEquals(LoginSideEffect.ShowError(LoginError.KAKAO_UNAVAILABLE), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailure_unclassifiedServerError_showsUnknownError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앱이 모르는 서버 에러 코드
        coEvery { loginWithKakaoUseCase(any(), any()) } returns Result.failure(
            AppError.Server(code = "SOME_NEW_CODE", statusCode = 500, serverMessage = "…"),
        )
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 서버가 코드를 추가해도 앱은 일반 문구로 안내한다
            assertEquals(LoginSideEffect.ShowError(LoginError.UNKNOWN), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailure_networkError_showsNetworkErrorAndTurnsOffLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 네트워크 단절
        coEvery { loginWithKakaoUseCase(any(), any()) } returns
            Result.failure(AppError.Network(null))
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 재시도할 수 있는 갈래라고 알리고 로딩이 풀린다
            assertEquals(LoginSideEffect.ShowError(LoginError.NETWORK), awaitItem())
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginFailure_useCaseThrows_showsUnknownErrorAndTurnsOffLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given UseCase 가 Result.failure 가 아니라 예외를 그대로 던진다
        coEvery { loginWithKakaoUseCase(any(), any()) } throws IllegalStateException("boom")
        val viewModel = viewModel()

        viewModel.effect.test {
            // Given 로딩이 이미 켜져 있다(SDK 로그인 요청 단계)
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()
            assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())
            assertTrue(viewModel.state.value.isLoading)

            // When SDK 성공 결과를 전달했지만 서버 UseCase 가 던진다
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then `Result.failure` 경로와 똑같이 알린다 — 던지는 실패만 조용하면 안 된다
            assertEquals(LoginSideEffect.ShowError(LoginError.UNKNOWN), awaitItem())
            // Then 로딩이 풀려 버튼이 영구 비활성으로 남지 않는다
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sdkCancel_turnsOffLoadingWithoutShowingError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그인 진행 중
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()
            assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())

            // When 사용자가 카카오 화면에서 취소
            viewModel.processIntent(LoginIntent.LoginWithKakaoCancel)
            runCurrent()

            // Then 로딩만 풀리고 아무 안내도 안 뜬다(취소는 에러가 아니다)
            expectNoEvents()
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sdkFailure_showsKakaoUnavailableErrorAndTurnsOffLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그인 진행 중
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()
            assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())

            // When SDK 가 실패를 돌려준다(idToken null 포함)
            viewModel.processIntent(LoginIntent.LoginWithKakaoFailure(IllegalStateException("idToken null")))
            runCurrent()

            // Then 카카오 쪽 문제로 안내하고 로딩이 풀린다
            assertEquals(LoginSideEffect.ShowError(LoginError.KAKAO_UNAVAILABLE), awaitItem())
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
