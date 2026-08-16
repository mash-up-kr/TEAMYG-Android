package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupDetailVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class GetGroupDetailUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    /**
     * 그룹 상세에 그룹명이 없어 목록에서 이름만 따로 집어 붙인다. 서버가 상세에 groupName 을
     * 실어 주면 두 번째 호출을 걷어낸다.
     *
     * 이름 조회 실패는 실패로 치지 않는다 — 이름 한 줄 때문에 멤버·초대코드까지 못 보여 주는
     * 것보다, 이름을 비우고 나머지를 띄우는 편이 낫다.
     */
    suspend operator fun invoke(groupId: GroupId): Result<GroupDetailVO> {
        val detail = parfaitGroupRepository
            .getGroupDetail(groupId)
            .getOrElse { throwable -> return Result.failure(throwable) }

        val groupName = parfaitGroupRepository
            .getMyGroups()
            .getOrNull()
            ?.firstOrNull { group -> group.groupId == groupId }
            ?.groupName
            ?: GroupName("")

        return Result.success(
            GroupDetailVO(
                groupId = detail.groupId,
                groupName = groupName,
                myNickname = detail.groupNickname,
                inviteCode = detail.inviteCode,
                members = detail.members,
            ),
        )
    }
}
