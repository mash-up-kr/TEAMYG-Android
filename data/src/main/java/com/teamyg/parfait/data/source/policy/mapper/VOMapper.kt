package com.teamyg.parfait.data.source.policy.mapper

import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO

internal fun PolicyResponse.toPolicyVOList(): List<PolicyVO> = policies.map { it.toPolicyVO() }

internal fun PolicyItemResponse.toPolicyVO(): PolicyVO = PolicyVO(
    termsId = TermsId(termsId),
    type = type.toPolicyType(),
    title = title,
    url = url,
    required = required,
)

private fun String.toPolicyType(): PolicyType = when (this) {
    PolicyType.TERMS_OF_SERVICE.name -> PolicyType.TERMS_OF_SERVICE
    PolicyType.PRIVACY_POLICY.name -> PolicyType.PRIVACY_POLICY
    else -> PolicyType.UNKNOWN
}
