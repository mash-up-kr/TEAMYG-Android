package com.teamyg.parfait.data.source.parfait.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.security.AccountKeyAlias
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanvasIntroLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()
    private val accountKeyAlias: AccountKeyAlias = mockk {
        every { alias("canvas-intro:1", MemberId(1L)) } returns "a1"
        every { alias("canvas-intro:2", MemberId(1L)) } returns "a2"
        every { alias("canvas-intro:1", MemberId(2L)) } returns "b1"
        every { alias("canvas-intro:777", MemberId(123456L)) } returns "c1"
    }

    private val dataSource = CanvasIntroLocalDataSourceImpl(dataStore, accountKeyAlias)

    @Test
    fun markPlayed_thenRead_roundTrips() = runTest {
        // Given, When 오늘 캔버스의 연출을 재생했다고 기록한다
        dataSource.markPlayed(ACCOUNT, GroupId(1L), LocalDate(2026, 11, 23))

        // Then 그대로 읽힌다
        assertEquals(LocalDate(2026, 11, 23), dataSource.playedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun playedDate_nothingSaved_isNull() = runTest {
        // Given, When 아무것도 기록하지 않았다

        // Then 재생한 적 없는 것으로 본다
        assertNull(dataSource.playedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun markPlayed_calledAgain_replacesThePreviousDate() = runTest {
        // Given 어제 캔버스를 재생했다
        dataSource.markPlayed(ACCOUNT, GroupId(1L), LocalDate(2026, 11, 22))

        // When 오늘 캔버스를 재생한다
        dataSource.markPlayed(ACCOUNT, GroupId(1L), LocalDate(2026, 11, 23))

        // Then 같은 키가 덮여 날마다 키가 늘지 않는다
        assertEquals(LocalDate(2026, 11, 23), dataSource.playedDate(ACCOUNT, GroupId(1L)))
        assertEquals(listOf("canvas_intro_played_a1"), storedKeyNames())
    }

    @Test
    fun markPlayed_differentGroups_doNotOverwriteEachOther() = runTest {
        // Given 그룹 1이 먼저 기록한다
        dataSource.markPlayed(ACCOUNT, GroupId(1L), LocalDate(2026, 11, 22))

        // When 그룹 2가 다른 날짜를 기록한다
        dataSource.markPlayed(ACCOUNT, GroupId(2L), LocalDate(2026, 11, 23))

        // Then 두 그룹의 값이 서로 덮이지 않는다
        assertEquals(LocalDate(2026, 11, 22), dataSource.playedDate(ACCOUNT, GroupId(1L)))
        assertEquals(LocalDate(2026, 11, 23), dataSource.playedDate(ACCOUNT, GroupId(2L)))
    }

    @Test
    fun playedDate_otherAccountSameGroup_isIsolated() = runTest {
        // When 계정 1이 그룹 1의 연출을 재생한다
        dataSource.markPlayed(ACCOUNT, GroupId(1L), LocalDate(2026, 11, 23))

        // Then 같은 그룹이라도 계정 2는 아직 보지 않은 것으로 본다
        assertNull(dataSource.playedDate(OTHER_ACCOUNT, GroupId(1L)))
    }

    @Test
    fun markPlayed_storesUnderAliasKey_withoutRawIds() = runTest {
        // When id 가 123456 인 계정이 그룹 777 의 연출을 기록한다
        dataSource.markPlayed(MemberId(123456L), GroupId(777L), LocalDate(2026, 11, 23))

        // Then 저장 키에는 계정 id 도 그룹 id 도 들어 있지 않다
        val keyNames = storedKeyNames()
        assertEquals(listOf("canvas_intro_played_c1"), keyNames)
        assertFalse(keyNames.any { "123456" in it || "777" in it })
    }

    @Test
    fun playedDate_aliasFails_isNull() = runTest {
        // Given Keystore 가 별칭을 만들지 못한다
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // Then 읽기는 null 이라 연출이 화면을 막지 않는다
        assertNull(dataSource.playedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun markPlayed_aliasFails_doesNotThrow() = runTest {
        // Given Keystore 가 별칭을 만들지 못한다
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // When 쓰기를 시도해도 예외 없이 끝나고 아무것도 저장하지 않는다
        dataSource.markPlayed(ACCOUNT, GroupId(1L), LocalDate(2026, 11, 23))

        assertTrue(storedKeyNames().isEmpty())
    }

    @Test
    fun playedDate_unparsableValue_isNull() = runTest {
        // Given 날짜로 읽을 수 없는 값이 남아 있다
        dataStore.edit { it[stringPreferencesKey("canvas_intro_played_a1")] = "not-a-date" }

        // Then 재생한 적 없는 것으로 본다
        assertNull(dataSource.playedDate(ACCOUNT, GroupId(1L)))
    }

    private suspend fun storedKeyNames(): List<String> = dataStore.data
        .first()
        .asMap()
        .keys
        .map { it.name }

    private companion object {
        val ACCOUNT = MemberId(1L)
        val OTHER_ACCOUNT = MemberId(2L)
    }
}
