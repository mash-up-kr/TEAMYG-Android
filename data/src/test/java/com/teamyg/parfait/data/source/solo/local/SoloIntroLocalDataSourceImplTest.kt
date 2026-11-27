package com.teamyg.parfait.data.source.solo.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SoloIntroLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    private val dataSource = SoloIntroLocalDataSourceImpl(dataStore)

    @Test
    fun markPlayed_thenRead_roundTrips() = runTest {
        // Given, When 오늘 연출을 재생했다고 기록한다
        dataSource.markPlayed(LocalDate(2026, 11, 27))

        // Then 그대로 읽힌다
        assertEquals(LocalDate(2026, 11, 27), dataSource.playedDate())
    }

    @Test
    fun markPlayed_overwritesPreviousDate() = runTest {
        // Given 어제 재생했다
        dataSource.markPlayed(LocalDate(2026, 11, 26))

        // When 오늘 다시 재생한다
        dataSource.markPlayed(LocalDate(2026, 11, 27))

        // Then 마지막 날짜 하나만 남는다
        assertEquals(LocalDate(2026, 11, 27), dataSource.playedDate())
    }

    @Test
    fun playedDate_nothingSaved_isNull() = runTest {
        // Given, When 아무것도 기록하지 않았다

        // Then 재생한 적 없는 것으로 본다
        assertNull(dataSource.playedDate())
    }

    @Test
    fun playedDate_corruptValue_isNull() = runTest {
        // Given 날짜로 읽을 수 없는 값이 저장돼 있다
        dataStore.edit { prefs -> prefs[stringPreferencesKey("solo_space_intro_played")] = "not-a-date" }

        // When, Then 재생한 적 없는 것으로 본다
        assertNull(dataSource.playedDate())
    }
}
