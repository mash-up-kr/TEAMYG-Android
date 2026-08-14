package com.teamyg.parfait.feature.intro.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param registrationToken 카카오 로그인이 신규 회원으로 판정하며 내려준 가입 토큰.
 *   약관 동의 후 `POST /api/v1/auth/signup` 에 넘긴다.
 */
@Serializable
data class NavKeyTermAgree(val registrationToken: String) : NavKey
