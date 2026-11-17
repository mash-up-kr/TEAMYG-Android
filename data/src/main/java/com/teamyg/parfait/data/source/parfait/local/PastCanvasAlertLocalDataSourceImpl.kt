package com.teamyg.parfait.data.source.parfait.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PastCanvasAlertLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : PastCanvasAlertLocalDataSource {
    override suspend fun lastSeenClosedDate(groupId: GroupId): LocalDate? {
        val raw = dataStore.data.first()[groupKey(groupId)] ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    override suspend fun markSeenClosedDate(
        groupId: GroupId,
        date: LocalDate,
    ) {
        dataStore.edit { prefs -> prefs[groupKey(groupId)] = date.toString() }
    }

    /** 그룹마다 별도 키를 둬 여러 그룹을 오가도 서로의 확인 여부를 덮지 않는다 */
    private fun groupKey(groupId: GroupId) = stringPreferencesKey("$KEY_PREFIX${groupId.value}")

    private companion object {
        const val KEY_PREFIX = "past_canvas_alert_seen_group_"
    }
}
