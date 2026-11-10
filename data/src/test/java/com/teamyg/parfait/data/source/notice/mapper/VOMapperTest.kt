package com.teamyg.parfait.data.source.notice.mapper

import com.teamyg.parfait.data.service.model.response.notice.NoticeResponse
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class VOMapperTest {
    @Test
    fun toNoticeVO_parsesIsoDateToLocalDateAndKeepsOtherFields() {
        // Given 서버 응답
        val response = NoticeResponse(
            id = 7L,
            type = "UPDATE",
            title = "파르페 v1.5.0 업데이트 안내",
            content = "본문",
            publishedDate = "2026-11-12",
        )

        // When VO 로 바꾼다
        val vo = response.toNoticeVO()

        // Then 게시일은 LocalDate 가 되고 나머지는 그대로다
        assertEquals(7L, vo.id)
        assertEquals("파르페 v1.5.0 업데이트 안내", vo.title)
        assertEquals("본문", vo.content)
        assertEquals(LocalDate(2026, 11, 12), vo.publishedDate)
    }
}
