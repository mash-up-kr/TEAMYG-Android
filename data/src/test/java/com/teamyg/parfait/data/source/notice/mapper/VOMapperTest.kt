package com.teamyg.parfait.data.source.notice.mapper

import com.teamyg.parfait.data.service.model.response.notice.NoticeResponse
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VOMapperTest {
    private val today = LocalDate.of(2026, 11, 17)

    private fun response(publishedDate: String) = NoticeResponse(
        id = 1L,
        type = "UPDATE",
        title = "파르페 v1.5.0 업데이트 안내",
        content = "본문",
        publishedDate = publishedDate,
    )

    @Test
    fun toNoticeVO_formatsDateWithDots() {
        // When 서버 날짜를 매핑한다
        val vo = response("2026-11-12").toNoticeVO(today)

        // Then 표시 형식으로 바뀌고 나머지 필드는 그대로다
        assertEquals("2026.11.12", vo.date)
        assertEquals(1L, vo.id)
        assertEquals("파르페 v1.5.0 업데이트 안내", vo.title)
        assertEquals("본문", vo.content)
    }

    @Test
    fun toNoticeVO_publishedToday_isNew() {
        assertTrue(response("2026-11-17").toNoticeVO(today).isNew)
    }

    @Test
    fun toNoticeVO_publishedSixDaysAgo_isNew() {
        assertTrue(response("2026-11-11").toNoticeVO(today).isNew)
    }

    @Test
    fun toNoticeVO_publishedSevenDaysAgo_isNotNew() {
        assertFalse(response("2026-11-10").toNoticeVO(today).isNew)
    }
}
