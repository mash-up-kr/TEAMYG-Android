package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    @SerialName("registrationToken")
    val registrationToken: String,
    @SerialName("agreements")
    val agreements: List<TermsAgreementRequest>,
)
