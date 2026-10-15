package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param inviteCode ASCII 영숫자 6자다. 서버가 대문자로 정규화해 조회하므로 소문자도 통한다.
 *   형식 위반과 없는 코드가 같은 404 INVALID_INVITE_CODE 로 나간다(`docs/api/parfait-group.md` 초대코드 형식).
 */
@Serializable
data class JoinParfaitGroupRequest(
    @SerialName("inviteCode")
    val inviteCode: String,
)
