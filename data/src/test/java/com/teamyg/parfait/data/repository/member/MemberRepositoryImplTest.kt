package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.member.remote.MemberRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemberRepositoryImplTest {
    private val remoteDataSource: MemberRemoteDataSource = mockk()

    /**
     * [MemberRepositoryImpl.myAccount] 가 생성자에서 곧바로 `localDataSource.myAccount` 를
     * 읽으므로, 기본 스텁이 없으면 strict mock 이 생성 시점에 MockKException 을 던진다.
     * 각 테스트는 필요하면 `every { localDataSource.myAccount } returns ...` 로 덮어쓴다.
     */
    private val localDataSource: UserInfoLocalDataSource = mockk {
        every { myAccount } returns flowOf(null)
    }
    private val repository = MemberRepositoryImpl(remoteDataSource, localDataSource)

    @Test
    fun refreshMyAccount_succeeds_savesToLocal() = runTest {
        // Given 서버가 계정 정보를 준다
        coEvery { remoteDataSource.getMyAccount() } returns Result.success(ACCOUNT)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 갱신한다
        val result = repository.refreshMyAccount()

        // Then 반환값과 저장값이 같다
        assertEquals(ACCOUNT, result.getOrNull())
        coVerify(exactly = 1) { localDataSource.save(ACCOUNT) }
    }

    @Test
    fun refreshMyAccount_fails_keepsLocalUntouched() = runTest {
        // Given 서버 조회가 실패한다
        coEvery { remoteDataSource.getMyAccount() } returns
            Result.failure(ApiException.Network(IOException("연결 실패")))

        // When 갱신한다
        val result = repository.refreshMyAccount()

        // Then 저장소를 건드리지 않는다 — 낡은 값이라도 지우지 않는다.
        // 지우면 오프라인에서 화면이 빈다
        assertTrue(result.isFailure)
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { localDataSource.save(any()) }
        coVerify(exactly = 0) { localDataSource.clear() }
    }

    @Test
    fun changeGlobalNickname_succeeds_updatesOnlyNickname() = runTest {
        // Given 저장된 계정 정보가 있고 서버가 새 닉네임을 확인해 준다
        every { localDataSource.myAccount } returns flowOf(ACCOUNT)
        coEvery { remoteDataSource.changeGlobalNickname(NEW_NICKNAME) } returns
            Result.success(NEW_NICKNAME)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 닉네임을 바꾼다
        val result = repository.changeGlobalNickname(NEW_NICKNAME)

        // Then 닉네임만 바뀌고 memberId·provider 는 그대로다
        assertEquals(NEW_NICKNAME, result.getOrNull())
        coVerify(exactly = 1) {
            localDataSource.save(ACCOUNT.copy(nickname = NEW_NICKNAME))
        }
    }

    @Test
    fun changeGlobalNickname_fails_leavesLocalUntouched() = runTest {
        // Given 서버가 거절한다
        every { localDataSource.myAccount } returns flowOf(ACCOUNT)
        coEvery { remoteDataSource.changeGlobalNickname(any()) } returns
            Result.failure(
                ApiException.Business(
                    code = "INVALID_NICKNAME",
                    statusCode = 400,
                    serverMessage = "…",
                    errorDetail = null,
                ),
            )

        // When 닉네임을 바꾼다
        val result = repository.changeGlobalNickname(NEW_NICKNAME)

        // Then 낙관적 갱신을 하지 않는다 — 실패했는데 다른 화면에 새 이름이 보이면 안 된다
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { localDataSource.save(any()) }
    }

    private companion object {
        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )
        val NEW_NICKNAME = GlobalNickname("새모카")
    }
}
