package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.intro.impl.R

/**
 * 가입 요청이 실패한 사유. `LoginError` 와 같은 형태이고 같은 이유로 재사용하지 않는다 —
 * feature `impl` 은 서로를 의존하지 않는 leaf 모듈이고, 실패 어휘도 화면마다 다르다.
 *
 * **문구가 갈리는 지점에서만** 나눈다. 서버 에러와 예상 못 한 오류가 한 갈래인 것은 둘 다
 * 사용자가 할 수 있는 일이 "잠시 후 다시"로 같아서다 — 구분은 로그가 남긴다.
 *
 * 약관 조회 실패는 여기 없다. 그 실패에는 재시도 버튼이라는 갈 곳이 있어 화면이 자기 UI 로
 * 말한다(`TermAgreeState.isLoadFailed`).
 */
enum class TermAgreeError {
    /** 연결 실패·타임아웃. 사용자가 스스로 고칠 수 있는 유일한 갈래다 */
    NETWORK,

    /** 서버 에러·필수 약관 도메인 거절·매퍼 실패 등 그 외 전부 */
    UNKNOWN,
}

@Composable
internal fun TermAgreeError.toStringResource(): String = when (this) {
    TermAgreeError.NETWORK -> stringResource(R.string.term_agree_signup_error_network)
    TermAgreeError.UNKNOWN -> stringResource(R.string.term_agree_signup_error_unknown)
}
