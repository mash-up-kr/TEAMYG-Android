package com.teamyg.parfait.domain.model.auth

import kotlin.time.Duration

data class AuthSessionVO(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
    val expiresIn: Duration,
)
