package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PastCanvasAlertLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    private val dataSource = PastCanvasAlertLocalDataSourceImpl(dataStore = dataStore)

    @Test
    fun markSeenClosedDate_thenRead_roundTrips() = runTest {
        // Given, When 마감일을 봤다고 기록한다
        dataSource.markSeenClosedDate(GroupId(1L), LocalDate(2026, 8, 20))

        // Then 그대로 읽힌다
        assertEquals(LocalDate(2026, 8, 20), dataSource.lastSeenClosedDate(GroupId(1L)))
    }

    @Test
    fun lastSeenClosedDate_nothingSaved_isNull() = runTest {
        // Given, When 아무것도 기록하지 않았다

        // Then 안 본 것으로 본다
        assertNull(dataSource.lastSeenClosedDate(GroupId(1L)))
    }

    @Test
    fun markSeenClosedDate_differentGroups_doNotOverwriteEachOther() = runTest {
        // Given 그룹 1이 먼저 마감일을 기록해 둔다
        dataSource.markSeenClosedDate(GroupId(1L), LocalDate(2026, 8, 20))

        // When 그룹 2가 다른 마감일을 기록한다
        dataSource.markSeenClosedDate(GroupId(2L), LocalDate(2026, 8, 21))

        // Then 두 그룹의 값이 서로 덮이지 않는다
        assertEquals(LocalDate(2026, 8, 20), dataSource.lastSeenClosedDate(GroupId(1L)))
        assertEquals(LocalDate(2026, 8, 21), dataSource.lastSeenClosedDate(GroupId(2L)))
    }

    @Test
    fun markSeenClosedDate_calledAgain_replacesThePreviousDate() = runTest {
        // Given 어제 마감일을 이미 확인했다
        dataSource.markSeenClosedDate(GroupId(1L), LocalDate(2026, 8, 20))

        // When 다음 마감일을 확인한다
        dataSource.markSeenClosedDate(GroupId(1L), LocalDate(2026, 8, 21))

        // Then 최신 값으로 덮인다 — 예전 마감일은 더 이상 의미가 없다
        assertEquals(LocalDate(2026, 8, 21), dataSource.lastSeenClosedDate(GroupId(1L)))
    }
}
