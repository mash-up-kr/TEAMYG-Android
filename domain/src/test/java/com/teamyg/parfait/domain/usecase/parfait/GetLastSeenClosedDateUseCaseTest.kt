package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
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

class GetLastSeenClosedDateUseCaseTest {
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
    private val pastCanvasAlertRepository: PastCanvasAlertRepository = mockk()
    private val useCase = GetLastSeenClosedDateUseCase(memberRepository, pastCanvasAlertRepository)

    @Test
    fun invoke_withAccount_readsUnderThatMember() = runTest {
        // Given 현재 계정의 기록이 있다
        coEvery {
            pastCanvasAlertRepository.lastSeenClosedDate(MemberId(1L), GroupId(5L))
        } returns LocalDate(2026, 8, 20)

        // Then 현재 계정으로 읽는다
        assertEquals(LocalDate(2026, 8, 20), useCase(GroupId(5L)))
    }

    @Test
    fun invoke_withoutAccount_isNullWithoutReading() = runTest {
        // Given 계정 정보가 없다
        account.value = null

        // Then 기준선만 세우는 경로(null)로 가고 저장소는 읽지 않는다
        assertNull(useCase(GroupId(5L)))
        coVerify(exactly = 0) { pastCanvasAlertRepository.lastSeenClosedDate(any(), any()) }
    }
}
