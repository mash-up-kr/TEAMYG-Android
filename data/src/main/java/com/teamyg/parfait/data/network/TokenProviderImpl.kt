package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.source.token.local.TokenLocalDataSource
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class TokenProviderImpl
@Inject
constructor(
    private val tokenLocalDataSource: TokenLocalDataSource,
) : TokenProvider {
    override fun getToken(): String? = runBlocking { tokenLocalDataSource.getAccessToken() }
}
