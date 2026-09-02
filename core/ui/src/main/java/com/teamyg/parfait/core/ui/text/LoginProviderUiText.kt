package com.teamyg.parfait.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.core.ui.R as CoreR

/**
 * [LoginProvider] 표시 문자열 매핑을 [com.teamyg.parfait.core.ui]가 단일 소유한다(ADR-0016) —
 * 로그인 수단을 보여주는 화면이 늘어도 매핑이 화면마다 중복되지 않게 하기 위해서다.
 *
 * [LoginProvider.UNKNOWN]도 문구를 준다 — 서버 영속 계층에는 core enum에 없는 provider(`GOOGLE`
 * 등, `api/member.md` 참고)가 있어 매퍼가 폴백으로 이 값을 줄 수 있다. 문구 없이 넘기면 화면이
 * 빈 문자열로 남는다.
 */
@Composable
fun LoginProvider.toStringResource(): String = when (this) {
    LoginProvider.KAKAO -> stringResource(CoreR.string.login_provider_kakao)
    LoginProvider.APPLE -> stringResource(CoreR.string.login_provider_apple)
    LoginProvider.UNKNOWN -> stringResource(CoreR.string.login_provider_unknown)
}
