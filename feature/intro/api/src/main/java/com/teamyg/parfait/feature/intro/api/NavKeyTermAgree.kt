package com.teamyg.parfait.feature.intro.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param registrationToken 카카오 로그인에서 신규 사용자로 판별될 때 발급되며, 회원 가입 요청에 그대로 사용한다
 */
@Serializable
data class NavKeyTermAgree(val registrationToken: String) : NavKey
