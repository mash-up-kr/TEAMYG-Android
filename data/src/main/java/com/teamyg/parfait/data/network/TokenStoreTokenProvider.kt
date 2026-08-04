package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.source.token.local.TokenStore
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class TokenStoreTokenProvider
@Inject
constructor(
    private val tokenStore: TokenStore,
) : TokenProvider {
    override fun getToken(): String? = runBlocking { tokenStore.getAccessToken() }
}
