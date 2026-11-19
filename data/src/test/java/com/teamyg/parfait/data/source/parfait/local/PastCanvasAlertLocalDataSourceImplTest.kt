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

class PastCanvasAlertLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()
    private val accountKeyAlias: AccountKeyAlias = mockk {
        every { alias("past-canvas-alert:1", MemberId(1L)) } returns "a1"
        every { alias("past-canvas-alert:2", MemberId(1L)) } returns "a2"
        every { alias("past-canvas-alert:1", MemberId(2L)) } returns "b1"
        every { alias("past-canvas-alert:777", MemberId(123456L)) } returns "c1"
    }

    private val dataSource = PastCanvasAlertLocalDataSourceImpl(dataStore, accountKeyAlias)

    @Test
    fun markSeenClosedDate_thenRead_roundTrips() = runTest {
        // Given, When 마감일을 봤다고 기록한다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 20))

        // Then 그대로 읽힌다
        assertEquals(LocalDate(2026, 8, 20), dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun lastSeenClosedDate_nothingSaved_isNull() = runTest {
        // Given, When 아무것도 기록하지 않았다

        // Then 안 본 것으로 본다
        assertNull(dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun markSeenClosedDate_differentGroups_doNotOverwriteEachOther() = runTest {
        // Given 그룹 1이 먼저 마감일을 기록해 둔다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 20))

        // When 그룹 2가 다른 마감일을 기록한다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(2L), LocalDate(2026, 8, 21))

        // Then 두 그룹의 값이 서로 덮이지 않는다
        assertEquals(LocalDate(2026, 8, 20), dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
        assertEquals(LocalDate(2026, 8, 21), dataSource.lastSeenClosedDate(ACCOUNT, GroupId(2L)))
    }

    @Test
    fun markSeenClosedDate_calledAgain_replacesThePreviousDate() = runTest {
        // Given 어제 마감일을 이미 확인했다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 20))

        // When 다음 마감일을 확인한다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 21))

        // Then 최신 값으로 덮인다 — 예전 마감일은 더 이상 의미가 없다
        assertEquals(LocalDate(2026, 8, 21), dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun lastSeenClosedDate_otherAccountSameGroup_isIsolated() = runTest {
        // When 계정 1 이 그룹 1 의 마감일을 확인한다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 20))

        // Then 같은 그룹이라도 계정 2 는 아직 처음이다 — 다른 계정의 확인을 물려받지 않는다
        assertNull(dataSource.lastSeenClosedDate(OTHER_ACCOUNT, GroupId(1L)))
    }

    @Test
    fun markSeenClosedDate_storesUnderAliasKey_withoutRawIds() = runTest {
        // When id 가 123456 인 계정이 그룹 777 의 마감일을 기록한다
        dataSource.markSeenClosedDate(MemberId(123456L), GroupId(777L), LocalDate(2026, 8, 20))

        // Then 저장 키는 별칭으로 만들어지고 계정 id 도 그룹 id 도 들어 있지 않다
        val keyNames = storedKeyNames()
        assertEquals(listOf("past_canvas_alert_seen_c1"), keyNames)
        assertFalse(keyNames.any { "123456" in it || "777" in it })
    }

    @Test
    fun lastSeenClosedDate_aliasFails_isNull() = runTest {
        // Given Keystore 가 별칭을 만들지 못한다
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // Then 읽기는 null 이다 — 알럿이 화면을 막지 않는다
        assertNull(dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
    }

    @Test
    fun markSeenClosedDate_aliasFails_doesNotThrow() = runTest {
        // Given Keystore 가 별칭을 만들지 못한다
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // When 쓰기를 시도해도 예외 없이 끝나고 아무것도 저장하지 않는다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 20))

        assertTrue(storedKeyNames().isEmpty())
    }

    @Test
    fun lastSeenClosedDate_legacyValue_movesToAliasKeyAndRemovesLegacy() = runTest {
        // Given 이전 버전이 그룹 id 를 그대로 키에 쓴 값이 남아 있다
        putLegacy(groupId = 1L, raw = "2026-08-20")

        // When 계정 1 이 그 그룹을 읽는다
        val seen = dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L))

        // Then 값을 이어받고, 값은 별칭 키로 옮겨지며 그룹 id 가 보이는 옛 키는 사라진다
        assertEquals(LocalDate(2026, 8, 20), seen)
        assertEquals(listOf("past_canvas_alert_seen_a1"), storedKeyNames())
    }

    @Test
    fun lastSeenClosedDate_legacyValueAlreadyMoved_otherAccountDoesNotInherit() = runTest {
        // Given 계정 1 이 먼저 읽어 옛 값을 가져갔다
        putLegacy(groupId = 1L, raw = "2026-08-20")
        dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L))

        // Then 같은 그룹이라도 계정 2 는 물려받지 않는다
        assertNull(dataSource.lastSeenClosedDate(OTHER_ACCOUNT, GroupId(1L)))
    }

    @Test
    fun lastSeenClosedDate_aliasKeyExists_winsAndDropsLegacy() = runTest {
        // Given 새 키에 이미 기록이 있고 옛 키도 남아 있다
        dataSource.markSeenClosedDate(ACCOUNT, GroupId(1L), LocalDate(2026, 8, 25))
        putLegacy(groupId = 1L, raw = "2026-08-20")

        // Then 새 키의 값이 이기고 옛 값으로 덮지 않으며, 그룹 id 가 드러나는 옛 키는 정리한다
        assertEquals(LocalDate(2026, 8, 25), dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
        assertEquals(listOf("past_canvas_alert_seen_a1"), storedKeyNames())
    }

    @Test
    fun lastSeenClosedDate_brokenLegacyValue_dropsItAndIsNull() = runTest {
        // Given 날짜로 읽을 수 없는 옛 값이 남아 있다
        putLegacy(groupId = 1L, raw = "not-a-date")

        // Then 값을 이어받지 않고 옛 키만 정리한다
        assertNull(dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
        assertTrue(storedKeyNames().isEmpty())
    }

    @Test
    fun lastSeenClosedDate_aliasFails_keepsLegacyValueForLater() = runTest {
        // Given 옛 값이 있고 Keystore 가 별칭을 만들지 못한다
        putLegacy(groupId = 1L, raw = "2026-08-20")
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // Then 옮기지 못하니 null 이고 옛 값은 다음 기회를 위해 남긴다
        assertNull(dataSource.lastSeenClosedDate(ACCOUNT, GroupId(1L)))
        assertEquals(listOf("past_canvas_alert_seen_group_1"), storedKeyNames())
    }

    private suspend fun putLegacy(
        groupId: Long,
        raw: String,
    ) {
        dataStore.edit { it[stringPreferencesKey("past_canvas_alert_seen_group_$groupId")] = raw }
    }

    private suspend fun storedKeyNames(): List<String> {
        val stored = dataStore.data.first()
        return stored.asMap().keys.map { it.name }
    }

    private companion object {
        val ACCOUNT = MemberId(1L)
        val OTHER_ACCOUNT = MemberId(2L)
    }
}
