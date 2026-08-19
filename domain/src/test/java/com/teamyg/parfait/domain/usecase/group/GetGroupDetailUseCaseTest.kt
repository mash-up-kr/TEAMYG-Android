package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetGroupDetailUseCaseTest {
    private val repository: ParfaitGroupRepository = mockk()

    private val groupId = GroupId(1L)

    private val detail = ParfaitGroupDetailVO(
        groupId = groupId,
        groupName = GroupName("모카의 파르페"),
        groupNickname = GroupNickname("모카"),
        inviteCode = InviteCode("ABCDEF"),
        memberLimit = 12,
        members = listOf(
            ParfaitGroupMemberVO(
                memberId = MemberId(42L),
                groupNickname = GroupNickname("모카"),
                nametagChip = NametagChipType.TYPE3,
            ),
        ),
    )

    @Test
    fun invoke_detailCached_emitsItWithoutReadingTheGroupList() = runTest {
        // Given 상세 캐시에만 값이 있고 목록 캐시는 비어 있다
        every { repository.groupDetail(groupId) } returns MutableStateFlow(detail)

        // When 상세를 구독한다
        val emitted = GetGroupDetailUseCase(repository)(groupId).first()

        // Then 그룹명까지 상세 하나에서 나온다 — 목록을 한 번 더 읽지 않는다
        assertEquals(GroupName("모카의 파르페"), emitted?.groupName)
        assertEquals(12, emitted?.memberLimit)
        assertEquals(NametagChipType.TYPE3, emitted?.members?.single()?.nametagChip)
    }

    @Test
    fun invoke_detailNotCachedYet_emitsNull() = runTest {
        // Given 아직 상세를 한 번도 받지 못했다
        every { repository.groupDetail(groupId) } returns MutableStateFlow(null)

        // When 상세를 구독한다
        val emitted = GetGroupDetailUseCase(repository)(groupId).first()

        // Then 미조회는 null 로 나온다 — 화면이 로딩과 빈 값을 가른다
        assertNull(emitted)
    }
}
