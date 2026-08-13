package com.teamyg.parfait.domain.repository.auth

import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement

interface AuthRepository {
    /**
     * @param idToken 카카오 SDK 가 발급한 **ID 토큰**(access token 이 아니다)
     * @param nonce SDK 요청에 넘긴 것과 **같은 값**
     */
    suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO>

    /**
     * 약관 동의 내역과 함께 회원 가입을 완료하고 로그인 세션을 받는다.
     *
     * @param registrationToken 카카오 로그인에서 신규 사용자로 판별될 때 발급된 토큰
     */
    suspend fun signUp(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO>

    /** 발급받은 세션을 암호화 저장소에 넣는다 */
    suspend fun saveSession(session: AuthSessionVO)
}
