package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param groupNickname 요청한 회원 본인의 그룹 닉네임이다.
 * @param members 탈퇴하지 않은 멤버만 참여 순으로 온다(`docs/api/parfait-group.md`).
 */
@Serializable
data class MyParfaitGroupDetailResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("inviteCode")
    val inviteCode: String,
    @SerialName("memberLimit")
    val memberLimit: Int,
    @SerialName("members")
    val members: List<ParfaitGroupMemberResponse>,
)
