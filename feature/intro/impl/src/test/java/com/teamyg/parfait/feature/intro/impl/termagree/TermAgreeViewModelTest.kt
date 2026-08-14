package com.teamyg.parfait.feature.intro.impl.termagree

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.usecase.auth.SignUpUseCase
import com.teamyg.parfait.domain.usecase.policy.GetPoliciesUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TermAgreeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPolicies: GetPoliciesUseCase = mockk()
    private val signUp: SignUpUseCase = mockk()

    /** 약관 조회는 `init` 에서 돌므로 ViewModel 을 만들기 전에 응답을 정해 둔다 */
    private fun viewModel() = TermAgreeViewModel(
        registrationTokenValue = REGISTRATION_TOKEN_VALUE,
        getPolicies = getPolicies,
        signUp = signUp,
    )

    private fun givenPoliciesLoaded() {
        coEvery { getPolicies() } returns Result.success(POLICIES)
    }

    private fun givenSignUpSucceeds() {
        coEvery { signUp(any(), any(), any()) } returns Result.success(SESSION)
    }

    @Test
    fun init_loadsPoliciesFromUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 화면 진입
        givenPoliciesLoaded()
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
        // Given 약관 조회가 네트워크 단절로 실패한다
        coEvery { getPolicies() } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel()

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
        coEvery { getPolicies() } returns
            Result.failure(AppError.Network(cause = null)) andThen Result.success(POLICIES)
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 다시 시도
        viewModel.processIntent(TermAgreeIntent.ClickRetryLoad)
        advanceUntilIdle()

        // Then 약관이 채워지고 실패 표시가 사라진다
        val state = viewModel.state.value
        assertEquals(POLICIES, state.policies)
        assertFalse(state.isLoadFailed)
    }

    @Test
    fun clickRetryLoad_whileLoading_doesNotStartSecondLoad() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { getPolicies() } coAnswers {
            gate.await()
            Result.success(POLICIES)
        }
        val viewModel = viewModel()
        runCurrent()

        // When 재시도를 누른다
        viewModel.processIntent(TermAgreeIntent.ClickRetryLoad)
        runCurrent()

        // Then 조회가 중복으로 나가지 않고 로딩 표시는 유지된다
        assertTrue(viewModel.state.value.isLoading)
        coVerify(exactly = 1) { getPolicies() }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun initialState_beforeLoad_isNotAvailable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관 화면 진입 직후
        givenPoliciesLoaded()
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
        givenPoliciesLoaded()
        val viewModel = viewModel()
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
        givenPoliciesLoaded()
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 필수 약관에만 동의
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(1L), newSelected = true))

        // Then 선택 약관이 남아 있어도 다음으로 갈 수 있다
        assertTrue(viewModel.state.value.isAvailable)
    }

    @Test
    fun clickAgreeAllTerm_agreesEveryPolicy() = runTest(mainDispatcherRule.dispatcher) {
        // Given 약관을 받아온 화면
        givenPoliciesLoaded()
        val viewModel = viewModel()
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
        givenPoliciesLoaded()
        val viewModel = viewModel()
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
        givenPoliciesLoaded()
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
        givenPoliciesLoaded()
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
        givenPoliciesLoaded()
        val gate = CompletableDeferred<Unit>()
        coEvery { signUp(any(), any(), any()) } coAnswers {
            gate.await()
            Result.success(SESSION)
        }
        val viewModel = viewModel()
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
    fun clickNextButton_passesShownPoliciesAndAgreedIdsToUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 필수 약관만 동의한 화면
        givenPoliciesLoaded()
        givenSignUpSucceeds()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(1L), newSelected = true))

        // When 다음 버튼 클릭
        viewModel.processIntent(TermAgreeIntent.ClickNextButton)
        advanceUntilIdle()

        // Then 화면에 노출한 약관 전체와 동의한 id 를 그대로 넘긴다
        // (미동의 약관을 agreed=false 로 바꿔 보내는 것은 UseCase 의 몫이다)
        coVerify(exactly = 1) {
            signUp(
                registrationToken = RegistrationToken(REGISTRATION_TOKEN_VALUE),
                policies = POLICIES,
                agreedTermsIds = setOf(TermsId(1L)),
            )
        }
    }

    @Test
    fun clickNextButton_requiredPolicyNotAgreed_doesNotSignUp() = runTest(mainDispatcherRule.dispatcher) {
        // Given 선택 약관에만 동의한 화면
        givenPoliciesLoaded()
        givenSignUpSucceeds()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId = TermsId(2L), newSelected = true))

        viewModel.effect.test {
            // When 다음 버튼 클릭
            viewModel.processIntent(TermAgreeIntent.ClickNextButton)
            advanceUntilIdle()

            // Then 가입 요청 자체가 나가지 않는다
            coVerify(exactly = 0) { signUp(any(), any(), any()) }
            expectNoEvents()
        }
    }

    @Test
    fun clickNextButton_whileSigningUp_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        // Given 가입 요청이 아직 끝나지 않은 화면
        givenPoliciesLoaded()
        val gate = CompletableDeferred<Unit>()
        coEvery { signUp(any(), any(), any()) } coAnswers {
            gate.await()
            Result.success(SESSION)
        }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(newSelected = true))
        viewModel.processIntent(TermAgreeIntent.ClickNextButton)
        runCurrent()

        // When 다음 버튼을 한 번 더 클릭
        viewModel.processIntent(TermAgreeIntent.ClickNextButton)
        runCurrent()

        // Then 중복 가입 요청이 나가지 않는다
        coVerify(exactly = 1) { signUp(any(), any(), any()) }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun clickNextButton_signUpFails_staysOnScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 가입이 네트워크 단절로 실패하는 화면
        givenPoliciesLoaded()
        coEvery { signUp(any(), any(), any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel()
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
        private const val REGISTRATION_TOKEN_VALUE = "registration-token"

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
