package com.teamyg.parfait.data.source.notice.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NoticeReadLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()
    private val dataSource = NoticeReadLocalDataSourceImpl(dataStore)

    @Test
    fun readNoticeIds_initially_isEmptySet() = runTest {
        assertEquals(emptySet(), dataSource.readNoticeIds.first())
    }

    @Test
    fun markRead_addsId() = runTest {
        // When 공지 1 을 읽음 처리한다
        dataSource.markRead(1L)

        // Then 읽은 id 집합에 들어간다
        assertEquals(setOf(1L), dataSource.readNoticeIds.first())
    }

    @Test
    fun markRead_sameIdTwice_isIdempotent() = runTest {
        // When 같은 공지를 두 번 연다
        dataSource.markRead(1L)
        dataSource.markRead(1L)

        // Then 집합이라 한 번만 담긴다
        assertEquals(setOf(1L), dataSource.readNoticeIds.first())
    }

    @Test
    fun markRead_accumulatesDifferentIds() = runTest {
        // When 서로 다른 공지를 연다
        dataSource.markRead(1L)
        dataSource.markRead(2L)

        // Then 앞의 기록을 덮지 않는다
        assertEquals(setOf(1L, 2L), dataSource.readNoticeIds.first())
    }

    @Test
    fun readNoticeIds_ignoresNonNumericStoredValues() = runTest {
        // Given 숫자가 아닌 값이 섞여 저장돼 있다
        dataStore.edit { it[stringSetPreferencesKey("notice_read_ids")] = setOf("1", "x") }

        // When 읽은 id 를 구독한다
        val ids = dataSource.readNoticeIds.first()

        // Then 깨진 값은 건너뛴다
        assertEquals(setOf(1L), ids)
    }
}
