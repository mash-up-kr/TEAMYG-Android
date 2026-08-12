package com.teamyg.parfait.data.service.model.request.member

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param nickname 빈 문자열은 서버 @NotBlank 에 걸려 400 INVALID_REQUEST 이고,
 *   형식 위반(연속 공백·허용 밖 문자·16자 이상)은 400 INVALID_NICKNAME 이다.
 *   코드가 갈리므로 소비 측이 두 실패를 같은 것으로 뭉개지 않도록 주의한다.
 */
@Serializable
data class ChangeGlobalNicknameRequest(
    @SerialName("nickname")
    val nickname: String,
)
