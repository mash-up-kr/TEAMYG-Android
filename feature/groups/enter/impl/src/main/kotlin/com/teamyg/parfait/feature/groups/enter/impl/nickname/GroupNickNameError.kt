package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 이 화면이 토스트로 알리는 사유.
 *
 * [NICKNAME_NOT_APPLIED] 를 뺀 나머지는 참여 요청이 서버에서 되돌아온 사유다. 앞 화면의
 * 미리보기를 통과한 뒤에야 나올 수 있어, 미리보기와 참여 사이에 그룹 상태가 바뀐 경우에 해당한다.
 */
enum class GroupNickNameError {
    /** 404 — 초대코드가 가리키는 그룹이 사라졌다 */
    INVALID_INVITE_CODE,

    /** 409 — 이미 참여한 그룹이다 */
    ALREADY_JOINED,

    /** 409 — 미리보기와 참여 사이에 정원이 찼다 */
    MEMBER_LIMIT_REACHED,

    NETWORK,

    UNKNOWN,

    /** 참여는 끝났고 닉네임만 못 붙은 경우 — 참여를 막지 않아 위 갈래와 성격이 다르다 */
    NICKNAME_NOT_APPLIED,
}

@Composable
internal fun GroupNickNameError.toStringResource(): String = when (this) {
    GroupNickNameError.INVALID_INVITE_CODE -> stringResource(R.string.invite_code_error_invalid)
    GroupNickNameError.ALREADY_JOINED -> stringResource(R.string.invite_code_error_already_joined)
    GroupNickNameError.MEMBER_LIMIT_REACHED -> stringResource(R.string.invite_code_error_member_limit_reached)
    GroupNickNameError.NETWORK -> stringResource(R.string.group_nickname_error_network)
    GroupNickNameError.UNKNOWN -> stringResource(R.string.group_nickname_error_unknown)
    GroupNickNameError.NICKNAME_NOT_APPLIED -> stringResource(R.string.group_nickname_error_not_applied)
}
