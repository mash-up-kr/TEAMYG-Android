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
import com.teamyg.parfait.data.source.common.mapper.toNametagChipType
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
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
import kotlinx.datetime.toInstant

internal fun MyParfaitGroupResponse.toMyParfaitGroupVO(): MyParfaitGroupVO = MyParfaitGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    recentImageUrl = recentImageUrl,
    // 서버는 오프셋 없는 로컬 날짜시각을 주고 그 벽시계는 KST다(api/parfait-group.md 타임존 절).
    // 오프셋을 안 붙이면 기기 타임존에 따라 다른 시점이 된다.
    recentImageUploadedAt = recentImageUploadedAt
        ?.let(LocalDateTime::parse)
        ?.toInstant(PARFAIT_TIME_ZONE),
    lastPlacedByNametagChip = lastPlacedByNameTagChip.toNametagChipType(),
)

internal fun MyParfaitGroupDetailResponse.toParfaitGroupDetailVO(): ParfaitGroupDetailVO = ParfaitGroupDetailVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    groupNickname = GroupNickname(groupNickname),
    inviteCode = InviteCode(inviteCode),
    memberLimit = memberLimit,
    members = members.map { it.toParfaitGroupMemberVO() },
)

internal fun ParfaitGroupMemberResponse.toParfaitGroupMemberVO(): ParfaitGroupMemberVO = ParfaitGroupMemberVO(
    memberId = MemberId(memberId),
    groupNickname = GroupNickname(groupNickname),
    nametagChip = nameTagChip.toNametagChipType(),
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
