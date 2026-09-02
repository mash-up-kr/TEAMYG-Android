package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 그룹 생성 실패를 사용자에게 알릴 때 고르는 갈래.
 *
 * 서버 사유는 더 잘게 나뉘지만 사용자가 할 수 있는 일이 연결 확인과 재시도 둘뿐이라 여기서 접는다.
 */
enum class GroupCreateError {
    NETWORK,

    UNKNOWN,
}

@Composable
internal fun GroupCreateError.toStringResource(): String = when (this) {
    GroupCreateError.NETWORK -> stringResource(R.string.group_create_error_network)
    GroupCreateError.UNKNOWN -> stringResource(R.string.group_create_error_unknown)
}
