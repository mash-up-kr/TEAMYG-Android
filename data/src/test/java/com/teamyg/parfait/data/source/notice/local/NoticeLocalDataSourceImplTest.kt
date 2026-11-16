package com.teamyg.parfait.data.source.notice.local

import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NoticeLocalDataSourceImplTest {
    private val dataSource = NoticeLocalDataSourceImpl()

    private val notice = NoticeVO(
        id = 1L,
        title = "공지",
        publishedDate = LocalDate(2026, 11, 17),
        content = "본문",
    )

    @Test
    fun notices_initially_isNull() {
        // 아직 한 번도 받지 못한 상태는 빈 목록과 구분되는 null 이다
        assertNull(dataSource.notices.value)
    }

    @Test
    fun saveNotices_emitsSavedList() {
        // When 목록을 저장한다
        dataSource.saveNotices(listOf(notice))

        // Then 구독하는 값이 그 목록이다
        assertEquals(listOf(notice), dataSource.notices.value)
    }

    @Test
    fun saveNotices_emptyList_isEmptyNotNull() {
        // When 공지 0건을 저장한다
        dataSource.saveNotices(emptyList())

        // Then null 이 아니라 빈 목록이다
        assertNotNull(dataSource.notices.value)
        assertEquals(emptyList(), dataSource.notices.value)
    }

    @Test
    fun clear_returnsToNull() {
        // Given 저장된 목록이 있다
        dataSource.saveNotices(listOf(notice))

        // When 비운다
        dataSource.clear()

        // Then 미수신 상태로 돌아간다
        assertNull(dataSource.notices.value)
    }
}
