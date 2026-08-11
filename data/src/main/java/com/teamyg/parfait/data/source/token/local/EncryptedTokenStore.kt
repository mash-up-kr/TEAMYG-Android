package com.teamyg.parfait.data.source.token.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.security.CryptoManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EncryptedTokenStore
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager,
) : TokenStore {
    override suspend fun getAccessToken(): String? = read(ACCESS_TOKEN_KEY)

    override suspend fun getRefreshToken(): String? = read(REFRESH_TOKEN_KEY)

    override suspend fun save(
        accessToken: String,
        refreshToken: String,
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = cryptoManager.encrypt(accessToken)
            preferences[REFRESH_TOKEN_KEY] = cryptoManager.encrypt(refreshToken)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }

    private suspend fun read(key: Preferences.Key<String>): String? = runCatching {
        val encrypted = dataStore.data.first()[key] ?: return@runCatching null
        cryptoManager.decrypt(encrypted)
    }.getOrElse {
        runCatching { clear() }
        null
    }

    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
}
