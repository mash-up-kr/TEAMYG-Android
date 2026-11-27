package com.teamyg.parfait.data.source.solo.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 그룹 연출 기록(`CanvasIntroLocalDataSourceImpl`)과 달리 계정 별칭 없이 고정 키 하나에 마지막 재생 날짜를 덮어쓴다 */
@Singleton
class SoloIntroLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : SoloIntroLocalDataSource {
    // 읽기는 edit 을 타지 않는다 — edit 은 쓰기 큐를 거쳐 다른 쓰기와 서로 기다리게 된다
    override suspend fun playedDate(): LocalDate? {
        val raw = dataStore.data.first()[PLAYED_DATE_KEY] ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    override suspend fun markPlayed(date: LocalDate) {
        dataStore.edit { prefs -> prefs[PLAYED_DATE_KEY] = date.toString() }
    }

    private companion object {
        val PLAYED_DATE_KEY = stringPreferencesKey("solo_space_intro_played")
    }
}
