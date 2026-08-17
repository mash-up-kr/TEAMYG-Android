package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class LeaveGroupUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(groupId: GroupId): Result<GroupId> = parfaitGroupRepository.leaveGroup(groupId)
}
