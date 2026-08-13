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

    suspend operator fun invoke(): Result<List<PolicyVO>> = policyRepository.getPolicies()
}
