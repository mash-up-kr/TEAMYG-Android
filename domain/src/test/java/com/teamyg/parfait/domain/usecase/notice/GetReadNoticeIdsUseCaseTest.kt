package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetReadNoticeIdsUseCaseTest {
    private val account = MutableStateFlow<MyAccountVO?>(ACCOUNT)
    private val memberRepository: MemberRepository = mockk {
        every { myAccount } returns account
    }
    private val noticeRepository: NoticeRepository = mockk()
    private val useCase = GetReadNoticeIdsUseCase(memberRepository, noticeRepository)

    @Test
    fun invoke_withAccount_exposesThatAccountsIds() = runTest {
        // Given 계정 1 의 읽음 저장소에 id 가 있다
        every { noticeRepository.readNoticeIds(MemberId(1L)) } returns flowOf(setOf(1L, 2L))

        // Then 그 값을 그대로 구독한다
        assertEquals(setOf(1L, 2L), useCase().first())
    }

    @Test
    fun invoke_withoutAccount_isEmptySet() = runTest {
        // Given 계정이 없다
        account.value = null

        // Then 저장소를 읽지 않고 빈 집합이다
        assertEquals(emptySet(), useCase().first())
    }

    @Test
    fun invoke_accountSwitch_readsNewAccountKey() = runTest {
        // Given 계정마다 읽은 기록이 다르다
        every { noticeRepository.readNoticeIds(MemberId(1L)) } returns flowOf(setOf(1L))
        every { noticeRepository.readNoticeIds(MemberId(2L)) } returns flowOf(setOf(9L))
        val first = useCase().first()

        // When 다른 계정으로 바뀐다
        account.value = OTHER_ACCOUNT
        val second = useCase().first()

        // Then 이전 계정의 기록이 아니라 새 계정의 기록을 본다
        assertEquals(setOf(1L), first)
        assertEquals(setOf(9L), second)
    }

    @Test
    fun invoke_accountSwitch_dropsThePreviousAccountsStream() = runTest {
        // Given 끝나지 않는 두 계정의 읽음 흐름을 한 구독으로 보고 있다 — 실제 DataStore 흐름처럼
        val previous = MutableStateFlow(setOf(1L))
        every { noticeRepository.readNoticeIds(MemberId(1L)) } returns previous
        every { noticeRepository.readNoticeIds(MemberId(2L)) } returns MutableStateFlow(setOf(9L))
        val emissions = mutableListOf<Set<Long>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().toList(emissions)
        }

        // When 다른 계정으로 바뀐 뒤 이전 계정의 흐름이 값을 더 낸다
        account.value = OTHER_ACCOUNT
        previous.value = setOf(1L, 2L)

        // Then 이전 계정의 방출은 새 계정 화면에 섞이지 않는다
        assertEquals(listOf(setOf(1L), setOf(9L)), emissions)
    }

    @Test
    fun invoke_signOutThenSameAccount_restoresRecords() = runTest {
        // Given 계정 1 의 기록이 저장돼 있고 구독 중이다
        every { noticeRepository.readNoticeIds(MemberId(1L)) } returns flowOf(setOf(1L))
        val emissions = mutableListOf<Set<Long>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().toList(emissions)
        }

        // When 로그아웃(계정 없음)했다가 같은 계정으로 다시 들어온다
        account.value = null
        account.value = ACCOUNT

        // Then 로그아웃 동안은 비고, 다시 들어오면 같은 계정의 기록이 돌아온다
        assertEquals(listOf(setOf(1L), emptySet(), setOf(1L)), emissions)
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
