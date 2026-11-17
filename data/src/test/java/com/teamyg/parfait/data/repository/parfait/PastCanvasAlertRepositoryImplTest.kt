package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.source.parfait.local.PastCanvasAlertLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PastCanvasAlertRepositoryImplTest {
    private val local: PastCanvasAlertLocalDataSource = mockk(relaxed = true)
    private val repository = PastCanvasAlertRepositoryImpl(local)

    @Test
    fun lastSeenClosedDate_readsUnderTheGivenMemberAndGroup() = runTest {
        // Given 계정 1 · 그룹 5 의 기록이 있다
        coEvery { local.lastSeenClosedDate(MemberId(1L), GroupId(5L)) } returns LocalDate(2026, 8, 20)

        // Then 넘겨받은 계정·그룹으로 읽는다
        assertEquals(LocalDate(2026, 8, 20), repository.lastSeenClosedDate(MemberId(1L), GroupId(5L)))
    }

    @Test
    fun markSeen_writesUnderTheGivenMemberAndGroup() = runTest {
        // When 계정 1 · 그룹 5 의 마감일을 기록한다
        repository.markSeen(MemberId(1L), GroupId(5L), LocalDate(2026, 8, 20))

        // Then 그 계정·그룹으로 저장소에 맡긴다
        coVerify(exactly = 1) { local.markSeenClosedDate(MemberId(1L), GroupId(5L), LocalDate(2026, 8, 20)) }
    }
}
