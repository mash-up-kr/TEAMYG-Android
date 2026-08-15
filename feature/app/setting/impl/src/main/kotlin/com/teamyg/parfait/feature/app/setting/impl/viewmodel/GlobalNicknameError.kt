package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.app.setting.impl.R

/**
 * 전역 닉네임 변경 요청이 서버에서 되돌아온 사유.
 *
 * 그룹 닉네임의 `GroupNickNameError`(`feature/groups/enter/impl`)와 형태는 같지만 재사용하지
 * 않는다 — feature `impl` 모듈은 서로를 의존하지 않는 leaf 모듈이라 그쪽을 참조할 수 없고,
 * 서버 에러 어휘도 실제로 다르다: 전역 닉네임은 그룹 스코프가 아니라 중복(`ALREADY_USED`)
 * 개념 자체가 없다(`parfait/api/member.md` — `PATCH /api/v1/users/me/nickname` 에러 코드
 * 전수: `INVALID_NICKNAME`·`MEMBER_NOT_FOUND`뿐).
 *
 * 입력 형식 자체가 틀린 경우는 요청 전에 `CheckNameValidUseCase`가 걸러
 * `NameValidResult.Error`로 표시하므로, 여기에는 서버만 알 수 있는 사유가 남는다.
 */
enum class GlobalNicknameError {
    /** 400 — 서버 닉네임 규칙에 걸렸다. 앱 검증과 서버 규칙이 어긋났다는 신호다 */
    INVALID,

    NETWORK,

    UNKNOWN,
}

@Composable
internal fun GlobalNicknameError.toStringResource(): String = when (this) {
    GlobalNicknameError.INVALID -> stringResource(R.string.account_info_error_invalid)
    GlobalNicknameError.NETWORK -> stringResource(R.string.account_info_error_network)
    GlobalNicknameError.UNKNOWN -> stringResource(R.string.account_info_error_unknown)
}
