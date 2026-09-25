package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param groupName 1~10자, 완성형 한글·자모 단독·영문·숫자와 단어 사이 공백 1개만 받는다.
 *   위반은 400 INVALID_GROUP_NAME 이다(`docs/api/parfait-group.md` 정책 대조 메모).
 * @param groupNickname groupName 과 같은 문자 규칙에 1~15자다. 위반은 400 INVALID_GROUP_NICKNAME 이다.
 * @param memberLimit 1~12 만 받는다. 위반은 400 INVALID_GROUP_MEMBER_LIMIT 이다.
 */
@Serializable
data class CreateParfaitGroupRequest(
    @SerialName("groupName")
    val groupName: String,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("memberLimit")
    val memberLimit: Int,
)
