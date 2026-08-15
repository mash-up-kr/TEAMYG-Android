package com.teamyg.parfait.domain.repository.policy

import com.teamyg.parfait.domain.model.policy.PolicyVO

interface PolicyRepository {
    suspend fun getPolicies(): Result<List<PolicyVO>>
}
