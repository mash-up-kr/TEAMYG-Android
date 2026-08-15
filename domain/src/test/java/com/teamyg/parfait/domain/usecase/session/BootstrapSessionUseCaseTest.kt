package com.teamyg.parfait.domain.usecase.session

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BootstrapSessionUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val memberRepository: MemberRepository = mockk(relaxed = true)
    private val bootstrap = BootstrapSessionUseCase(authRepository, memberRepository)

    @Test
    fun invoke_noToken_goesToLoginWithoutServerCall() = runTest {
        // Given 저장된 토큰이 없다
        coEvery { authRepository.hasSession() } returns false

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 서버를 부르지 않고 로그인으로 간다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 0) { memberRepository.refreshMyAccount() }
    }

    @Test
    fun invoke_tokenAndRefreshSucceeds_goesToGroupList() = runTest {
        // Given 토큰이 있고 조회가 성공한다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns Result.success(ACCOUNT)

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 그룹 목록으로 간다 — SSoT 가 채워진 상태다
        assertEquals(SessionBootstrap.ToGroupList, result)
    }

    @Test
    fun invoke_tokenButRefreshFailsWithNetwork_goesToLoginAndClearsNothing() = runTest {
        // Given 토큰은 있으나 네트워크 실패로 조회가 실패한다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(AppError.Network(cause = null))

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 로그인으로 보내되, 세션은 살아있다고 가정하고 아무것도 지우지 않는다 —
        // 연결이 돌아온 뒤 다시 켜면 자동로그인이 성립해야 한다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 0) { authRepository.logout() }
        coVerify(exactly = 0) { memberRepository.clearMyAccount() }
    }

    @Test
    fun invoke_tokenButRefreshFailsWithNonNetworkError_clearsSessionAndGoesToLogin() = runTest {
        // Given 토큰은 있으나 서버 거절 등 네트워크 외 사유로 조회가 실패한다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(AppError.Server(code = "UNAUTHORIZED", statusCode = 401, serverMessage = "만료"))
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { memberRepository.clearMyAccount() } returns Unit

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 세션이 확실히 죽었다고 보고 토큰·계정 정보를 모두 지운 뒤 로그인으로 보낸다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }

    private companion object {
        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )
    }
}
