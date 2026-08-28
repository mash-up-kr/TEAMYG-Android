package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogoutUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val memberRepository: MemberRepository = mockk(relaxed = true)
    private val parfaitGroupRepository: ParfaitGroupRepository = mockk(relaxed = true)
    private val parfaitRepository: ParfaitRepository = mockk(relaxed = true)
    private val logout = LogoutUseCase(authRepository, memberRepository, parfaitGroupRepository, parfaitRepository)

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

    @Test
    fun invoke_clearsTokenAccountAndGroups() = runTest {
        // Given 로그아웃이 성공한다
        coEvery { authRepository.logout() } returns Result.success(Unit)

        // When 로그아웃한다
        LogoutUseCase(authRepository, memberRepository, parfaitGroupRepository, parfaitRepository).invoke()

        // Then 계정 정보와 그룹 캐시를 함께 지운다 — 하나만 남으면 계정 전환 때 이전 사용자
        // 흔적이 남는다
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
        verify(exactly = 1) { parfaitGroupRepository.clearGroups() }
    }

    @Test
    fun invoke_clearsTheCanvasCacheToo() = runTest {
        logout()

        verify { parfaitRepository.clearTodayCanvas() }
    }

    @Test
    fun invoke_clearsInMemoryCachesBeforeTheAccountStore() = runTest {
        logout()

        coVerifyOrder {
            parfaitGroupRepository.clearGroups()
            parfaitRepository.clearTodayCanvas()
            memberRepository.clearMyAccount()
        }
    }
}
