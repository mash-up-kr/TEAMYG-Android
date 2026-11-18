package com.teamyg.parfait.data.source.notice.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.security.AccountKeyAlias
import com.teamyg.parfait.domain.model.id.MemberId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticeReadLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()
    private val accountKeyAlias: AccountKeyAlias = mockk {
        every { alias("notice-read", MemberId(1L)) } returns "aaaa"
        every { alias("notice-read", MemberId(2L)) } returns "bbbb"
        every { alias("notice-read", MemberId(123456L)) } returns "cccc"
    }
    private val dataSource = NoticeReadLocalDataSourceImpl(dataStore, accountKeyAlias)

    @Test
    fun readNoticeIds_initially_isEmptySet() = runTest {
        assertEquals(emptySet(), dataSource.readNoticeIds(ACCOUNT).first())
    }

    @Test
    fun markRead_addsId() = runTest {
        // When 공지 1 을 읽음 처리한다
        dataSource.markRead(ACCOUNT, 1L)

        // Then 읽은 id 집합에 들어간다
        assertEquals(setOf(1L), dataSource.readNoticeIds(ACCOUNT).first())
    }

    @Test
    fun markRead_sameIdTwice_isIdempotent() = runTest {
        // When 같은 공지를 두 번 연다
        dataSource.markRead(ACCOUNT, 1L)
        dataSource.markRead(ACCOUNT, 1L)

        // Then 집합이라 한 번만 담긴다
        assertEquals(setOf(1L), dataSource.readNoticeIds(ACCOUNT).first())
    }

    @Test
    fun markRead_accumulatesDifferentIds() = runTest {
        // When 서로 다른 공지를 연다
        dataSource.markRead(ACCOUNT, 1L)
        dataSource.markRead(ACCOUNT, 2L)

        // Then 앞의 기록을 덮지 않는다
        assertEquals(setOf(1L, 2L), dataSource.readNoticeIds(ACCOUNT).first())
    }

    @Test
    fun readNoticeIds_ignoresNonNumericStoredValues() = runTest {
        // Given 숫자가 아닌 값이 섞여 저장돼 있다
        dataStore.edit { it[stringSetPreferencesKey("notice_read_aaaa")] = setOf("1", "x") }

        // When 읽은 id 를 구독한다
        val ids = dataSource.readNoticeIds(ACCOUNT).first()

        // Then 깨진 값은 건너뛴다
        assertEquals(setOf(1L), ids)
    }

    @Test
    fun readNoticeIds_otherAccount_isIsolated() = runTest {
        // When 계정 1 이 공지를 읽는다
        dataSource.markRead(ACCOUNT, 1L)

        // Then 계정 2 는 아직 아무것도 읽지 않았다
        assertEquals(emptySet(), dataSource.readNoticeIds(OTHER_ACCOUNT).first())
    }

    @Test
    fun clear_removesOnlyThatAccount() = runTest {
        // Given 두 계정이 각자 읽은 기록이 있다
        dataSource.markRead(ACCOUNT, 1L)
        dataSource.markRead(OTHER_ACCOUNT, 2L)

        // When 계정 1 의 기록을 지운다
        dataSource.clear(ACCOUNT)

        // Then 계정 1 만 비고 계정 2 는 남는다
        assertEquals(emptySet(), dataSource.readNoticeIds(ACCOUNT).first())
        assertEquals(setOf(2L), dataSource.readNoticeIds(OTHER_ACCOUNT).first())
    }

    @Test
    fun markRead_storesUnderAliasKey_withoutRawId() = runTest {
        // When id 가 123456 인 계정이 공지를 읽는다
        dataSource.markRead(MemberId(123456L), 1L)

        // Then 저장 키는 별칭으로 만들어지고 원본 id 가 들어 있지 않다
        val keyNames = storedKeyNames()
        assertEquals(listOf("notice_read_cccc"), keyNames)
        assertFalse(keyNames.any { "123456" in it })
    }

    @Test
    fun readNoticeIds_aliasFails_isEmptySet() = runTest {
        // Given Keystore 가 별칭을 만들지 못한다
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // Then 읽기는 빈 집합이다 — 읽음 기록이 화면을 막지 않는다
        assertEquals(emptySet(), dataSource.readNoticeIds(ACCOUNT).first())
    }

    @Test
    fun markRead_aliasFails_doesNotThrow() = runTest {
        // Given Keystore 가 별칭을 만들지 못한다
        every { accountKeyAlias.alias(any(), any()) } throws IllegalStateException("keystore")

        // When 쓰기와 삭제를 시도해도 예외 없이 끝나고 아무것도 저장하지 않는다
        dataSource.markRead(ACCOUNT, 1L)
        dataSource.clear(ACCOUNT)

        assertTrue(storedKeyNames().isEmpty())
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
