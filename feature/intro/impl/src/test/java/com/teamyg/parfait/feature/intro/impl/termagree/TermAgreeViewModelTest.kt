package com.teamyg.parfait.feature.intro.impl.termagree

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import com.teamyg.parfait.domain.usecase.auth.SignUpUseCase
import com.teamyg.parfait.domain.usecase.policy.GetPoliciesUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TermAgreeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePolicyRepository(
        var result: Result<List<PolicyVO>>,
    ) : PolicyRepository {
        override suspend fun getPolicies(): Result<List<PolicyVO>> = result
    }

    /**
     * @param gate 값이 있으면 이 게이트가 완료될 때까지 가입 요청이 진행 중 상태로 머문다
     */
    private class FakeAuthRepository(
        private val result: Result<AuthSessionVO> = Result.success(SESSION),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : AuthRepository {
        var callCount = 0
            private set
        var requestedAgreements: List<TermsAgreement>? = null
            private set

        override suspend fun signUp(
            registrationToken: RegistrationToken,
            agreements: List<TermsAgreement>,
        ): Result<AuthSessionVO> {
            callCount++
            requestedAgreements = agreements
            gate?.await()
            return result
        }
    }

    private fun viewModel(
        policyRepository: PolicyRepository = FakePolicyRepository(Result.success(POLICIES)),
        authRepository: AuthRepository = FakeAuthRepository(),
    ) = TermAgreeViewModel(
        registrationTokenValue = "registration-token",
        getPolicies = GetPoliciesUseCase(policyRepository),
        signUp = SignUpUseCase(authRepository),
    )

    private fun loadedViewModel(authRepository: AuthRepository = FakeAuthRepository()) =
        viewModel(authRepository = authRepository)

    @Test
    fun init_loadsPoliciesFromUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 화면 진입
        val viewModel = viewModel()

        // When 조회가 끝나면
        advanceUntilIdle()

        // Then 약관이 채워지고 로딩이 끝난다
        val state = viewModel.state.value
        assertEquals(POLICIES, state.policies)
        assertFalse(state.isLoading)
        assertFalse(state.isLoadFailed)
    }

    @Test
    fun init_loadFails_marksLoadFailed() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 조회가 실패한다
        val viewModel = viewModel(policyRepository = FakePolicyRepository(Result.failure(IOException("network"))))

        // When 조회가 끝나면
        advanceUntilIdle()

        // Then 실패 상태가 되고 다음으로 갈 수 없다
        val state = viewModel.state.value
        assertTrue(state.isLoadFailed)
        assertFalse(state.isLoading)
        assertFalse(state.isAvailable)
    }

    @Test
    fun clickRetryLoad_afterFailure_loadsAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 실패한 화면
        val repository = FakePolicyRepository(Result.failure(IOException("network")))
        val viewModel = viewModel(policyRepository = repository)
        advanceUntilIdle()

        // When 서버가 정상으로 돌아온 뒤 다시 시도
        repository.result = Result.success(POLICIES)
        viewModel.processIntent(TermAgreeIntent.ClickRetryLoad)
        advanceUntilIdle()

        // Then 약관이 채워지고 실패 표시가 사라진다
        val state = viewModel.state.value
        assertEquals(POLICIES, state.policies)
        assertFalse(state.isLoadFailed)
    }

    @Test
    fun initialState_beforeLoad_isNotAvailable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 화면 진입 직후
        val viewModel = viewModel()

        // Then 약관이 없으므로 다음으로 갈 수 없다
        val state = viewModel.state.value
        assertTrue(state.isLoading)
        assertFalse(state.isAvailable)
        assertFalse(state.isAllSelected)
    }

    @Test
    fun clickTermAgree_agreesOnlyThatPolicy() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관을 받아온 화면
        val viewModel = loadedViewModel()
        advanceUntilIdle()

        // When 필수 약관만 동의
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(1L), newSelected = true))

        // Then 해당 약관만 동의 상태가 된다
        val state = viewModel.state.value
        assertTrue(state.isAgreed(REQUIRED_POLICY))
        assertFalse(state.isAgreed(OPTIONAL_POLICY))
        assertFalse(state.isAllSelected)
    }

    @Test
    fun clickTermAgree_requiredPolicyAgreed_becomesAvailable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관을 받아온 화면
        val viewModel = loadedViewModel()
        advanceUntilIdle()

        // When 필수 약관에만 동의
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(1L), newSelected = true))

        // Then 선택 약관이 남아 있어도 다음으로 갈 수 있다
        assertTrue(viewModel.state.value.isAvailable)
    }

    @Test
    fun clickAgreeAllTerm_agreesEveryPolicy() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관을 받아온 화면
        val viewModel = loadedViewModel()
        advanceUntilIdle()

        // When 전체 동의
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        // Then 모든 약관이 동의 상태가 된다
        val state = viewModel.state.value
        assertTrue(state.isAllSelected)
        assertTrue(state.isAvailable)
    }

    @Test
    fun clickAgreeAllTerm_deselect_clearsEveryPolicy() = runTest(mainDispatcherRule.dispatcher) {
        // Given 전체 동의된 화면
        val viewModel = loadedViewModel()
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        // When 전체 동의를 해제
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = false))

        // Then 모두 해제되고 다음으로 갈 수 없다
        val state = viewModel.state.value
        assertFalse(state.isAllSelected)
        assertFalse(state.isAvailable)
    }

    @Test
    fun clickTermLandingUrl_emitsNavigateToUrl() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 화면
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            // When 약관 상세 링크 클릭
            viewModel.processIntent(TermAgreeIntent.ClickTermLandingUrl("https://example.com/terms"))

            // Then 클릭한 URL 그대로 이동 이펙트가 나간다
            assertEquals(TermAgreeSideEffect.NavigateToUrl("https://example.com/terms"), awaitItem())
        }
    }

    @Test
    fun clickBackButton_emitsNavigateToBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 화면
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            // When 뒤로가기 클릭
            viewModel.processIntent(TermAgreeIntent.ClickBackButton)

            // Then 뒤로가기 이펙트가 나간다
            assertEquals(TermAgreeSideEffect.NavigateToBack, awaitItem())
        }
    }

    @Test
    fun clickNextButton_signsUpAndNavigatesToNext() = runTest(mainDispatcherRule.dispatcher) {
        // Given 전체 동의된 화면과 아직 응답하지 않는 서버
        val gate = CompletableDeferred<Unit>()
        val authRepository = FakeAuthRepository(gate = gate)
        val viewModel = loadedViewModel(authRepository)
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        viewModel.effect.test {
            // When 다음 버튼 클릭
            viewModel.processIntent(TermAgreeIntent.ClickNextButton)
            runCurrent()

            // Then 응답 전까지 가입 중 상태로 머물고
            assertTrue(viewModel.state.value.isSigningUp)

            gate.complete(Unit)
            advanceUntilIdle()

            // Then 가입이 끝나면 다음 화면으로 이동한다
            assertEquals(TermAgreeSideEffect.NavigateToNext, awaitItem())
            assertFalse(viewModel.state.value.isSigningUp)
        }
    }

    @Test
    fun clickNextButton_sendsEveryShownPolicyWithAgreedFlag() = runTest(mainDispatcherRule.dispatcher) {
        // Given 필수 약관만 동의한 화면
        val authRepository = FakeAuthRepository()
        val viewModel = loadedViewModel(authRepository)
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(1L), newSelected = true))

        // When 다음 버튼 클릭
        viewModel.processIntent(TermAgreeIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 미동의 약관도 agreed=false 로 함께 나간다
        assertEquals(
            listOf(
                TermsAgreement(termsId = TermsId(1L), agreed = true),
                TermsAgreement(termsId = TermsId(2L), agreed = false),
            ),
            authRepository.requestedAgreements,
        )
    }

    @Test
    fun clickNextButton_requiredPolicyNotAgreed_doesNotSignUp() = runTest(mainDispatcherRule.dispatcher) {
        // Given 선택 약관에만 동의한 화면
        val authRepository = FakeAuthRepository()
        val viewModel = loadedViewModel(authRepository)
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(2L), newSelected = true))

        viewModel.effect.test {
            // When 다음 버튼 클릭
            viewModel.processIntent(TermAgreeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 가입 요청 자체가 나가지 않는다
            assertEquals(0, authRepository.callCount)
            expectNoEvents()
        }
    }

    @Test
    fun clickNextButton_whileSigningUp_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        // Given 가입 요청이 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        val authRepository = FakeAuthRepository(gate = gate)
        val viewModel = loadedViewModel(authRepository)
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))
        viewModel.processIntent(TermAgreeIntent.ClickNextButton)
        runCurrent()

        // When 다음 버튼을 한 번 더 클릭
        viewModel.processIntent(TermAgreeIntent.ClickNextButton)
        runCurrent()

        // Then 중복 가입 요청이 나가지 않는다
        assertEquals(1, authRepository.callCount)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun clickNextButton_signUpFails_staysOnScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 가입이 실패하는 화면
        val authRepository = FakeAuthRepository(Result.failure(IOException("network")))
        val viewModel = loadedViewModel(authRepository)
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))

        viewModel.effect.test {
            // When 다음 버튼 클릭
            viewModel.processIntent(TermAgreeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 이동하지 않고 다시 시도할 수 있는 상태로 돌아온다
            expectNoEvents()
            assertFalse(viewModel.state.value.isSigningUp)
        }
    }

    companion object {
        private val REQUIRED_POLICY = PolicyVO(
            termsId = TermsId(1L),
            type = PolicyType.TERMS_OF_SERVICE,
            title = "서비스 이용약관",
            url = "https://example.com/terms",
            required = true,
        )

        private val OPTIONAL_POLICY = PolicyVO(
            termsId = TermsId(2L),
            type = PolicyType.PRIVACY_POLICY,
            title = "마케팅 정보 수신 동의",
            url = "https://example.com/marketing",
            required = false,
        )

        private val POLICIES = listOf(REQUIRED_POLICY, OPTIONAL_POLICY)

        private val SESSION = AuthSessionVO(
            accessToken = AccessToken("access-token"),
            refreshToken = RefreshToken("refresh-token"),
            expiresIn = 3600.seconds,
        )
    }
}
