package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.model.request.group.ChangeMyParfaitGroupNicknameRequest
import com.teamyg.parfait.data.service.model.request.group.CreateParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.JoinParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.ReportParfaitGroupRequest
import com.teamyg.parfait.data.source.group.mapper.toCreatedGroupVO
import com.teamyg.parfait.data.source.group.mapper.toGroupId
import com.teamyg.parfait.data.source.group.mapper.toGroupName
import com.teamyg.parfait.data.source.group.mapper.toGroupNicknameVO
import com.teamyg.parfait.data.source.group.mapper.toJoinedGroupVO
import com.teamyg.parfait.data.source.group.mapper.toMyParfaitGroupVO
import com.teamyg.parfait.data.source.group.mapper.toParfaitGroupDetailVO
import com.teamyg.parfait.data.source.group.mapper.toReportedGroupVO
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
import javax.inject.Inject

class ParfaitGroupRemoteDataSourceImpl @Inject constructor(
    private val parfaitGroupService: ParfaitGroupService,
    private val apiCaller: ApiCaller,
) : ParfaitGroupRemoteDataSource {
    override suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>> = apiCaller
        .safeApiCall(
            block = { parfaitGroupService.getParfaitGroups() },
            transform = { responses -> responses.map { it.toMyParfaitGroupVO() } },
        )

    override suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO> = apiCaller
        .safeApiCall(
            block = { parfaitGroupService.getParfaitGroupsByGroupId(groupId.value) },
            transform = { it.toParfaitGroupDetailVO() },
        )

    override suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName> = apiCaller
        .safeApiCall(
            block = { parfaitGroupService.getParfaitGroupsJoinPreview(inviteCode.value) },
            transform = { it.toGroupName() },
        )

    override suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO> = apiCaller
        .safeApiCall(
            block = {
                parfaitGroupService.postParfaitGroupsJoin(
                    request = JoinParfaitGroupRequest(
                        inviteCode = inviteCode.value,
                    ),
                )
            },
            transform = { it.toJoinedGroupVO() },
        )

    override suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO> = apiCaller
        .safeApiCall(
            block = {
                parfaitGroupService.postParfaitGroups(
                    CreateParfaitGroupRequest(
                        groupName = groupName.value,
                        groupNickname = groupNickname.value,
                        memberLimit = memberLimit,
                    ),
                )
            },
            transform = { it.toCreatedGroupVO() },
        )

    override suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO> = apiCaller
        .safeApiCall(
            block = {
                parfaitGroupService.patchParfaitGroupsByGroupIdNickname(
                    groupId = groupId.value,
                    request = ChangeMyParfaitGroupNicknameRequest(
                        groupNickname = groupNickname.value,
                    ),
                )
            },
            transform = { it.toGroupNicknameVO() },
        )

    override suspend fun leaveGroup(groupId: GroupId): Result<GroupId> = apiCaller
        .safeApiCall(
            block = { parfaitGroupService.deleteParfaitGroupsByGroupIdMembersMe(groupId.value) },
            transform = { it.toGroupId() },
        )

    override suspend fun reportGroup(
        groupId: GroupId,
        reason: String,
    ): Result<ReportedGroupVO> = apiCaller
        .safeApiCall(
            block = {
                parfaitGroupService.postParfaitGroupsByGroupIdReports(
                    groupId = groupId.value,
                    request = ReportParfaitGroupRequest(reason = reason),
                )
            },
            transform = { it.toReportedGroupVO() },
        )
}
