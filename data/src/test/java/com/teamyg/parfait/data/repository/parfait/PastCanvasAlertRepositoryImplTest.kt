package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.PastCanvasAlertLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PastCanvasAlertRepositoryImplTest {
    private val local: PastCanvasAlertLocalDataSource = mockk(relaxed = true)
    private val account = MutableStateFlow<MyAccountVO?>(ACCOUNT)
    private val userInfo: UserInfoLocalDataSource = mockk {
        every { myAccount } returns account
    }
    private val repository = PastCanvasAlertRepositoryImpl(local, userInfo)

    @Test
    fun lastSeenClosedDate_withAccount_readsUnderThatMemberId() = runTest {
        // Given 현재 계정의 기록이 있다
        coEvery { local.lastSeenClosedDate(MemberId(1L), GroupId(5L)) } returns LocalDate(2026, 8, 20)

        // Then 현재 계정으로 읽는다
        assertEquals(LocalDate(2026, 8, 20), repository.lastSeenClosedDate(GroupId(5L)))
    }

    @Test
    fun lastSeenClosedDate_accountSwitched_readsTheNewAccount() = runTest {
        // Given 계정 1 의 기록만 있다
        coEvery { local.lastSeenClosedDate(MemberId(1L), GroupId(5L)) } returns LocalDate(2026, 8, 20)
        coEvery { local.lastSeenClosedDate(MemberId(2L), GroupId(5L)) } returns null

        // When 다른 계정으로 바뀐다
        account.value = OTHER_ACCOUNT

        // Then 이전 계정의 기록을 물려받지 않는다
        assertNull(repository.lastSeenClosedDate(GroupId(5L)))
    }

    @Test
    fun lastSeenClosedDate_noAccount_isNullWithoutReading() = runTest {
        // Given 계정 정보가 없다
        account.value = null

        // Then 기준선만 세우는 경로(null)로 가고 저장소는 읽지 않는다
        assertNull(repository.lastSeenClosedDate(GroupId(5L)))
        coVerify(exactly = 0) { local.lastSeenClosedDate(any(), any()) }
    }

    @Test
    fun markSeen_withAccount_writesUnderThatMemberId() = runTest {
        // When 마감일을 확인했다고 기록한다
        repository.markSeen(GroupId(5L), LocalDate(2026, 8, 20))

        // Then 현재 계정으로 저장한다
        coVerify(exactly = 1) { local.markSeenClosedDate(MemberId(1L), GroupId(5L), LocalDate(2026, 8, 20)) }
    }

    @Test
    fun markSeen_noAccount_skipsWrite() = runTest {
        // Given 계정 정보가 없다
        account.value = null

        // When 기록을 시도한다
        repository.markSeen(GroupId(5L), LocalDate(2026, 8, 20))

        // Then 엉뚱한 계정에 귀속시키지 않도록 쓰지 않는다
        coVerify(exactly = 0) { local.markSeenClosedDate(any(), any(), any()) }
    }

    private companion object {
        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )
        val OTHER_ACCOUNT = MyAccountVO(
            memberId = MemberId(2L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("라떼"),
        )
    }
}
