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
import kotlin.test.assertTrue
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

    @Test
    fun logout_serverSucceeds_clearsLocalTokens() = runTest {
        // Given 로그인 상태이고 서버 로그아웃이 성공한다
        coEvery { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { remoteDataSource.logout(RefreshToken("refresh")) } returns Result.success(Unit)
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 성공이고 로컬 토큰이 지워진다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun logout_serverFails_stillClearsLocalTokens() = runTest {
        // Given 서버 로그아웃이 실패한다
        coEvery { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { remoteDataSource.logout(RefreshToken("refresh")) } returns
            Result.failure(ApiException.Network(IOException("연결 실패")))
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 사용자가 눌렀으니 이 기기에서는 나간다 — 서버 실패는 전파하지 않는다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun logout_refreshTokenRotatedMidFlight_retriesOnceWithNewToken() = runTest {
        // Given access token 이 만료된 채 로그아웃해 인증기가 재발급을 태웠고,
        // 그 재발급이 refresh token 을 회전시켜 방금 본문에 실어 보낸 값이 죽었다
        coEvery { tokenStore.getRefreshToken() } returnsMany listOf("old-refresh", "new-refresh")
        coEvery { remoteDataSource.logout(RefreshToken("old-refresh")) } returns
            Result.failure(
                ApiException.Business(
                    code = "INVALID_TOKEN",
                    serverMessage = "유효하지 않은 토큰입니다",
                    statusCode = 401,
                    errorDetail = null,
                ),
            )
        coEvery { remoteDataSource.logout(RefreshToken("new-refresh")) } returns Result.success(Unit)
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 회전된 새 값으로 딱 한 번 다시 보내 서버 세션을 실제로 끊고, 로컬도 정리한다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remoteDataSource.logout(RefreshToken("old-refresh")) }
        coVerify(exactly = 1) { remoteDataSource.logout(RefreshToken("new-refresh")) }
        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun logout_serverFailsWithoutRotation_doesNotRetry() = runTest {
        // Given 서버 호출이 실패했지만 refresh token 은 그대로다(회전이 없었다)
        coEvery { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { remoteDataSource.logout(RefreshToken("refresh")) } returns
            Result.failure(ApiException.Network(IOException("연결 실패")))
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 같은 값으로 다시 보내지 않는다 — 재시도는 회전됐을 때만이다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remoteDataSource.logout(any()) }
        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun logout_noRefreshToken_clearsWithoutCallingServer() = runTest {
        // Given 이미 토큰이 없는 상태
        coEvery { tokenStore.getRefreshToken() } returns null
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 서버를 부르지 않고 로컬만 정리한다
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { remoteDataSource.logout(any()) }
        coVerify(exactly = 1) { tokenStore.clear() }
    }
}
