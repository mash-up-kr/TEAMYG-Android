package com.teamyg.parfait.data.service.model.response.appversion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param updateType 서버 enum 이름 문자열. enum 으로 받으면 서버가 값을 추가할 때 역직렬화가 통째로 실패해서 문자열로 받는다.
 */
@Serializable
data class AppVersionCheckResponse(
    @SerialName("updateType")
    val updateType: String,
    @SerialName("minimumVersion")
    val minimumVersion: String,
    @SerialName("latestVersion")
    val latestVersion: String,
    @SerialName("storeUrl")
    val storeUrl: String? = null,
)
