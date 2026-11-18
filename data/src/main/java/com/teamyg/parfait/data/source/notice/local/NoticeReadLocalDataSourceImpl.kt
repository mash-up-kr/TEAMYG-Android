package com.teamyg.parfait.data.source.notice.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.teamyg.parfait.data.security.AccountKeyAlias
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoticeReadLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val accountKeyAlias: AccountKeyAlias,
) : NoticeReadLocalDataSource {
    override fun readNoticeIds(memberId: MemberId): Flow<Set<Long>> {
        val key = keyOrNull(memberId) ?: return flowOf(emptySet())

        // 숫자가 아닌 값이 섞여 있어도 앱이 죽지 않게 건너뛴다
        return dataStore.data
            .map { prefs -> prefs[key].orEmpty().mapNotNull(String::toLongOrNull).toSet() }
            .distinctUntilChanged()
    }

    override suspend fun markRead(
        memberId: MemberId,
        noticeId: Long,
    ) {
        val key = keyOrNull(memberId) ?: return
        dataStore.edit { prefs -> prefs[key] = prefs[key].orEmpty() + noticeId.toString() }
    }

    override suspend fun clear(memberId: MemberId) {
        val key = keyOrNull(memberId) ?: return
        dataStore.edit { prefs -> prefs.remove(key) }
    }

    // 읽음 기록은 편의 기능이라 Keystore 가 실패해도 화면을 막지 않고 기록만 포기한다
    private fun keyOrNull(memberId: MemberId): Preferences.Key<Set<String>>? = runCatching {
        accountKeyAlias.alias(SCOPE, memberId)
    }.getOrNull()?.let { alias -> stringSetPreferencesKey("$KEY_PREFIX$alias") }

    private companion object {
        const val SCOPE = "notice-read"
        const val KEY_PREFIX = "notice_read_"
    }
}
