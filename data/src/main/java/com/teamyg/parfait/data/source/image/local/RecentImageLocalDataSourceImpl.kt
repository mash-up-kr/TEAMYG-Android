package com.teamyg.parfait.data.source.image.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentImageLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : RecentImageLocalDataSource {
    override val values: Flow<List<String>>
        get() = dataStore.data
            .map { prefs -> decode(prefs[KEY]) }

    override suspend fun add(value: String) {
        dataStore.edit { prefs ->
            val current: List<String> = decode(prefs[KEY])
            val updated: List<String> = (listOf(value) + current.filterNot { it == value }).take(MAX_SIZE)

            prefs[KEY] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrDefault(emptyList())
    }

    companion object {
        private const val MAX_SIZE: Int = 10

        private val KEY = stringPreferencesKey("recent_image_uris")
    }
}
