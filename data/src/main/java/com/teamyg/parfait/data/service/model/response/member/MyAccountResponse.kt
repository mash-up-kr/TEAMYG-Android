package com.teamyg.parfait.data.service.model.response.member

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param provider LoginProvider 이름 문자열. 매퍼가 enum 으로 바꾸며 모르는 값은 UNKNOWN 이다.
 */
@Serializable
data class MyAccountResponse(
    @SerialName("memberId")
    val memberId: Long,
    @SerialName("provider")
    val provider: String,
    @SerialName("nickname")
    val nickname: String,
)
