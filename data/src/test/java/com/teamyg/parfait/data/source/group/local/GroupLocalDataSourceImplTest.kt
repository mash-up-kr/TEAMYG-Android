package com.teamyg.parfait.data.source.group.local

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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupLocalDataSourceImplTest {
    private val dataSource = GroupLocalDataSourceImpl()

    @Test
    fun myGroups_beforeAnySave_isNull() {
        // Given/When 아무것도 저장하지 않았다
        // Then 미조회는 null 이다 — 0건과 구분돼야 빈 화면과 로딩이 갈린다
        assertNull(dataSource.myGroups.value)
    }

    @Test
    fun saveMyGroups_withEmptyList_isEmptyNotNull() {
        // Given/When 서버가 그룹 0건을 줬다
        dataSource.saveMyGroups(emptyList())

        // Then 미조회가 아니라 0건이다
        assertEquals(emptyList(), dataSource.myGroups.value)
    }

    @Test
    fun removeGroup_dropsFromListAndDetail() = runTest {
        // Given 목록과 상세가 모두 캐시에 있다
        dataSource.saveMyGroups(listOf(GROUP_A, GROUP_B))
        dataSource.saveGroupDetail(DETAIL_A)

        // When 그룹 A 를 지운다
        dataSource.removeGroup(GROUP_ID_A)

        // Then 목록에서도 상세에서도 사라진다 — 한쪽만 지우면 나간 그룹의 상세가 남는다
        assertEquals(listOf(GROUP_B), dataSource.myGroups.value)
        dataSource.groupDetail(GROUP_ID_A).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun groupDetail_otherGroupSaved_doesNotReemit() = runTest {
        // Given A 의 상세를 구독하고 있다
        dataSource.saveGroupDetail(DETAIL_A)

        dataSource.groupDetail(GROUP_ID_A).test {
            assertEquals(DETAIL_A, awaitItem())

            // When 다른 그룹의 상세가 저장된다
            dataSource.saveGroupDetail(DETAIL_B)

            // Then A 구독자는 흔들리지 않는다
            expectNoEvents()
        }
    }

    @Test
    fun clear_resetsToUnloaded() = runTest {
        // Given 목록과 상세가 차 있다
        dataSource.saveMyGroups(listOf(GROUP_A))
        dataSource.saveGroupDetail(DETAIL_A)

        // When 세션이 끝난다
        dataSource.clear()

        // Then 미조회로 되돌아간다 — 계정이 바뀌어도 이전 그룹이 남지 않는다
        assertNull(dataSource.myGroups.value)
        dataSource.groupDetail(GROUP_ID_A).test {
            assertNull(awaitItem())
        }
    }

    private companion object {
        val GROUP_ID_A = GroupId(1L)
        val GROUP_ID_B = GroupId(2L)

        val GROUP_A = MyParfaitGroupVO(
            groupId = GROUP_ID_A,
            groupName = GroupName("아메리카노"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = null,
        )
        val GROUP_B = MyParfaitGroupVO(
            groupId = GROUP_ID_B,
            groupName = GroupName("라떼"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = null,
        )

        val DETAIL_A = ParfaitGroupDetailVO(
            groupId = GROUP_ID_A,
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
        val DETAIL_B = DETAIL_A.copy(groupId = GROUP_ID_B)
    }
}
