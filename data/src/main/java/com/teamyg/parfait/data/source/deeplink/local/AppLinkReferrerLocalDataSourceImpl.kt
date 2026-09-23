package com.teamyg.parfait.data.source.deeplink.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLinkReferrerLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : AppLinkReferrerLocalDataSource {
    override suspend fun hasChecked(): Boolean = dataStore.data.first()[KEY_CHECKED] ?: false

    override suspend fun markChecked() {
        dataStore.edit { prefs -> prefs[KEY_CHECKED] = true }
    }

    private companion object {
        val KEY_CHECKED = booleanPreferencesKey("app_link_referrer_checked")
    }
}
