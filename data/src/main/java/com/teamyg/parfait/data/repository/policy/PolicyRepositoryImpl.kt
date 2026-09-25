package com.teamyg.parfait.data.repository.policy

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSource
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import javax.inject.Inject

class PolicyRepositoryImpl @Inject constructor(
    private val policyRemoteDataSource: PolicyRemoteDataSource,
) : PolicyRepository {
    override suspend fun getPolicies(): Result<List<PolicyVO>> = policyRemoteDataSource
        .getPolicies()
        .mapErrorToAppError()
}
