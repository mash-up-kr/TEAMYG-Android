package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.member.ChangeGlobalNicknameUseCase
import com.teamyg.parfait.domain.usecase.member.ObserveMyAccountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountInfoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val changeGlobalNickname: ChangeGlobalNicknameUseCase = mockk()
    private val observeMyAccount: ObserveMyAccountUseCase = mockk()

    private fun viewModel(accountFlow: Flow<MyAccountVO?> = flowOf(null)): AccountInfoViewModel {
        every { observeMyAccount() } returns accountFlow
        return AccountInfoViewModel(
            observeMyAccount = observeMyAccount,
            checkNameValid = CheckNameValidUseCase(),
            changeGlobalNickname = changeGlobalNickname,
        )
    }

    private fun accountOf(nickname: String) = MyAccountVO(
        memberId = MemberId(1L),
        provider = LoginProvider.KAKAO,
        nickname = GlobalNickname(nickname),
    )

    @Test
    fun init_beforeSSoTEmits_nicknameIsNull() = runTest(mainDispatcherRule.dispatcher) {
        // Given SSoT 가 아직 아무 것도 방출하지 않은 첫 프레임
        val viewModel = viewModel(accountFlow = MutableSharedFlow())
        runCurrent()

        // Then null 은 로딩을 의미한다 — placeholder 문자열이 아니다
        assertNull(viewModel.state.value.nickname)
    }

    @Test
    fun init_ssotEmitsAccount_updatesNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given SSoT 에 저장된 계정 정보
        // When 화면이 SSoT 를 구독한다
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()

        // Then 상태가 SSoT 값을 그대로 반영한다
        assertEquals("모카", viewModel.state.value.nickname)
    }

    @Test
    fun clickConfirm_succeeds_doesNotWriteResultDirectly_followsSSoTInstead() = runTest(mainDispatcherRule.dispatcher) {
        // Given SSoT 를 직접 제어할 수 있는 화면(리포지토리가 로컬에 쓴 뒤 흘려보내는 것을 흉내)
        val ssot = MutableStateFlow<MyAccountVO?>(accountOf("모카"))
        coEvery { changeGlobalNickname(any()) } returns Result.success(GlobalNickname("라떼"))
        val viewModel = viewModel(accountFlow = ssot)
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord("라떼"))

        // When 확인을 누른다 — 서버는 성공했지만 SSoT 는 아직 갱신되지 않았다
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        advanceUntilIdle()

        // Then 성공했다고 해서 오류가 남지 않고, 요청이 끝났다는 것만 반영된다
        assertNull(viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.isSubmitting)

        // When SSoT 가 실제로 갱신된다
        ssot.value = accountOf("서버가확정한값")
        advanceUntilIdle()

        // Then 화면은 SSoT 를 따라간다 — 변경 결과값이 아니라 SSoT 가 정본이다
        assertEquals("서버가확정한값", viewModel.state.value.nickname)
    }

    @Test
    fun clickConfirm_invalidFormat_doesNotCallServer() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앞뒤 공백이 있는 닉네임
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord(" 라떼"))

        // When 확인을 누른다
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        advanceUntilIdle()

        // Then 요청 전에 걸러지고 형식 오류만 표시된다
        coVerify(exactly = 0) { changeGlobalNickname(any()) }
        assertIs<NameValidResult.Error.SpaceAtEdge>(viewModel.state.value.nicknameError)
    }

    @Test
    fun clickConfirm_serverRejectsWithInvalidNickname_showsInvalidError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 400 INVALID_NICKNAME 으로 응답
        coEvery { changeGlobalNickname(any()) } returns Result.failure(
            AppError.Server(
                code = ServerErrorCode.Member.INVALID_NICKNAME,
                statusCode = 400,
                serverMessage = "…",
            ),
        )
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord("라떼"))

        // When 확인을 누른다
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        advanceUntilIdle()

        // Then 서버 실패 사유가 형식 오류와 별개 필드에 붙는다
        assertEquals(GlobalNicknameError.INVALID, viewModel.state.value.submitError)
        assertNull(viewModel.state.value.nicknameError)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun clickConfirm_networkFails_showsNetworkError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연결 실패
        coEvery { changeGlobalNickname(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord("라떼"))

        // When 확인을 누른다
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        advanceUntilIdle()

        // Then 네트워크 사유가 붙고 다시 시도할 수 있는 상태로 돌아온다
        assertEquals(GlobalNicknameError.NETWORK, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun clickConfirm_whileRequestInFlight_marksSubmittingUntilItFinishes() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임 변경 요청이 끝나지 않게 붙잡아 둔다
        val gate = CompletableDeferred<Unit>()
        coEvery { changeGlobalNickname(any()) } coAnswers {
            gate.await()
            Result.success(GlobalNickname("라떼"))
        }
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord("라떼"))
        assertFalse(viewModel.state.value.isSubmitting)

        // When 확인을 누른다
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        runCurrent()

        // Then 요청 중이라 확인 버튼이 비활성 상태로 표현된다
        assertTrue(viewModel.state.value.isSubmitting)

        // When 요청이 끝난다
        gate.complete(Unit)
        advanceUntilIdle()

        // Then 플래그가 내려간다
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun clickConfirm_rapidTaps_requestsExactlyOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임 변경 요청이 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { changeGlobalNickname(any()) } coAnswers {
            gate.await()
            Result.success(GlobalNickname("라떼"))
        }
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord("라떼"))
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        runCurrent()

        // When 연타로 확인을 한 번 더 누른다
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        runCurrent()

        // Then 중복 요청이 나가지 않는다
        coVerify(exactly = 1) { changeGlobalNickname(any()) }

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun inputWord_afterServerFailure_clearsSubmitError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버 실패로 사유가 붙은 화면
        coEvery { changeGlobalNickname(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel(accountFlow = flowOf(accountOf("모카")))
        advanceUntilIdle()
        viewModel.processIntent(AccountInfoIntent.InputWord("라떼"))
        viewModel.processIntent(AccountInfoIntent.ClickConfirm)
        advanceUntilIdle()

        // When 닉네임을 고친다
        viewModel.processIntent(AccountInfoIntent.InputWord("카페"))

        // Then 표시된 서버 실패 사유가 사라진다
        assertNull(viewModel.state.value.submitError)
    }
}
