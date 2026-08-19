package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WithdrawUseCaseTest {
    private val memberRepository: MemberRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val withdraw = WithdrawUseCase(
        memberRepository = memberRepository,
        logout = LogoutUseCase(authRepository, memberRepository),
    )

    @Test
    fun invoke_serverAccepts_clearsTheDevice() = runTest {
        // Given 서버가 탈퇴를 받아 준다
        coEvery { memberRepository.withdraw() } returns Result.success(Unit)
        coEvery { authRepository.logout() } returns Result.success(Unit)

        // When 탈퇴한다
        val result = withdraw()

        // Then 토큰과 계정 정보를 지운다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }

    @Test
    fun invoke_serverRejects_leavesTheDeviceSignedIn() = runTest {
        // Given 탈퇴 요청이 실패한다
        coEvery { memberRepository.withdraw() } returns Result.failure(AppError.Network(cause = null))

        // When 탈퇴한다
        val result = withdraw()

        // Then 계정이 살아 있으므로 아무것도 지우지 않는다 — 지우면 사용자는 탈퇴했다고
        // 믿는데 계정은 남는다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { authRepository.logout() }
        coVerify(exactly = 0) { memberRepository.clearMyAccount() }
    }
}
