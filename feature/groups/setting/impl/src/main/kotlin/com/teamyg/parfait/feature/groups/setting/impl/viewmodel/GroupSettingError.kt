package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.setting.impl.R

/**
 * 닉네임 변경이 서버에서 되돌아온 사유.
 *
 * 입력 형식은 앱이 먼저 거르지만([com.teamyg.parfait.domain.usecase.CheckNameValidUseCase]),
 * 서버 규칙이 앞서 갈 수 있어 [INVALID_NICKNAME] 을 따로 받는다.
 */
enum class GroupSettingError {
    /** 400 — 서버가 닉네임 형식을 거절했다 */
    INVALID_NICKNAME,

    NETWORK,

    UNKNOWN,
}

@Composable
internal fun GroupSettingError.toStringResource(): String = when (this) {
    GroupSettingError.INVALID_NICKNAME -> stringResource(R.string.group_setting_nickname_error_invalid)
    GroupSettingError.NETWORK -> stringResource(R.string.group_setting_nickname_error_network)
    GroupSettingError.UNKNOWN -> stringResource(R.string.group_setting_nickname_error_unknown)
}
