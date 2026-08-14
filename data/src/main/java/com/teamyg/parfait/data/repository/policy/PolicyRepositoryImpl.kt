package com.teamyg.parfait.data.repository.policy

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSource
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import javax.inject.Inject

/**
 * 약관 목록 조회.
 *
 * 실패 원인을 여기서 [com.teamyg.parfait.domain.model.error.AppError] 로 바꾼다 —
 * 이 경계가 있어야 feature 모듈이 `:data` 의 `ApiException` 을 보지 않는다.
 */
class PolicyRepositoryImpl @Inject constructor(
    private val policyRemoteDataSource: PolicyRemoteDataSource,
) : PolicyRepository {
    override suspend fun getPolicies(): Result<List<PolicyVO>> = policyRemoteDataSource
        .getPolicies()
        .mapErrorToAppError()
}
