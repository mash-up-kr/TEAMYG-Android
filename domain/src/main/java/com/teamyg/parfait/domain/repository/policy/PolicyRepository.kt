package com.teamyg.parfait.domain.repository.policy

import com.teamyg.parfait.domain.model.policy.PolicyVO

interface PolicyRepository {
    /**
     * 가입 시 동의해야 하는 약관 목록을 가져온다.
     */
    suspend fun getPolicies(): Result<List<PolicyVO>>
}
