package com.teamyg.parfait.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 메모리 [MutableStateFlow] 로 구현한 [DataStore]. Keystore·디스크 IO 없이 `edit`/`data`
 * 왕복만 검증하면 되는 테스트 전용 대역이다. `putRaw` 는 테스트가 저장 형태를 직접
 * 심어(암호화되지 않은 원문 그대로) "앱이 모르는 값이 저장돼 있다" 같은 케이스를 세팅하는
 * 헬퍼다. [updateCount] 는 쓰기가 몇 번의 `edit` 으로 나갔는지 센다.
 */
internal class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    var updateCount: Int = 0
        private set

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        updateCount++
        val updated = transform(state.value)
        state.value = updated
        return updated
    }

    fun putRaw(
        key: String,
        value: String,
    ) {
        val mutable: MutablePreferences = state.value.toMutablePreferences()
        mutable[stringPreferencesKey(key)] = value
        state.value = mutable
    }
}
