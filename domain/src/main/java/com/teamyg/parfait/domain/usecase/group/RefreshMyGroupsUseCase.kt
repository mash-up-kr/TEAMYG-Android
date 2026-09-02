package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class RefreshMyGroupsUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(): Result<Unit> = parfaitGroupRepository.refreshMyGroups()
}
