package com.teamyg.parfait.data.service.model.request.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param token 같은 token 으로 다시 보내면 새 행을 만들지 않고 기존 행의 회원·세션·플랫폼을 덮어쓴다(upsert).
 * @param platform String 이지만 서버가 받는 값은 IOS·ANDROID 뿐이고 그 밖은 400 INVALID_REQUEST 이다.
 */
@Serializable
data class RegisterDeviceTokenRequest(
    @SerialName("token")
    val token: String,
    @SerialName("platform")
    val platform: String,
)
