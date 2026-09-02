package com.teamyg.parfait.domain.model

/**
 * 카카오 **SDK** 로그인 결과.
 *
 * 서버 로그인 응답은 [com.teamyg.parfait.domain.model.auth.KakaoLoginVO] 다 — 이름이 닮았지만
 * 다른 것이다. 이쪽은 SDK 가 준 ID 토큰, 저쪽은 우리 서버가 준 세션/가입 토큰이다.
 */
sealed interface KakaoLoginResult {
    /**
     * @param idToken 서버 `POST /api/v1/auth/kakao` 에 보낼 ID 토큰
     * @param nonce SDK 요청에 넘긴 값. **서버 요청에도 같은 값을 보내야 한다**
     */
    data class Success(
        val idToken: String,
        val nonce: String,
    ) : KakaoLoginResult

    data class Cancel(val throwable: Throwable?) : KakaoLoginResult

    data class Failure(val throwable: Throwable?) : KakaoLoginResult
}
