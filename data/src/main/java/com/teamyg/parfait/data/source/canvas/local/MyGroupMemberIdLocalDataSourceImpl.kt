package com.teamyg.parfait.data.source.canvas.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyGroupMemberIdLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    @LocalJson private val json: Json,
) : MyGroupMemberIdLocalDataSource {
    override val values: Flow<Map<GroupId, GroupMemberId>> = dataStore.data
        .map { prefs -> prefs[MY_GROUP_MEMBER_ID_KEY] }
        .distinctUntilChanged()
        .map { raw -> decode(raw) }

    override suspend fun save(
        groupId: GroupId,
        groupMemberId: GroupMemberId,
    ) {
        dataStore.edit { prefs ->
            val updated = decode(prefs[MY_GROUP_MEMBER_ID_KEY]) + (groupId to groupMemberId)
            prefs[MY_GROUP_MEMBER_ID_KEY] = json.encodeToString(updated.toRawMap())
        }
    }

    private fun decode(raw: String?): Map<GroupId, GroupMemberId> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<Long, Long>>(raw).toDomainMap() }.getOrDefault(emptyMap())
    }

    private fun Map<GroupId, GroupMemberId>.toRawMap(): Map<Long, Long> =
        mapKeys { (groupId, _) -> groupId.value }.mapValues { (_, groupMemberId) -> groupMemberId.value }

    private fun Map<Long, Long>.toDomainMap(): Map<GroupId, GroupMemberId> =
        mapKeys { (groupId, _) -> GroupId(groupId) }.mapValues { (_, groupMemberId) -> GroupMemberId(groupMemberId) }

    internal companion object {
        const val MY_GROUP_MEMBER_ID_KEY_NAME = "my_group_member_id_map"
        val MY_GROUP_MEMBER_ID_KEY = stringPreferencesKey(MY_GROUP_MEMBER_ID_KEY_NAME)
    }
}
