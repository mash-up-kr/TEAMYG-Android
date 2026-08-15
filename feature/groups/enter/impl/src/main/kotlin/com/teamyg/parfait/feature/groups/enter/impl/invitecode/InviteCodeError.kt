package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 초대코드 입력 자리에 한 줄로 붙는 실패 사유.
 *
 * ViewModel 이 문자열 대신 이 타입을 들고, 문구 선택은 [toStringResource] 가 한다 —
 * ViewModel 이 안드로이드 리소스를 보지 않아야 테스트에서 그대로 검증할 수 있다.
 */
enum class InviteCodeError {
    /** 404 — 초대코드에 해당하는 그룹이 없다 */
    INVALID_CODE,

    /** 409 — 이미 참여한 그룹이다 */
    ALREADY_JOINED,

    /** 409 — 그룹 정원이 찼다 */
    MEMBER_LIMIT_REACHED,

    NETWORK,

    UNKNOWN,
}

@Composable
internal fun InviteCodeError.toStringResource(): String = when (this) {
    InviteCodeError.INVALID_CODE -> stringResource(R.string.invite_code_error_invalid)
    InviteCodeError.ALREADY_JOINED -> stringResource(R.string.invite_code_error_already_joined)
    InviteCodeError.MEMBER_LIMIT_REACHED -> stringResource(R.string.invite_code_error_member_limit_reached)
    InviteCodeError.NETWORK -> stringResource(R.string.invite_code_error_network)
    InviteCodeError.UNKNOWN -> stringResource(R.string.invite_code_error_unknown)
}
