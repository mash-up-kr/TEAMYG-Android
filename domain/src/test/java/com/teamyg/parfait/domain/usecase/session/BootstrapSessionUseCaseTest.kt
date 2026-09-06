package com.teamyg.parfait.domain.usecase.session

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.notification.DeviceTokenRegistrar
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BootstrapSessionUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val memberRepository: MemberRepository = mockk(relaxed = true)
    private val parfaitGroupRepository: ParfaitGroupRepository = mockk(relaxed = true)
    private val parfaitRepository: ParfaitRepository = mockk(relaxed = true)
    private val deviceTokenRegistrar: DeviceTokenRegistrar = mockk(relaxed = true)

    // 정리 경로는 실물 LogoutUseCase 를 통과시킨다 — mock 으로 바꾸면 "정리를 위임했다"만
    // 검증되고 정작 무엇이 지워지는지는 이 테스트가 놓친다.
    private val bootstrap = BootstrapSessionUseCase(
        authRepository = authRepository,
        memberRepository = memberRepository,
        logout = LogoutUseCase(authRepository, memberRepository, parfaitGroupRepository, parfaitRepository),
        deviceTokenRegistrar = deviceTokenRegistrar,
    )

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
    fun invoke_tokenButRefreshFailsWithHttp401_clearsSessionAndGoesToLogin() = runTest {
        // Given 토큰은 있으나 서버가 401 로 인증을 거절해 조회가 실패한다
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

    @Test
    fun invoke_tokenButRefreshFailsWithMemberNotFound_clearsSessionAndGoesToLogin() = runTest {
        // Given 토큰은 유효해 보이지만 서버가 그 회원을 찾지 못한다(탈퇴·강제 삭제 등) —
        // 상태코드는 401 이 아니라 404 다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(
                AppError.Server(
                    code = ServerErrorCode.Member.MEMBER_NOT_FOUND,
                    statusCode = 404,
                    serverMessage = "회원 없음",
                ),
            )
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { memberRepository.clearMyAccount() } returns Unit

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 상태코드가 401 이 아니어도 이 코드 자체가 인증 거절을 뜻하므로 세션을 지운다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }

    @Test
    fun invoke_tokenButRefreshFailsWithServerError_goesToLoginAndClearsNothing() = runTest {
        // Given 토큰은 있으나 서버가 5xx 로 실패해 envelope 밖 실패(AppError.Unexpected)로 온다 —
        // 배포 중 순단·서버 장애 상황이지 자격증명이 죽은 게 아니다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(AppError.Unexpected(cause = RuntimeException("500")))

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 로그인으로 보내되, 인증 거절이 아니므로 아무것도 지우지 않는다 — 매 배포마다
        // 돌아온 사용자가 로그아웃되면 안 된다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 0) { authRepository.logout() }
        coVerify(exactly = 0) { memberRepository.clearMyAccount() }
    }

    @Test
    fun invoke_tokenButRefreshFailsWithNon401Server_goesToLoginAndClearsNothing() = runTest {
        // Given 로컬 저장 실패 등 인증과 무관한 사유가 AppError.Server 로 표면화됐지만
        // statusCode·code 어느 쪽도 인증 거절을 뜻하지 않는다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(
                AppError.Server(code = "SOME_OTHER_ERROR", statusCode = 500, serverMessage = "서버 오류"),
            )

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 인증 거절이 아니므로 아무것도 지우지 않는다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 0) { authRepository.logout() }
        coVerify(exactly = 0) { memberRepository.clearMyAccount() }
    }

    private companion object {
        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )
    }

    @Test
    fun invoke_sessionAlive_registersDeviceToken() = runTest {
        // Given 토큰이 있고 조회가 성공한다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns Result.success(ACCOUNT)

        // When 부트스트랩한다
        bootstrap()

        // Then 이 세션의 기기 토큰을 등록한다 — 등록 유실을 앱 진입마다 메우는 자리다
        verify(exactly = 1) { deviceTokenRegistrar.register() }
    }

    @Test
    fun invoke_refreshFails_doesNotRegisterDeviceToken() = runTest {
        // Given 토큰은 있으나 조회가 실패한다 — 세션이 살아있다고 볼 수 없다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(AppError.Network(cause = null))

        // When 부트스트랩한다
        bootstrap()

        // Then 등록은 성공 분기에서만 돈다
        verify(exactly = 0) { deviceTokenRegistrar.register() }
    }

    @Test
    fun invoke_noToken_doesNotRegisterDeviceToken() = runTest {
        // Given 저장된 토큰이 없다
        coEvery { authRepository.hasSession() } returns false

        // When 부트스트랩한다
        bootstrap()

        // Then 인증이 필요한 등록을 부르지 않는다
        verify(exactly = 0) { deviceTokenRegistrar.register() }
    }
}
