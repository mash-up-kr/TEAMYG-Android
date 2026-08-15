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
     * @param registrationToken 카카오 로그인에서 신규 사용자로 판별될 때 발급된 토큰
     */
    suspend fun signUp(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO>

    /** 발급받은 세션을 암호화 저장소에 넣는다 */
    suspend fun saveSession(session: AuthSessionVO)

    /**
     * 서버 세션을 끊고 로컬 토큰을 지운다.
     *
     * **서버 호출이 실패해도 로컬은 정리하고 성공을 반환한다** — 사용자가 로그아웃을 눌렀으면
     * 이 기기에서는 나가는 것이 기대 동작이고, 서버 세션 정리 실패는 로그로 남긴다.
     */
    suspend fun logout(): Result<Unit>
}
