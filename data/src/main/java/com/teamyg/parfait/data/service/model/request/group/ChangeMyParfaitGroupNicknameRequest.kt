package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param groupNickname 1~15자, 완성형 한글·자모 단독·영문·숫자와 단어 사이 공백 1개만 받는다.
 *   위반은 400 INVALID_GROUP_NICKNAME 이다. 같은 그룹 안 중복은 허용된다(`docs/api/parfait-group.md`).
 */
@Serializable
data class ChangeMyParfaitGroupNicknameRequest(
    @SerialName("groupNickname")
    val groupNickname: String,
)
