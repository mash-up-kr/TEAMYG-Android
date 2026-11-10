package com.teamyg.parfait.data.source.notice.mapper

import com.teamyg.parfait.data.service.model.response.notice.NoticeResponse
import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.datetime.LocalDate

internal fun NoticeResponse.toNoticeVO(): NoticeVO = NoticeVO(
    id = id,
    title = title,
    publishedDate = LocalDate.parse(publishedDate),
    content = content,
)
