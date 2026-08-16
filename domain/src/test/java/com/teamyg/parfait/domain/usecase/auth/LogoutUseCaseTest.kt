package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogoutUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val memberRepository: MemberRepository = mockk(relaxed = true)
    private val logout = LogoutUseCase(authRepository, memberRepository)

    @Test
    fun invoke_always_clearsTokensAndAccount() = runTest {
        // Given 로그인 상태
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { memberRepository.clearMyAccount() } returns Unit

        // When 로그아웃한다
        logout()

        // Then 토큰과 계정 정보가 둘 다 지워진다 — 하나만 지우면 계정 전환 시 이전
        // 사용자 정보가 남는다
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }
}
