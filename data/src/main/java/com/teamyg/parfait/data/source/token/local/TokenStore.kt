package com.teamyg.parfait.data.source.token.local

interface TokenStore {
    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun save(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clear()
}
