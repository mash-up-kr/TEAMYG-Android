package com.teamyg.parfait.domain.repository.auth

import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO

interface AuthRepository {
    /**
     * @param idToken 카카오 SDK 가 발급한 **ID 토큰**(access token 이 아니다)
     * @param nonce SDK 요청에 넘긴 것과 **같은 값**
     */
    suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO>

    /** 발급받은 세션을 암호화 저장소에 넣는다 */
    suspend fun saveSession(session: AuthSessionVO)
}
