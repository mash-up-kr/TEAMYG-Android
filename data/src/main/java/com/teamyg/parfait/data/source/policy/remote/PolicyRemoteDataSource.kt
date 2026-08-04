package com.teamyg.parfait.data.source.policy.remote

import com.teamyg.parfait.domain.model.policy.PolicyVO

interface PolicyRemoteDataSource {
    suspend fun getPolicies(): Result<List<PolicyVO>>
}
