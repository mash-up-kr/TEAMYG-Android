package com.teamyg.parfait.domain.model.auth

/**
 * 서버 `POST /api/v1/auth/kakao` 응답.
 *
 * 카카오 **SDK** 로그인 결과는 [com.teamyg.parfait.domain.model.KakaoLoginResult] 다 —
 * 이름이 닮았지만 다른 것이다.
 */
sealed interface KakaoLoginVO {
    data class ExistingMember(val session: AuthSessionVO) : KakaoLoginVO

    data class NewUser(val registrationToken: RegistrationToken) : KakaoLoginVO
}
