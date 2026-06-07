package com.teamyg.core.datastore.temp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TempPreferencesDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : TempPreferencesDataSource {
    override val accessToken: Flow<String?>
        get() = dataStore.data
            .map { preferences -> preferences[KEY_ACCESS_TOKEN] }

    override suspend fun setAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = token
        }
    }

    override suspend fun clearAccessToken() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
        }
    }

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    }
}
