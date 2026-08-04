package com.teamyg.parfait.data.source.group.mapper

import com.teamyg.parfait.data.service.model.response.group.ChangeMyParfaitGroupNicknameResponse
import com.teamyg.parfait.data.service.model.response.group.CreateParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.JoinParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.LeaveParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupDetailResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.ParfaitGroupMemberResponse
import com.teamyg.parfait.data.service.model.response.group.PreviewParfaitGroupJoinResponse
import com.teamyg.parfait.data.service.model.response.group.ReportParfaitGroupResponse
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
import com.teamyg.parfait.domain.model.id.ReportId
import kotlinx.datetime.LocalDateTime

internal fun MyParfaitGroupResponse.toMyParfaitGroupVO(): MyParfaitGroupVO = MyParfaitGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    recentImageUrl = recentImageUrl,
    recentImageUploadedAt = recentImageUploadedAt?.let(LocalDateTime::parse),
)

internal fun MyParfaitGroupDetailResponse.toParfaitGroupDetailVO(): ParfaitGroupDetailVO = ParfaitGroupDetailVO(
    groupId = GroupId(groupId),
    groupNickname = GroupNickname(groupNickname),
    inviteCode = InviteCode(inviteCode),
    members = members.map { it.toParfaitGroupMemberVO() },
)

internal fun ParfaitGroupMemberResponse.toParfaitGroupMemberVO(): ParfaitGroupMemberVO = ParfaitGroupMemberVO(
    memberId = MemberId(memberId),
    groupNickname = GroupNickname(groupNickname),
)

internal fun JoinParfaitGroupResponse.toJoinedGroupVO(): JoinedGroupVO = JoinedGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
)

internal fun CreateParfaitGroupResponse.toCreatedGroupVO(): CreatedGroupVO = CreatedGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    inviteCode = InviteCode(inviteCode),
    memberLimit = memberLimit,
)

internal fun ChangeMyParfaitGroupNicknameResponse.toGroupNicknameVO(): GroupNicknameVO = GroupNicknameVO(
    groupId = GroupId(groupId),
    groupNickname = GroupNickname(groupNickname),
)

internal fun ReportParfaitGroupResponse.toReportedGroupVO(): ReportedGroupVO = ReportedGroupVO(
    groupId = GroupId(groupId),
    reportId = ReportId(reportId),
)

internal fun PreviewParfaitGroupJoinResponse.toGroupName(): GroupName = GroupName(groupName)

internal fun LeaveParfaitGroupResponse.toGroupId(): GroupId = GroupId(groupId)
