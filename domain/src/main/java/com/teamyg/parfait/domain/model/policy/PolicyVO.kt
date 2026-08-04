package com.teamyg.parfait.domain.model.policy

import com.teamyg.parfait.domain.model.id.TermsId

data class PolicyVO(
    val termsId: TermsId,
    val type: PolicyType,
    val title: String,
    val url: String,
    val required: Boolean,
)
