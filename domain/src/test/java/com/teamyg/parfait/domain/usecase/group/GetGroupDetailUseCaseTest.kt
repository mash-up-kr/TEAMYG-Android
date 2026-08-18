package com.teamyg.parfait.domain.usecase.group

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetGroupDetailUseCaseTest {
    private val repository: ParfaitGroupRepository = mockk()

    @Test
    fun invoke_listCacheEmpty_stillEmitsDetailWithBlankName() = runTest {
        // Given 상세는 있는데 목록 캐시가 비어 있다
        every { repository.groupDetail(GROUP_ID) } returns flowOf(DETAIL)
        every { repository.myGroups } returns flowOf(null)

        // When 상세를 구독한다
        GetGroupDetailUseCase(repository).invoke(GROUP_ID).test {
            // Then 이름만 비고 나머지는 보인다 — 이름 한 줄 때문에 멤버·초대코드를 가리지 않는다
            val detail = awaitItem()
            assertEquals(GroupName(""), detail?.groupName)
            assertEquals(DETAIL.inviteCode, detail?.inviteCode)
            assertEquals(DETAIL.members, detail?.members)
            awaitComplete()
        }
    }

    @Test
    fun invoke_listCacheArrivesLater_emitsNameOnly() = runTest {
        // Given 상세는 이미 있고 목록은 나중에 채워진다
        val groups = MutableStateFlow<List<MyParfaitGroupVO>?>(null)
        every { repository.groupDetail(GROUP_ID) } returns flowOf(DETAIL)
        every { repository.myGroups } returns groups

        GetGroupDetailUseCase(repository).invoke(GROUP_ID).test {
            assertEquals(GroupName(""), awaitItem()?.groupName)

            // When 목록 캐시가 채워진다
            groups.value = listOf(GROUP)

            // Then 이름이 붙어 다시 나온다
            assertEquals(GroupName("아메리카노"), awaitItem()?.groupName)
        }
    }

    @Test
    fun invoke_detailNotCached_emitsNull() = runTest {
        // Given 상세를 아직 받지 못했다
        every { repository.groupDetail(GROUP_ID) } returns flowOf(null)
        every { repository.myGroups } returns flowOf(listOf(GROUP))

        // When 구독한다
        GetGroupDetailUseCase(repository).invoke(GROUP_ID).test {
            // Then 보여 줄 것이 없다
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    private companion object {
        val GROUP_ID = GroupId(1L)

        val GROUP = MyParfaitGroupVO(
            groupId = GROUP_ID,
            groupName = GroupName("아메리카노"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = null,
        )

        val DETAIL = ParfaitGroupDetailVO(
            groupId = GROUP_ID,
            groupName = GroupName("모카의 파르페"),
            groupNickname = GroupNickname("모카"),
            inviteCode = InviteCode("ABC123"),
            memberLimit = 12,
            members = listOf(
                ParfaitGroupMemberVO(
                    memberId = MemberId(10L),
                    groupNickname = GroupNickname("모카"),
                    nametagChip = NametagChipType.TYPE1,
                ),
            ),
        )
    }
}
