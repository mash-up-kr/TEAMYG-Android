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

    /**
     * 404 — 서버에 그 회원이 없다. 다른 기기에서 탈퇴했는데 이 화면에 남아 있는 경우다.
     *
     * [UNKNOWN] 과 나눠 두는 이유는 **재시도가 절대 성공하지 못하는 실패**여서다 —
     * "잠시 후 다시 시도해 주세요"는 이 경우엔 거짓말이다. 같은 서버 코드를
     * `BootstrapSessionUseCase` 는 세션 사망(인증 거절)으로 판정한다.
     *
     * ⚠️ 이 화면은 **표시만 하고 세션을 정리하지 않는다.** 죽은 세션은 다음 앱 진입의
     * 부트스트랩이 걷어낸다 — 화면이 직접 세션을 파괴하는 경로를 여는 것은 별개 결정이라
     * (강제 로그아웃 발신 주체는 `TokenAuthenticator` 하나다) 여기서 하지 않는다.
     */
    ACCOUNT_GONE,

    NETWORK,

    UNKNOWN,
}

@Composable
internal fun GlobalNicknameError.toStringResource(): String = when (this) {
    GlobalNicknameError.INVALID -> stringResource(R.string.account_info_error_invalid)
    GlobalNicknameError.ACCOUNT_GONE -> stringResource(R.string.account_info_error_account_gone)
    GlobalNicknameError.NETWORK -> stringResource(R.string.account_info_error_network)
    GlobalNicknameError.UNKNOWN -> stringResource(R.string.account_info_error_unknown)
}
