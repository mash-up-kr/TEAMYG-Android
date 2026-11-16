package com.teamyg.parfait.domain.model.notice

import kotlinx.datetime.LocalDate

/**
 * 서버가 준 사실만 담는다. NEW 여부는 읽음 상태와 오늘 날짜에 따라 바뀌는 파생 값이라
 * 여기에 두지 않는다([NoticeItemVO]).
 *
 * @property publishedDate 서버가 KST 기준으로 정한 게시일
 */
data class NoticeVO(
    val id: Long,
    val title: String,
    val publishedDate: LocalDate,
    val content: String,
)
