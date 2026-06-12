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

    override suspend fun addAndGetEvicted(value: String): List<String> {
        var evicted: List<String> = emptyList()

        dataStore.edit { prefs ->
            val current: List<String> = decode(prefs[KEY])
            val updated: List<String> = (listOf(value) + current.filterNot { it == value }).take(MAX_SIZE)

            evicted = current.filterNot { it in updated }
            prefs[KEY] = json.encodeToString(updated)
        }

        return evicted
    }

    override suspend fun remove(values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        dataStore.edit { prefs ->
            val current: List<String> = decode(prefs[KEY])
            val updated: List<String> = current.filterNot { it in values }

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
        private const val MAX_SIZE: Int = 9

        private val KEY = stringPreferencesKey("recent_image_uris")
    }
}
