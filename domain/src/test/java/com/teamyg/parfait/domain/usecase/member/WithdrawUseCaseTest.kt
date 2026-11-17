package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WithdrawUseCaseTest {
    private val account = MutableStateFlow<MyAccountVO?>(ACCOUNT)
    private val memberRepository: MemberRepository = mockk(relaxed = true) {
        every { myAccount } returns account
    }
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val parfaitGroupRepository: ParfaitGroupRepository = mockk(relaxed = true)
    private val parfaitRepository: ParfaitRepository = mockk(relaxed = true)
    private val noticeRepository: NoticeRepository = mockk(relaxed = true)
    private val withdraw = WithdrawUseCase(
        memberRepository = memberRepository,
        noticeRepository = noticeRepository,
        logout = LogoutUseCase(
            authRepository,
            memberRepository,
            parfaitGroupRepository,
            parfaitRepository,
            noticeRepository,
        ),
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
        coVerify(exactly = 0) { noticeRepository.clearReadNotices(any()) }
    }

    @Test
    fun invoke_serverAccepts_clearsReadNoticesBeforeLogout() = runTest {
        // Given 서버가 탈퇴를 받아 준다
        coEvery { memberRepository.withdraw() } returns Result.success(Unit)

        // When 탈퇴한다
        withdraw()

        // Then 로그아웃이 계정 정보를 비우기 전에 그 계정의 읽음 기록을 지운다 — 순서가
        // 바뀌면 지울 키를 알 수 없다
        coVerifyOrder {
            noticeRepository.clearReadNotices(MemberId(1L))
            authRepository.logout()
            memberRepository.clearMyAccount()
        }
    }

    @Test
    fun invoke_clearReadNoticesFails_stillLogsOut() = runTest {
        // Given 서버는 탈퇴를 받았지만 읽음 기록 삭제가 실패한다
        coEvery { memberRepository.withdraw() } returns Result.success(Unit)
        coEvery { noticeRepository.clearReadNotices(any()) } throws IllegalStateException("datastore")

        // When 탈퇴한다
        val result = withdraw()

        // Then 탈퇴는 성공이고 기기는 로그아웃된다 — 삭제 실패가 탈퇴한 계정을 로그인 상태로 두면 안 된다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }

    @Test
    fun invoke_noAccount_skipsClearingButStillLogsOut() = runTest {
        // Given 서버는 탈퇴를 받았지만 계정 정보가 없어 어느 기록인지 모른다
        coEvery { memberRepository.withdraw() } returns Result.success(Unit)
        account.value = null

        // When 탈퇴한다
        val result = withdraw()

        // Then 지울 대상이 없어 건너뛰고 로그아웃은 진행한다
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { noticeRepository.clearReadNotices(any()) }
        coVerify(exactly = 1) { authRepository.logout() }
    }

    private companion object {
        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )
    }
}
