package com.teamyg.parfait.data.source.image.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.RecentImageEditor
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.data.utils.sourceLogger
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
    @LocalJson private val json: Json,
) : RecentImageLocalDataSource {
    init {
        sourceLogger.i { "RecentImageLocalDataSourceImpl::init" }
    }

    override val values: Flow<List<String>> = dataStore.data
        .map { prefs -> decode(prefs[RECENT_IMAGE_URIS_KEY]) }

    override fun encodeValue(value: List<String>): String = json.encodeToString(value)

    override fun decodeValue(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrDefault(emptyList())
    }

    override suspend fun edit(transform: suspend (RecentImageEditor) -> Unit) {
        dataStore.edit { prefs ->
            transform(
                object : RecentImageEditor {
                    override fun get(): String? = prefs[RECENT_IMAGE_URIS_KEY]

                    override fun set(value: String) {
                        prefs[RECENT_IMAGE_URIS_KEY] = value
                    }
                },
            )
        }
    }

    override suspend fun remove(values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        dataStore.edit { prefs ->
            val current: List<String> = decode(prefs[RECENT_IMAGE_URIS_KEY])
            val updated: List<String> = current.filterNot { it in values }

            prefs[RECENT_IMAGE_URIS_KEY] = json.encodeToString(updated)
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
        private val RECENT_IMAGE_URIS_KEY = stringPreferencesKey("recent_image_uris")
    }
}
