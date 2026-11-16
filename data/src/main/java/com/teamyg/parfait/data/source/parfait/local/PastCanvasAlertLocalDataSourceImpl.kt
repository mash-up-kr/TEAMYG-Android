package com.teamyg.parfait.data.source.parfait.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.security.AccountKeyAlias
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PastCanvasAlertLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val accountKeyAlias: AccountKeyAlias,
) : PastCanvasAlertLocalDataSource {
    override suspend fun lastSeenClosedDate(
        memberId: MemberId,
        groupId: GroupId,
    ): LocalDate? {
        val key = keyOrNull(memberId, groupId) ?: return null
        val raw = dataStore.data.first()[key] ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    override suspend fun markSeenClosedDate(
        memberId: MemberId,
        groupId: GroupId,
        date: LocalDate,
    ) {
        val key = keyOrNull(memberId, groupId) ?: return
        dataStore.edit { prefs -> prefs[key] = date.toString() }
    }

    // 그룹 id 도 scope 에 섞어 키에서 계정과 그룹이 둘 다 드러나지 않게 한다. 별칭을 못 만들면
    // 알럿 기록이 화면을 막지 않도록 읽기는 null, 쓰기는 건너뛴다
    private fun keyOrNull(
        memberId: MemberId,
        groupId: GroupId,
    ) = runCatching { accountKeyAlias.alias("$SCOPE_PREFIX${groupId.value}", memberId) }
        .getOrNull()
        ?.let { alias -> stringPreferencesKey("$KEY_PREFIX$alias") }

    private companion object {
        const val SCOPE_PREFIX = "past-canvas-alert:"
        const val KEY_PREFIX = "past_canvas_alert_seen_"
    }
}
