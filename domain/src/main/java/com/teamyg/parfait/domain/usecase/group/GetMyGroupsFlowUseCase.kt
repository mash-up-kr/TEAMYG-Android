package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 캐시된 내 그룹 목록을 구독한다. 조회는 [RefreshMyGroupsUseCase] 가 따로 부른다 */
class GetMyGroupsFlowUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    operator fun invoke(): Flow<List<MyParfaitGroupVO>?> = parfaitGroupRepository.myGroups
}
