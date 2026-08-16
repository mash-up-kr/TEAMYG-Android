package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

/**
 * 그룹 하나를 한 덩어리로 본 모습. 서버 응답 하나에 대응하지 않는다 —
 * 그룹명이 상세에 없어 목록에서 따로 집어 붙인다
 * ([com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCase]).
 *
 * @param myNickname 이 그룹 안에서 쓰는 내 이름. 전역 닉네임과 별개다
 */
data class GroupDetailVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val myNickname: GroupNickname,
    val inviteCode: InviteCode,
    val members: List<ParfaitGroupMemberVO>,
)
