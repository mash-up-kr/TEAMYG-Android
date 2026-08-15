package com.teamyg.parfait.data.repository.auth

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSource
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.TermsId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class AuthRepositoryImplTest {
    private val remoteDataSource: AuthRemoteDataSource = mockk()
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val repository = AuthRepositoryImpl(
        authRemoteDataSource = remoteDataSource,
        tokenStore = tokenStore,
    )

    private val registrationToken = RegistrationToken("registration-token")
    private val agreements = listOf(TermsAgreement(termsId = TermsId(1L), agreed = true))

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-1"),
        refreshToken = RefreshToken("refresh-1"),
        expiresIn = 3600.seconds,
    )

    @Test
    fun loginWithKakao_remoteSucceeds_returnsVoUnchanged() = runTest {
        // Given 원격이 신규 회원으로 응답
        val vo = KakaoLoginVO.NewUser(RegistrationToken("reg-1"))
        coEvery { remoteDataSource.loginWithKakao("id-1", "nonce-1") } returns Result.success(vo)

        // When 로그인
        val result = repository.loginWithKakao(idToken = "id-1", nonce = "nonce-1")

        // Then VO 가 그대로 나온다
        assertEquals(vo, result.getOrNull())
    }

    @Test
    fun loginWithKakao_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 원격이 401 INVALID_ID_TOKEN 으로 실패
        coEvery { remoteDataSource.loginWithKakao(any(), any()) } returns Result.failure(
            ApiException.Business(
                code = "INVALID_ID_TOKEN",
                serverMessage = "유효하지 않은 ID 토큰입니다",
                statusCode = 401,
                errorDetail = null,
            ),
        )

        // When 로그인
        val result = repository.loginWithKakao(idToken = "id-1", nonce = "nonce-1")

        // Then 도메인 에러로 바뀌어 나온다(호출부가 :data 를 보지 않아도 된다)
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("INVALID_ID_TOKEN", error.code)
        assertEquals(401, error.statusCode)
    }

    @Test
    fun loginWithKakao_remoteFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { remoteDataSource.loginWithKakao(any(), any()) } returns
            Result.failure(ApiException.Network(IOException("offline")))

        // When 로그인
        val result = repository.loginWithKakao(idToken = "id-1", nonce = "nonce-1")

        // Then Network 갈래다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun signUp_remoteSucceeds_returnsSession() = runTest {
        // Given 가입에 성공한다
        coEvery { remoteDataSource.signup(any(), any()) } returns Result.success(session)

        // When 가입 요청
        val result = repository.signUp(registrationToken, agreements)

        // Then 세션을 그대로 돌려준다
        assertEquals(session, result.getOrThrow())
    }

    @Test
    fun signUp_passesTokenAndAgreementsToDataSource() = runTest {
        // Given 가입에 성공한다
        coEvery { remoteDataSource.signup(any(), any()) } returns Result.success(session)

        // When 가입 요청
        repository.signUp(registrationToken, agreements)

        // Then 입력값이 가공 없이 전달된다
        coVerify(exactly = 1) {
            remoteDataSource.signup(
                registrationToken = registrationToken,
                agreements = agreements,
            )
        }
    }

    @Test
    fun signUp_remoteFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { remoteDataSource.signup(any(), any()) } returns
            Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 가입 요청
        val result = repository.signUp(registrationToken, agreements)

        // Then Network 갈래로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun saveSession_delegatesRawTokenValuesToTokenStore() = runTest {
        // When 저장
        repository.saveSession(session)

        // Then value class 를 벗겨 원시 문자열로 저장한다
        coVerify(exactly = 1) {
            tokenStore.save(accessToken = "access-1", refreshToken = "refresh-1")
        }
    }
}
