package com.teamyg.parfait.data.source.auth.mapper

import com.teamyg.parfait.data.service.model.request.auth.TermsAgreementRequest
import com.teamyg.parfait.data.service.model.response.auth.KakaoLoginResponse
import com.teamyg.parfait.data.service.model.response.auth.ReissueResponse
import com.teamyg.parfait.data.service.model.response.auth.SignupResponse
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import kotlin.time.Duration.Companion.seconds

internal fun KakaoLoginResponse.toKakaoLoginVO(): KakaoLoginVO = when (isNewUser) {
    true -> KakaoLoginVO.NewUser(
        registrationToken = RegistrationToken(
            requireNotNull(registrationToken) { "newUser=true 인데 registrationToken 이 없다" },
        ),
    )

    false -> KakaoLoginVO.ExistingMember(
        session = AuthSessionVO(
            accessToken = AccessToken(
                requireNotNull(accessToken) { "newUser=false 인데 accessToken 이 없다" },
            ),
            refreshToken = RefreshToken(
                requireNotNull(refreshToken) { "newUser=false 인데 refreshToken 이 없다" },
            ),
            expiresIn = requireNotNull(expiresIn) { "newUser=false 인데 expiresIn 이 없다" }.seconds,
        ),
    )
}

internal fun SignupResponse.toAuthSessionVO(): AuthSessionVO = AuthSessionVO(
    accessToken = AccessToken(accessToken),
    refreshToken = RefreshToken(refreshToken),
    expiresIn = expiresIn.seconds,
)

internal fun ReissueResponse.toAuthSessionVO(): AuthSessionVO = AuthSessionVO(
    accessToken = AccessToken(accessToken),
    refreshToken = RefreshToken(refreshToken),
    expiresIn = expiresIn.seconds,
)

internal fun TermsAgreement.toRequest(): TermsAgreementRequest = TermsAgreementRequest(
    termsId = termsId.value,
    agreed = agreed,
)
