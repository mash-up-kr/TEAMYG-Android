package com.teamyg.parfait.data.source.policy.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.PolicyService
import com.teamyg.parfait.data.source.policy.mapper.toPolicyVOList
import com.teamyg.parfait.domain.model.policy.PolicyVO
import javax.inject.Inject

class PolicyRemoteDataSourceImpl @Inject constructor(
    private val policyService: PolicyService,
    private val apiCaller: ApiCaller,
) : PolicyRemoteDataSource {
    override suspend fun getPolicies(): Result<List<PolicyVO>> = apiCaller
        .safeApiCall(
            block = { policyService.getPolicies() },
            transform = { it.toPolicyVOList() },
        )
}
