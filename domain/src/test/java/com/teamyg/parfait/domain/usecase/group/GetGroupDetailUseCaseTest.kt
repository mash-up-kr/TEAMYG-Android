package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetGroupDetailUseCaseTest {
    private class FakeParfaitGroupRepository(
        private val detailResult: Result<ParfaitGroupDetailVO> = Result.success(DETAIL),
        private val myGroupsResult: Result<List<MyParfaitGroupVO>> = Result.success(listOf(MY_GROUP)),
    ) : ParfaitGroupRepository {
        override suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO> = detailResult

        override suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>> = myGroupsResult

        override suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName> = error("쓰지 않는다")

        override suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO> = error("쓰지 않는다")

        override suspend fun createGroup(
            groupName: GroupName,
            groupNickname: GroupNickname,
            memberLimit: Int,
        ): Result<CreatedGroupVO> = error("쓰지 않는다")

        override suspend fun changeMyNickname(
            groupId: GroupId,
            groupNickname: GroupNickname,
        ): Result<GroupNicknameVO> = error("쓰지 않는다")

        override suspend fun leaveGroup(groupId: GroupId): Result<GroupId> = error("쓰지 않는다")

        override suspend fun reportGroup(
            groupId: GroupId,
            reason: String,
        ): Result<ReportedGroupVO> = error("쓰지 않는다")
    }

    @Test
    fun invoke_bothCallsSucceed_fillsTheNameFromTheList() = runTest {
        // Given 상세와 목록이 모두 온다
        val repository = FakeParfaitGroupRepository()

        // When 그룹 상세 조회
        val detail = GetGroupDetailUseCase(repository)(GroupId(GROUP_ID)).getOrThrow()

        // Then 상세에 없는 그룹명이 목록에서 채워진다
        assertEquals(GroupName(GROUP_NAME), detail.groupName)
        assertEquals(GroupNickname(MY_NICKNAME), detail.myNickname)
        assertEquals(InviteCode(INVITE_CODE), detail.inviteCode)
        assertEquals(2, detail.members.size)
    }

    @Test
    fun invoke_listHasOtherGroupsToo_picksTheAskedOne() = runTest {
        // Given 목록에 다른 그룹이 먼저 있다
        val repository = FakeParfaitGroupRepository(
            myGroupsResult = Result.success(
                listOf(myGroup(id = 99L, name = "다른그룹"), MY_GROUP),
            ),
        )

        // When 그룹 상세 조회
        val detail = GetGroupDetailUseCase(repository)(GroupId(GROUP_ID)).getOrThrow()

        // Then 순서가 아니라 groupId 로 고른다
        assertEquals(GroupName(GROUP_NAME), detail.groupName)
    }

    @Test
    fun invoke_listFails_stillReturnsDetailWithBlankName() = runTest {
        // Given 목록 조회만 실패한다
        val repository = FakeParfaitGroupRepository(myGroupsResult = Result.failure(IOException("네트워크")))

        // When 그룹 상세 조회
        val detail = GetGroupDetailUseCase(repository)(GroupId(GROUP_ID)).getOrThrow()

        // Then 이름 한 줄 때문에 멤버·초대코드까지 막지 않는다
        assertEquals(GroupName(""), detail.groupName)
        assertEquals(InviteCode(INVITE_CODE), detail.inviteCode)
    }

    @Test
    fun invoke_groupMissingFromList_stillReturnsDetailWithBlankName() = runTest {
        // Given 목록에 그 그룹이 없다 — 방금 나갔거나 목록이 낡았다
        val repository = FakeParfaitGroupRepository(myGroupsResult = Result.success(emptyList()))

        // When 그룹 상세 조회
        val detail = GetGroupDetailUseCase(repository)(GroupId(GROUP_ID)).getOrThrow()

        // Then 이름만 비운다
        assertEquals(GroupName(""), detail.groupName)
    }

    @Test
    fun invoke_detailFails_propagatesFailure() = runTest {
        // Given 상세 조회가 실패한다
        val repository = FakeParfaitGroupRepository(detailResult = Result.failure(IOException("네트워크")))

        // When 그룹 상세 조회
        val result = GetGroupDetailUseCase(repository)(GroupId(GROUP_ID))

        // Then 화면에 띄울 것이 없으므로 실패로 남는다
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
    }

    private companion object {
        const val GROUP_ID = 7L
        const val GROUP_NAME = "모카의 파르페"
        const val MY_NICKNAME = "모카"
        const val INVITE_CODE = "WDIDCJ"

        fun myGroup(
            id: Long,
            name: String,
        ) = MyParfaitGroupVO(
            groupId = GroupId(id),
            groupName = GroupName(name),
            recentImageUrl = null,
            recentImageUploadedAt = null,
        )

        val MY_GROUP = myGroup(id = GROUP_ID, name = GROUP_NAME)

        val DETAIL = ParfaitGroupDetailVO(
            groupId = GroupId(GROUP_ID),
            groupNickname = GroupNickname(MY_NICKNAME),
            inviteCode = InviteCode(INVITE_CODE),
            members = listOf(
                ParfaitGroupMemberVO(MemberId(1L), GroupNickname(MY_NICKNAME)),
                ParfaitGroupMemberVO(MemberId(2L), GroupNickname("체리마루")),
            ),
        )
    }
}
