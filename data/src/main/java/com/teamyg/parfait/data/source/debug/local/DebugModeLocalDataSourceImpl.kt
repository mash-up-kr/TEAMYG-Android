package com.teamyg.parfait.data.source.debug.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugModeLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : DebugModeLocalDataSource {
    // 이 파일을 공유하는 다른 키가 바뀌어도 `data` 는 재방출한다 — 여기서 먼저 dedupe 하지
    // 않으면 무관한 쓰기마다 구독자가 흔들린다(`ToppingDraftLocalDataSourceImpl` 과 같은 이유)
    override val isEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[DEBUG_MODE_KEY] == true }
        .distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DEBUG_MODE_KEY] = enabled }
    }

    private companion object {
        val DEBUG_MODE_KEY = booleanPreferencesKey("debug_mode")
    }
}
