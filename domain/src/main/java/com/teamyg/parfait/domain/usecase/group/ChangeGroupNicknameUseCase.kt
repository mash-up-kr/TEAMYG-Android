package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class ChangeGroupNicknameUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO> = parfaitGroupRepository.changeMyNickname(
        groupId = groupId,
        groupNickname = groupNickname,
    )
}
