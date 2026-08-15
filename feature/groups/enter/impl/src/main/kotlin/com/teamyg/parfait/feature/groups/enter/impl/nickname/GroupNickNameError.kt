package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 닉네임 적용 요청이 서버에서 되돌아온 사유.
 *
 * 입력 형식 자체가 틀린 경우는 요청 전에 `CheckNameValidUseCase` 가 걸러
 * `NameValidResult.Error` 로 표시하므로, 여기에는 서버만 알 수 있는 사유가 남는다.
 */
enum class GroupNickNameError {
    /** 400 — 서버 닉네임 규칙에 걸렸다. 앱 검증과 서버 규칙이 어긋났다는 신호다 */
    INVALID,

    NETWORK,

    UNKNOWN,
}

@Composable
internal fun GroupNickNameError.toStringResource(): String = when (this) {
    GroupNickNameError.INVALID -> stringResource(R.string.group_nickname_error_invalid)
    GroupNickNameError.NETWORK -> stringResource(R.string.group_nickname_error_network)
    GroupNickNameError.UNKNOWN -> stringResource(R.string.group_nickname_error_unknown)
}
