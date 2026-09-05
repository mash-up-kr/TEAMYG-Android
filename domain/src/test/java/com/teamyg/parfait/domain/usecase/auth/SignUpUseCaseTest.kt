package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.exception.SignUpException
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.usecase.member.RefreshMyAccountUseCase
import com.teamyg.parfait.domain.usecase.notification.RegisterCurrentDeviceTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SignUpUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val refreshMyAccount: RefreshMyAccountUseCase = mockk()
    private val registerCurrentDeviceToken: RegisterCurrentDeviceTokenUseCase = mockk(relaxed = true)
    private val useCase = SignUpUseCase(authRepository, refreshMyAccount, registerCurrentDeviceToken)

    private val registrationToken = RegistrationToken("registration-token")

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-token"),
        refreshToken = RefreshToken("refresh-token"),
        expiresIn = 3600.seconds,
    )

    private val myAccount = MyAccountVO(
        memberId = MemberId(1L),
        provider = LoginProvider.KAKAO,
        nickname = GlobalNickname("nickname"),
    )

    private val requiredPolicy = policy(id = 1L, required = true)
    private val optionalPolicy = policy(id = 2L, required = false)

    private fun policy(
        id: Long,
        required: Boolean,
    ) = PolicyVO(
        termsId = TermsId(id),
        type = PolicyType.TERMS_OF_SERVICE,
        title = "약관 $id",
        url = "https://example.com/$id",
        required = required,
    )

    @Test
    fun invoke_allPoliciesAgreed_returnsSession() = runTest {
        // Given 모든 약관에 동의한 상태
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 가입 요청
        val result = useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy, optionalPolicy),
            agreedTermsIds = setOf(TermsId(1L), TermsId(2L)),
        )

        // Then 세션을 돌려주고 토큰이 그대로 전달된다
        assertEquals(session, result.getOrThrow())
        coVerify(exactly = 1) { authRepository.signUp(registrationToken = registrationToken, agreements = any()) }
    }

    @Test
    fun invoke_success_savesSession() = runTest {
        // Given 가입에 성공한다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 가입 요청
        useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 세션이 저장된다 — 화면이 잊을 수 없도록 여기서 한다
        coVerify(exactly = 1) { authRepository.saveSession(session) }
    }

    @Test
    fun invoke_optionalPolicyNotAgreed_sendsItAsNotAgreed() = runTest {
        // Given 필수 약관만 동의한 상태
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 가입 요청
        useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy, optionalPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 미동의 약관도 agreed=false 로 함께 보낸다
        coVerify(exactly = 1) {
            authRepository.signUp(
                registrationToken = registrationToken,
                agreements = listOf(
                    TermsAgreement(termsId = TermsId(1L), agreed = true),
                    TermsAgreement(termsId = TermsId(2L), agreed = false),
                ),
            )
        }
    }

    @Test
    fun invoke_requiredPolicyNotAgreed_failsWithoutCallingRepository() = runTest {
        // Given 필수 약관에 동의하지 않은 상태

        // When 가입 요청
        val result = useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy, optionalPolicy),
            agreedTermsIds = setOf(TermsId(2L)),
        )

        // Then 서버로 나가지 않고 도메인에서 막는다
        assertTrue(result.isFailure)
        val error = assertIs<SignUpException.RequiredPolicyNotAgreed>(result.exceptionOrNull())
        assertEquals(listOf(TermsId(1L)), error.termsIds)
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
    }

    @Test
    fun invoke_agreedIdNotInPolicies_isNotSent() = runTest {
        // Given 목록에 없는 약관 id 가 동의 목록에 섞여 있다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 가입 요청
        useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L), TermsId(999L)),
        )

        // Then 화면에 노출한 약관만 전송한다
        coVerify(exactly = 1) {
            authRepository.signUp(
                registrationToken = registrationToken,
                agreements = listOf(TermsAgreement(termsId = TermsId(1L), agreed = true)),
            )
        }
    }

    @Test
    fun invoke_repositoryFails_propagatesFailureAndSkipsSave() = runTest {
        // Given 저장소가 실패를 준다
        coEvery { authRepository.signUp(any(), any()) } returns
            Result.failure(AppError.Network(cause = null))

        // When 가입 요청
        val result = useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 실패가 그대로 전달되고 저장하지 않는다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { authRepository.saveSession(any()) }
    }

    @Test
    fun invoke_saveSessionThrows_returnsFailureInsteadOfThrowing() = runTest {
        // Given 가입은 성공했지만 세션 저장(KeyStore·DataStore IO)이 던진다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { authRepository.saveSession(session) } throws IllegalStateException("keystore boom")

        // When 가입 요청 — throw 하지 않고 Result 로 돌아와야 한다
        val result = useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then Result.failure(AppError.Unexpected) 로 감싸져 있다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }

    @Test
    fun invoke_signUpSucceeds_refreshesAccount() = runTest {
        // Given 가입이 성공한다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 가입 요청
        useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 세션 저장과 함께 계정 정보를 한 번 당겨온다
        coVerify(exactly = 1) { refreshMyAccount() }
    }

    @Test
    fun invoke_accountRefreshFails_signUpStillSucceeds() = runTest {
        // Given 가입은 성공하나 계정 조회가 실패한다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.failure(AppError.Network(cause = null))

        // When 가입 요청
        val result = useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 가입 결과는 성공이다 — 그 시점에 되돌릴 곳이 없고 값은 다음 진입에서 채워진다
        assertTrue(result.isSuccess)
    }

    @Test
    fun invoke_signUpSucceeds_registersDeviceToken() = runTest {
        // Given 가입이 성공한다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)

        // When 가입 요청
        useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 세션이 저장된 뒤에 등록한다 — 이 엔드포인트는 인증이 필요하다(화이트리스트 밖)
        coVerifyOrder {
            authRepository.saveSession(session)
            registerCurrentDeviceToken()
        }
    }

    @Test
    fun invoke_deviceTokenRegistrationFails_signUpStillSucceeds() = runTest {
        // Given 가입은 성공하나 기기 토큰 등록이 실패한다
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(session)
        coEvery { refreshMyAccount() } returns Result.success(myAccount)
        coEvery { registerCurrentDeviceToken() } returns Result.failure(AppError.Network(cause = null))

        // When 가입 요청
        val result = useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = setOf(TermsId(1L)),
        )

        // Then 가입 결과는 성공이다 — 등록 실패는 다음 등록 시점이 메운다
        assertTrue(result.isSuccess)
    }

    @Test
    fun invoke_requiredPolicyNotAgreed_doesNotRegisterDeviceToken() = runTest {
        // Given 필수 약관에 동의하지 않아 가입 자체가 성립하지 않는다

        // When 가입 요청
        useCase(
            registrationToken = registrationToken,
            policies = listOf(requiredPolicy),
            agreedTermsIds = emptySet(),
        )

        // Then 세션이 없으니 등록도 부르지 않는다
        coVerify(exactly = 0) { registerCurrentDeviceToken() }
    }
}
