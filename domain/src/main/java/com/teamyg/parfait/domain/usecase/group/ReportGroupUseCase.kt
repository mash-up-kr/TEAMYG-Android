package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class ReportGroupUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        reason: String,
    ): Result<ReportedGroupVO> = parfaitGroupRepository.reportGroup(
        groupId = groupId,
        reason = reason,
    )
}
