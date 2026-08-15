package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class GetMyGroupsUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(): Result<List<MyParfaitGroupVO>> = parfaitGroupRepository.getMyGroups()
}
