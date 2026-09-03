package com.teamyg.parfait.data.service.model.request.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param platform String 이지만 서버가 받는 값은 IOS·ANDROID 뿐이고 그 밖은 400 이다.
 */
@Serializable
data class RegisterDeviceTokenRequest(
    @SerialName("token")
    val token: String,
    @SerialName("platform")
    val platform: String,
)
