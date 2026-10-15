package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param termsId GET /api/v1/policies 가 준 값을 그대로 보낸다. 하드코딩하면 약관이 개정될 때
 *   400 TERMS_NOT_FOUND 이다.
 */
@Serializable
data class TermsAgreementRequest(
    @SerialName("termsId")
    val termsId: Long,
    @SerialName("agreed")
    val agreed: Boolean,
)
