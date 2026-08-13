package com.teamyg.parfait.data.repository.policy

import com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSource
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyRepositoryImpl
@Inject
constructor(
    private val policyRemoteDataSource: PolicyRemoteDataSource,
) : PolicyRepository {
    init {
        repositoryLogger.i { "PolicyRepositoryImpl::init" }
    }

    override suspend fun getPolicies(): Result<List<PolicyVO>> = policyRemoteDataSource.getPolicies()
}
