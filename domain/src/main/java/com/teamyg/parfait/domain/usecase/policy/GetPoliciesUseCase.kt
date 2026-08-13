package com.teamyg.parfait.domain.usecase.policy

import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import javax.inject.Inject

class GetPoliciesUseCase
@Inject
constructor(
    private val policyRepository: PolicyRepository,
) {
    init {
        useCaseLogger.i { "GetPoliciesUseCase::init" }
    }

    /**
     * 서버 응답 순서는 보장되지 않으므로 필수 약관이 앞에 오도록 정렬한다.
     */
    suspend operator fun invoke(): Result<List<PolicyVO>> = policyRepository
        .getPolicies()
        .map { policies -> policies.sortedByDescending(PolicyVO::required) }
}
