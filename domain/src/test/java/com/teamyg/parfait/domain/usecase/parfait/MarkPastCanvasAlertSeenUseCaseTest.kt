package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class MarkPastCanvasAlertSeenUseCaseTest {
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
    private val pastCanvasAlertRepository: PastCanvasAlertRepository = mockk(relaxed = true)
    private val useCase = MarkPastCanvasAlertSeenUseCase(memberRepository, pastCanvasAlertRepository)

    @Test
    fun invoke_withAccount_recordsUnderThatMember() = runTest {
        // When 그룹 5 의 마감일을 봤다고 기록한다
        useCase(GroupId(5L), LocalDate(2026, 8, 20))

        // Then 현재 계정으로 저장한다
        coVerify(exactly = 1) {
            pastCanvasAlertRepository.markSeen(MemberId(1L), GroupId(5L), LocalDate(2026, 8, 20))
        }
    }

    @Test
    fun invoke_withoutAccount_skipsWrite() = runTest {
        // Given 계정 정보가 없다
        account.value = null

        // When 기록을 시도한다
        useCase(GroupId(5L), LocalDate(2026, 8, 20))

        // Then 엉뚱한 계정에 귀속시키지 않도록 쓰지 않는다
        coVerify(exactly = 0) { pastCanvasAlertRepository.markSeen(any(), any(), any()) }
    }
}
