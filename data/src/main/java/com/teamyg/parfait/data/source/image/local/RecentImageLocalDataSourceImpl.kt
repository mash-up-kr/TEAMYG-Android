package com.teamyg.parfait.data.source.image.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.RecentImageEditor
import com.teamyg.parfait.data.model.local.RecentImageEntity
import com.teamyg.parfait.data.model.local.RecentImageKindEntity
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

    override val values: Flow<List<RecentImageEntity>> = dataStore.data
        .map { prefs -> decode(prefs[RECENT_IMAGE_URIS_KEY]) }

    override fun encodeValue(value: List<RecentImageEntity>): String = json.encodeToString(value)

    override fun decodeValue(raw: String?): List<RecentImageEntity> = decode(raw)

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

    override suspend fun remove(uris: List<String>) {
        if (uris.isEmpty()) {
            return
        }

        dataStore.edit { prefs ->
            val current: List<RecentImageEntity> = decode(prefs[RECENT_IMAGE_URIS_KEY])
            val updated: List<RecentImageEntity> = current.filterNot { it.uri in uris }

            prefs[RECENT_IMAGE_URIS_KEY] = json.encodeToString(updated)
        }
    }

    /**
     * 종류 축이 없던 시절의 값도 읽는다. 폴백 없이 빈 목록으로 떨어뜨리면 목록만 사라지고
     * 파일은 남아, 데이 윈도우 정리가 목록을 기준으로 도는 탓에 영영 고아가 된다.
     */
    private fun decode(raw: String?): List<RecentImageEntity> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching { json.decodeFromString<List<RecentImageEntity>>(raw) }
            .recoverCatching {
                json
                    .decodeFromString<List<String>>(raw)
                    .map { uri -> RecentImageEntity(uri = uri, kind = RecentImageKindEntity.SOURCE) }
            }.getOrElse { throwable ->
                sourceLogger.e(throwable) { "decode - both formats failed, dropping recent image list" }
                emptyList()
            }
    }

    companion object {
        private val RECENT_IMAGE_URIS_KEY = stringPreferencesKey("recent_image_uris")
    }
}
