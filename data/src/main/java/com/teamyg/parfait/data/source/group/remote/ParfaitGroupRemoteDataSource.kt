package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId

interface ParfaitGroupRemoteDataSource {
    suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>>

    suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO>

    suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName>

    suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO>

    suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO>

    suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO>

    suspend fun leaveGroup(groupId: GroupId): Result<GroupId>

    suspend fun reportGroup(
        groupId: GroupId,
        reason: String,
    ): Result<ReportedGroupVO>
}
