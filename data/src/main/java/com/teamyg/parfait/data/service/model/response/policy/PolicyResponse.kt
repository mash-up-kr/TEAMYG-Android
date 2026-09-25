package com.teamyg.parfait.data.service.model.response.policy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param policies 약관 종류당 최대 1건이라 0~2개이고, 빈 배열도 200 정상 응답이다. 순서는 서버가
 *   TERMS_OF_SERVICE → PRIVACY_POLICY 로 고정한다(`docs/api/policy.md`).
 */
@Serializable
data class PolicyResponse(
    @SerialName("policies")
    val policies: List<PolicyItemResponse>,
)
