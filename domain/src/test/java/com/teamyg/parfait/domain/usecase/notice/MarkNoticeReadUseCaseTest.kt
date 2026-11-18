package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MarkNoticeReadUseCaseTest {
    private val account = MutableStateFlow<MyAccountVO?>(
        MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        ),
    )
    private val memberRepository: MemberRepository = mockk {
        every { myAccount } returns account
    }
    private val noticeRepository: NoticeRepository = mockk(relaxed = true)
    private val useCase = MarkNoticeReadUseCase(memberRepository, noticeRepository)

    @Test
    fun invoke_withAccount_recordsUnderThatMember() = runTest {
        // When 공지 3 을 읽음 처리한다
        useCase(3L)

        // Then 현재 계정으로 기록한다
        coVerify(exactly = 1) { noticeRepository.markRead(MemberId(1L), 3L) }
    }

    @Test
    fun invoke_withoutAccount_writesNothing() = runTest {
        // Given 계정이 없다
        account.value = null

        // When 읽음 처리한다
        useCase(3L)

        // Then 엉뚱한 계정에 귀속시키느니 기록하지 않는다
        coVerify(exactly = 0) { noticeRepository.markRead(any(), any()) }
    }
}
