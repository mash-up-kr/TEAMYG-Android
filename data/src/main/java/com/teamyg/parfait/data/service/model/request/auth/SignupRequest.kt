package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param agreements 서버 검증 순서대로 termsId 중복은 400 DUPLICATE_TERMS_ID, 현재 유효 약관에 없는 id 는
 *   400 TERMS_NOT_FOUND, 필수 약관 미동의는 400 REQUIRED_TERMS_NOT_AGREED 이다(`docs/api/auth.md`).
 */
@Serializable
data class SignupRequest(
    @SerialName("registrationToken")
    val registrationToken: String,
    @SerialName("agreements")
    val agreements: List<TermsAgreementRequest>,
)
