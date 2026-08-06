package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TermsAgreementRequest(
    @SerialName("termsId")
    val termsId: Long,
    @SerialName("agreed")
    val agreed: Boolean,
)
