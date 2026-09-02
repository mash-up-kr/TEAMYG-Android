package com.teamyg.parfait.data.source.toppingdraft.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.model.local.ToppingDraftEntity
import com.teamyg.parfait.data.model.local.toEntity
import com.teamyg.parfait.data.model.local.toVO
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToppingDraftLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    @LocalJson private val json: Json,
) : ToppingDraftLocalDataSource {
    // 이 파일을 공유하는 다른 키가 바뀌어도 `data` 는 재방출한다. 원문에서 먼저 dedupe 해야
    // 안 바뀐 JSON 을 매번 다시 파싱하지 않는다(`EncryptedPreferences.observe` 와 같은 이유)
    override val draft: Flow<ToppingDraft?> = dataStore.data
        .map { prefs -> prefs[TOPPING_DRAFT_KEY] }
        .distinctUntilChanged()
        .map { raw -> decode(raw) }

    override suspend fun save(draft: ToppingDraft) {
        dataStore.edit { prefs ->
            prefs[TOPPING_DRAFT_KEY] = json.encodeToString(draft.toEntity())
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(TOPPING_DRAFT_KEY) }
    }

    /** 못 읽는 값은 초안이 없는 것으로 본다. 지우지도 않는다 — 다음 흐름 진입이 덮어쓴다 */
    private fun decode(raw: String?): ToppingDraft? {
        if (raw.isNullOrBlank()) {
            return null
        }

        return runCatching { json.decodeFromString<ToppingDraftEntity>(raw).toVO() }.getOrNull()
    }

    internal companion object {
        const val TOPPING_DRAFT_KEY_NAME = "topping_draft"
        val TOPPING_DRAFT_KEY = stringPreferencesKey(TOPPING_DRAFT_KEY_NAME)
    }
}
