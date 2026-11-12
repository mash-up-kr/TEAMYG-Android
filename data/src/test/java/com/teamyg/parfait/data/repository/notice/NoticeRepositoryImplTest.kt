package com.teamyg.parfait.data.repository.notice

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticeRepositoryImplTest {
    private val repository = NoticeRepositoryImpl()

    @Test
    fun getNotices_returnsFiveNoticesNewestFirstWithOnlyTheLatestMarkedNew() = runTest {
        // When 목록을 조회한다
        val notices = repository.getNotices().getOrThrow()

        // Then 최신 순 5건이고 첫 항목만 NEW 다
        assertEquals(5, notices.size)
        assertEquals("이용약관 및 개인정보처리방침 개정 안내", notices.first().title)
        assertEquals("2026.11.17", notices.first().date)
        assertTrue(notices.first().isNew)
        assertTrue(notices.drop(1).none { it.isNew })
    }

    @Test
    fun getNotice_existingId_returnsThatNotice() = runTest {
        // Given 목록의 첫 항목
        val first = repository.getNotices().getOrThrow().first()

        // When 그 id 로 조회한다
        val notice = repository.getNotice(first.id).getOrThrow()

        // Then 같은 항목이다
        assertEquals(first, notice)
    }

    @Test
    fun getNotice_unknownId_returnsFailure() = runTest {
        // When 없는 id 로 조회한다
        val result = repository.getNotice(-1L)

        // Then 실패다
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }
}
