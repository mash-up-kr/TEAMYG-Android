package com.teamyg.parfait.domain.model.auth

import com.teamyg.parfait.domain.model.id.TermsId

data class TermsAgreement(
    val termsId: TermsId,
    val agreed: Boolean,
)
