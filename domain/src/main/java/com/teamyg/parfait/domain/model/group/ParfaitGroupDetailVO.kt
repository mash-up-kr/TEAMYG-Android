package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

/**
 * 그룹 상세. 서버 응답 하나에 1:1 로 대응한다.
 *
 * @param groupNickname **인증 회원 본인**이 이 그룹에서 쓰는 이름이다. 전역 닉네임과 별개이고,
 *  [members] 안의 내 항목과 같은 값이다
 * @param memberLimit 그룹 정원(1~12). 생성 이후 바뀌지 않는다
 * @param members 탈퇴하지 않은 멤버만, 참여 순
 */
data class ParfaitGroupDetailVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val groupNickname: GroupNickname,
    val inviteCode: InviteCode,
    val memberLimit: Int,
    val members: List<ParfaitGroupMemberVO>,
)
