package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.notice.local.NoticeLocalDataSource
import com.teamyg.parfait.data.source.notice.local.NoticeReadLocalDataSource
import com.teamyg.parfait.data.source.notice.remote.NoticeRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.model.notice.NoticeVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoticeRepositoryImplTest {
    private val remote: NoticeRemoteDataSource = mockk()
    private val local: NoticeLocalDataSource = mockk(relaxed = true)
    private val readLocal: NoticeReadLocalDataSource = mockk(relaxed = true)
    private val account = MutableStateFlow<MyAccountVO?>(ACCOUNT)
    private val userInfo: UserInfoLocalDataSource = mockk {
        every { myAccount } returns account
    }
    private val repository get() = NoticeRepositoryImpl(remote, local, readLocal, userInfo)

    private val notice = NoticeVO(
        id = 1L,
        title = "공지",
        publishedDate = LocalDate(2026, 11, 17),
        content = "본문",
    )

    @Test
    fun refreshNotices_success_savesToCache() = runTest {
        // Given 서버가 공지를 준다
        coEvery { remote.getNotices() } returns Result.success(listOf(notice))

        // When 갱신한다
        val result = repository.refreshNotices()

        // Then 캐시에 저장하고 성공이다
        assertTrue(result.isSuccess)
        verify(exactly = 1) { local.saveNotices(listOf(notice)) }
    }

    @Test
    fun refreshNotices_failure_keepsExistingCache() = runTest {
        // Given 서버 호출이 네트워크로 실패한다
        coEvery { remote.getNotices() } returns Result.failure(ApiException.Network(IOException("down")))

        // When 갱신한다
        val result = repository.refreshNotices()

        // Then 기존 캐시를 덮어쓰지 않고 실패를 전달한다
        assertTrue(result.isFailure)
        verify(exactly = 0) { local.saveNotices(any()) }
    }

    @Test
    fun refreshNotices_failure_mapsToAppError() = runTest {
        // Given 서버 호출이 네트워크로 실패한다
        coEvery { remote.getNotices() } returns Result.failure(ApiException.Network(IOException("down")))

        // When 갱신한다
        val error = repository.refreshNotices().exceptionOrNull()

        // Then 화면 쪽이 다루는 AppError 로 바뀐다
        assertIs<AppError>(error)
    }

    @Test
    fun notices_exposesLocalCacheFlow() = runTest {
        // Given 캐시에 공지가 있다 — Repository 가 만들어질 때 캐시 Flow 를 잡으므로 스텁을 먼저 건다
        every { local.notices } returns MutableStateFlow(listOf(notice))

        // Then 그 값을 그대로 구독한다
        assertEquals(listOf(notice), repository.notices.first())
    }

    @Test
    fun readNoticeIds_withAccount_exposesThatAccountsIds() = runTest {
        // Given 계정 1 의 읽음 저장소에 id 가 있다
        every { readLocal.readNoticeIds(MemberId(1L)) } returns flowOf(setOf(1L, 2L))

        // Then 그 값을 그대로 구독한다
        assertEquals(setOf(1L, 2L), repository.readNoticeIds.first())
    }

    @Test
    fun readNoticeIds_withoutAccount_isEmptySet() = runTest {
        // Given 계정이 없다
        account.value = null

        // Then 빈 집합이다
        assertEquals(emptySet(), repository.readNoticeIds.first())
    }

    @Test
    fun readNoticeIds_accountSwitch_readsNewAccountKey() = runTest {
        // Given 계정마다 읽은 기록이 다르다
        every { readLocal.readNoticeIds(MemberId(1L)) } returns flowOf(setOf(1L))
        every { readLocal.readNoticeIds(MemberId(2L)) } returns flowOf(setOf(9L))
        val repository = repository
        val first = repository.readNoticeIds.first()

        // When 다른 계정으로 바뀐다
        account.value = OTHER_ACCOUNT
        val second = repository.readNoticeIds.first()

        // Then 이전 계정의 기록이 아니라 새 계정의 기록을 본다
        assertEquals(setOf(1L), first)
        assertEquals(setOf(9L), second)
    }

    @Test
    fun readNoticeIds_accountSwitch_dropsThePreviousAccountsStream() = runTest {
        // Given 끝나지 않는 두 계정의 읽음 흐름을 한 구독으로 보고 있다 — 실제 DataStore 흐름처럼
        val previous = MutableStateFlow(setOf(1L))
        every { readLocal.readNoticeIds(MemberId(1L)) } returns previous
        every { readLocal.readNoticeIds(MemberId(2L)) } returns MutableStateFlow(setOf(9L))
        val repository = repository
        val emissions = mutableListOf<Set<Long>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.readNoticeIds.toList(emissions)
        }

        // When 다른 계정으로 바뀐 뒤 이전 계정의 흐름이 값을 더 낸다
        account.value = OTHER_ACCOUNT
        previous.value = setOf(1L, 2L)

        // Then 이전 계정의 방출은 새 계정 화면에 섞이지 않는다
        assertEquals(listOf(setOf(1L), setOf(9L)), emissions)
    }

    @Test
    fun readNoticeIds_signOutThenSameAccount_restoresRecords() = runTest {
        // Given 계정 1 의 기록이 저장돼 있고 구독 중이다
        every { readLocal.readNoticeIds(MemberId(1L)) } returns flowOf(setOf(1L))
        val repository = repository
        val emissions = mutableListOf<Set<Long>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.readNoticeIds.toList(emissions)
        }

        // When 로그아웃(계정 없음)했다가 같은 계정으로 다시 들어온다
        account.value = null
        account.value = ACCOUNT

        // Then 로그아웃 동안은 비고, 다시 들어오면 같은 계정의 기록이 돌아온다
        assertEquals(listOf(setOf(1L), emptySet(), setOf(1L)), emissions)
    }

    @Test
    fun markRead_withAccount_delegatesWithMemberId() = runTest {
        // When 읽음 처리한다
        repository.markRead(3L)

        // Then 현재 계정으로 읽음 저장소에 맡긴다
        coVerify(exactly = 1) { readLocal.markRead(MemberId(1L), 3L) }
    }

    @Test
    fun markRead_withoutAccount_writesNothing() = runTest {
        // Given 계정이 없다
        account.value = null

        // When 읽음 처리한다
        repository.markRead(3L)

        // Then 귀속할 계정이 없어 아무것도 쓰지 않는다
        coVerify(exactly = 0) { readLocal.markRead(any(), any()) }
    }

    @Test
    fun clearReadNotices_clearsCurrentAccountOnly() = runTest {
        // When 읽음 기록을 지운다
        repository.clearReadNotices()

        // Then 현재 계정의 기록만 지운다
        coVerify(exactly = 1) { readLocal.clear(MemberId(1L)) }
    }

    @Test
    fun clearReadNotices_withoutAccount_doesNothing() = runTest {
        // Given 계정이 없다
        account.value = null

        // When 읽음 기록을 지운다
        repository.clearReadNotices()

        // Then 지울 키를 알 수 없어 아무것도 하지 않는다
        coVerify(exactly = 0) { readLocal.clear(any()) }
    }

    @Test
    fun clearNotices_clearsCacheButNotReadIds() = runTest {
        // When 세션이 끝나 캐시를 비운다
        repository.clearNotices()

        // Then 캐시만 비우고 읽음 기록은 지우지 않는다 — 같은 계정으로 다시 들어오면 이어 쓴다
        verify(exactly = 1) { local.clear() }
        coVerify(exactly = 0) { readLocal.clear(any()) }
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
