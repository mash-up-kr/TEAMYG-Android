package com.teamyg.parfait.domain.model.auth

sealed interface KakaoLoginVO {
    data class ExistingMember(val session: AuthSessionVO) : KakaoLoginVO

    data class NewUser(val registrationToken: RegistrationToken) : KakaoLoginVO
}
