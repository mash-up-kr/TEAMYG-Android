package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.intro.impl.R

/**
 * 가입 요청이 실패한 사유. 서버 에러와 예상 못 한 오류가 한 갈래인 것은 사용자가 할 수 있는
 * 일이 "잠시 후 다시"로 같아서다 — 문구가 갈리는 지점에서만 나눈다.
 */
enum class TermAgreeError {
    NETWORK,

    /** 서버 에러·필수 약관 도메인 거절·매퍼 실패 등 그 외 전부 */
    UNKNOWN,
}

@Composable
internal fun TermAgreeError.toStringResource(): String = when (this) {
    TermAgreeError.NETWORK -> stringResource(R.string.term_agree_signup_error_network)
    TermAgreeError.UNKNOWN -> stringResource(R.string.term_agree_signup_error_unknown)
}
