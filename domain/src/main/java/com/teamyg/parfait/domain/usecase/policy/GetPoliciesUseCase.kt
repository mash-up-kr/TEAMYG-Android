package com.teamyg.parfait.domain.usecase.policy

import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import javax.inject.Inject

/** 화면에 노출할 약관 목록을 서버 순서 그대로 가져온다 */
class GetPoliciesUseCase @Inject constructor(
    private val policyRepository: PolicyRepository,
) {
    suspend operator fun invoke(): Result<List<PolicyVO>> = policyRepository.getPolicies()
}
