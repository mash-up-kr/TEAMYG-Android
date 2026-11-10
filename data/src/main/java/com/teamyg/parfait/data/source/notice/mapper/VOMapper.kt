package com.teamyg.parfait.data.source.notice.mapper

import com.teamyg.parfait.data.service.model.response.notice.NoticeResponse
import com.teamyg.parfait.domain.model.notice.NoticeVO
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val NEW_DAYS = 7L

// 서버에 읽음 상태가 없어 게시 후 일정 기간만 NEW 로 본다. 기획 확정 전 임시 기준이다
internal fun NoticeResponse.toNoticeVO(today: LocalDate): NoticeVO {
    val published = LocalDate.parse(publishedDate)

    return NoticeVO(
        id = id,
        title = title,
        date = publishedDate.replace('-', '.'),
        content = content,
        isNew = ChronoUnit.DAYS.between(published, today) < NEW_DAYS,
    )
}

internal fun List<NoticeResponse>.toNoticeVOList(today: LocalDate): List<NoticeVO> = map { it.toNoticeVO(today) }
