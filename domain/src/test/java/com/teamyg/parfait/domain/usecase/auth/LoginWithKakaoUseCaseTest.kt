package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.usecase.member.RefreshMyAccountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LoginWithKakaoUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val refreshMyAccount: RefreshMyAccountUseCase = mockk()
    private val useCase = LoginWithKakaoUseCase(authRepository, refreshMyAccount)

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-1"),
        refreshToken = RefreshToken("refresh-1"),
        expiresIn = 3600.seconds,
    )

    private val myAccount = MyAccountVO(
        memberId = MemberId(1L),
        provider = LoginProvider.KAKAO,
        nickname = GlobalNickname("nickname"),
    )

    @Test
    fun invoke_existingMember_savesSession() = runTest {
        // Given 기존 회원 응답
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 로그인
        useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 세션이 저장된다 — 화면이 잊을 수 없도록 여기서 한다
        coVerify(exactly = 1) { authRepository.saveSession(session) }
    }

    @Test
    fun invoke_loginSucceeds_refreshesAccount() = runTest {
        // Given 기존 회원 로그인이 성공한다
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 로그인한다
        useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 세션 저장과 함께 계정 정보를 한 번 당겨온다
        coVerify(exactly = 1) { refreshMyAccount() }
    }

    @Test
    fun invoke_accountRefreshFails_loginStillSucceeds() = runTest {
        // Given 로그인은 성공하나 계정 조회가 실패한다
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))
        coEvery { refreshMyAccount() } returns Result.failure(AppError.Network(cause = null))

        // When 로그인한다
        val result = useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 로그인 결과는 성공이다 — 그 시점에 되돌릴 곳이 없고 값은 다음 진입에서 채워진다
        assertTrue(result.isSuccess)
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

    @Test
    fun invoke_saveSessionThrows_returnsFailureInsteadOfThrowing() = runTest {
        // Given 기존 회원 응답이지만 세션 저장(KeyStore·DataStore IO)이 던진다
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))
        coEvery { authRepository.saveSession(session) } throws IllegalStateException("keystore boom")

        // When 로그인 — throw 하지 않고 Result 로 돌아와야 한다
        val result = useCase(idToken = "id-1", nonce = "nonce-1")

        // Then Result.failure(AppError.Unexpected) 로 감싸져 있다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }
}
