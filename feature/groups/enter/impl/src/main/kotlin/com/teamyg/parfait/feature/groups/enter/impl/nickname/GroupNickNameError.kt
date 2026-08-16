package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 참여 요청이 서버에서 되돌아온 사유.
 *
 * 앞 화면의 미리보기를 통과한 뒤에야 나올 수 있어, 미리보기와 참여 사이에 그룹 상태가
 * 바뀐 경우에 해당한다. 닉네임 적용 실패는 참여를 막지 않으므로 여기에 없다.
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
}

@Composable
internal fun GroupNickNameError.toStringResource(): String = when (this) {
    GroupNickNameError.INVALID_INVITE_CODE -> stringResource(R.string.invite_code_error_invalid)
    GroupNickNameError.ALREADY_JOINED -> stringResource(R.string.invite_code_error_already_joined)
    GroupNickNameError.MEMBER_LIMIT_REACHED -> stringResource(R.string.invite_code_error_member_limit_reached)
    GroupNickNameError.NETWORK -> stringResource(R.string.group_nickname_error_network)
    GroupNickNameError.UNKNOWN -> stringResource(R.string.group_nickname_error_unknown)
}
