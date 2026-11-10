package com.teamyg.parfait.data.source.notice.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoticeReadLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : NoticeReadLocalDataSource {
    // 숫자가 아닌 값이 섞여 있어도 앱이 죽지 않게 건너뛴다
    override val readNoticeIds: Flow<Set<Long>> = dataStore.data
        .map { prefs -> prefs[KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet() }
        .distinctUntilChanged()

    override suspend fun markRead(id: Long) {
        dataStore.edit { prefs -> prefs[KEY] = prefs[KEY].orEmpty() + id.toString() }
    }

    private companion object {
        val KEY = stringSetPreferencesKey("notice_read_ids")
    }
}
