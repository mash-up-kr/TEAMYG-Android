package com.teamyg.parfait.feature.login.impl.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.login.impl.R

/**
 * 로그인이 실패한 사유. **화면이 사용자에게 말할 수 있는 단위**로만 갈라 둔다 —
 * 서버 에러 코드 하나에 갈래 하나가 아니라, 문구가 갈리는 지점에서만 나눈다.
 *
 * `AccountInfoViewModel`의 `GlobalNicknameError`와 같은 형태이고 같은 이유로 재사용하지
 * 않는다: feature `impl` 은 서로를 의존하지 않는 leaf 모듈이고, 실패 어휘도 화면마다 다르다.
 *
 * 사용자 취소는 여기에 없다. 취소는 실패가 아니라서 아무것도 알리지 않는다.
 */
enum class LoginError {
    /** 연결 실패·타임아웃. 사용자가 스스로 고칠 수 있는 유일한 갈래다 */
    NETWORK,

    /** 서버가 ID 토큰을 검증하지 못했다(401). 다시 로그인하면 대개 풀린다 */
    INVALID_ID_TOKEN,

    /**
     * 카카오 쪽이 원활하지 않다 — 공개키 조회 실패(502)·카카오 서버 불가(503)·SDK 단계 실패.
     * 셋 다 앱이 할 수 있는 게 없고 사용자에게 할 말도 "잠시 후 다시"로 같아서 한 갈래다.
     */
    KAKAO_UNAVAILABLE,

    /** 미분류 서버 에러·매퍼 실패 등 그 외 전부 */
    UNKNOWN,
}

@Composable
internal fun LoginError.toStringResource(): String = when (this) {
    LoginError.NETWORK -> stringResource(R.string.login_error_network)
    LoginError.INVALID_ID_TOKEN -> stringResource(R.string.login_error_invalid_id_token)
    LoginError.KAKAO_UNAVAILABLE -> stringResource(R.string.login_error_kakao_unavailable)
    LoginError.UNKNOWN -> stringResource(R.string.login_error_unknown)
}
