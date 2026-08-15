package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 참여 확인 팝업에서 나간 요청이 서버에서 되돌아온 사유. 참여와 닉네임 적용을 잇달아
 * 보내므로 두 요청의 사유가 함께 들어온다.
 *
 * 입력 형식 자체가 틀린 경우는 요청 전에 `CheckNameValidUseCase` 가 걸러
 * `NameValidResult.Error` 로 표시하므로, 여기에는 서버만 알 수 있는 사유가 남는다.
 */
enum class GroupNickNameError {
    /** 400 — 서버 닉네임 규칙에 걸렸다. 앱 검증과 서버 규칙이 어긋났다는 신호다 */
    INVALID,

    /** 404 — 초대코드가 가리키는 그룹이 사라졌다. 앞 화면의 미리보기 이후에 바뀐 경우다 */
    INVALID_INVITE_CODE,

    /** 409 — 이미 참여한 그룹이다 */
    ALREADY_JOINED,

    /** 409 — 미리보기와 참여 사이에 정원이 찼다 */
    MEMBER_LIMIT_REACHED,

    NETWORK,

    UNKNOWN,
}

/** 참여 쪽 사유는 초대코드 화면과 같은 상황이라 문구도 같이 쓴다 */
@Composable
internal fun GroupNickNameError.toStringResource(): String = when (this) {
    GroupNickNameError.INVALID -> stringResource(R.string.group_nickname_error_invalid)
    GroupNickNameError.INVALID_INVITE_CODE -> stringResource(R.string.invite_code_error_invalid)
    GroupNickNameError.ALREADY_JOINED -> stringResource(R.string.invite_code_error_already_joined)
    GroupNickNameError.MEMBER_LIMIT_REACHED -> stringResource(R.string.invite_code_error_member_limit_reached)
    GroupNickNameError.NETWORK -> stringResource(R.string.group_nickname_error_network)
    GroupNickNameError.UNKNOWN -> stringResource(R.string.group_nickname_error_unknown)
}
