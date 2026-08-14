package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class LoginWithKakaoUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val useCase = LoginWithKakaoUseCase(authRepository)

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-1"),
        refreshToken = RefreshToken("refresh-1"),
        expiresIn = 3600.seconds,
    )

    @Test
    fun invoke_existingMember_savesSession() = runTest {
        // Given 기존 회원 응답
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))

        // When 로그인
        useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 세션이 저장된다 — 화면이 잊을 수 없도록 여기서 한다
        coVerify(exactly = 1) { authRepository.saveSession(session) }
    }

    @Test
    fun invoke_newUser_doesNotSaveSession() = runTest {
        // Given 신규 회원 응답(세션이 아직 없다)
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.NewUser(RegistrationToken("reg-1")))

        // When 로그인
        useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 저장 호출이 없다
        coVerify(exactly = 0) { authRepository.saveSession(any()) }
    }

    @Test
    fun invoke_failure_propagatesErrorAndSkipsSave() = runTest {
        // Given 서버가 401 로 실패
        coEvery { authRepository.loginWithKakao(any(), any()) } returns Result.failure(
            AppError.Server(code = "INVALID_ID_TOKEN", statusCode = 401, serverMessage = "…"),
        )

        // When 로그인
        val result = useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 실패가 그대로 전달되고 저장하지 않는다
        assertIs<AppError.Server>(result.exceptionOrNull())
        coVerify(exactly = 0) { authRepository.saveSession(any()) }
    }
}
