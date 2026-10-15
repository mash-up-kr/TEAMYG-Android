package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param nonce 앱이 로그인 직전에 만들어 카카오 SDK 요청과 이 API 에 같은 값을 보낸다. 서버는 해시하지 않고
 *   ID 토큰의 nonce 클레임과 그대로 비교하며, 어긋나면 401 INVALID_ID_TOKEN 이다(`docs/api/auth.md`).
 */
@Serializable
data class KakaoLoginRequest(
    @SerialName("idToken")
    val idToken: String,
    @SerialName("nonce")
    val nonce: String,
)
