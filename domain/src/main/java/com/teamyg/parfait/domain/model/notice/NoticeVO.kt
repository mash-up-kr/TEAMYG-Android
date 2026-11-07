package com.teamyg.parfait.domain.model.notice

/**
 * @property date 표시 형식 그대로(`2026.11.17`). 화면이 날짜 연산을 하지 않아 타입으로 바꾸지 않는다.
 */
data class NoticeVO(
    val id: Long,
    val title: String,
    val date: String,
    val content: String,
    val isNew: Boolean,
)
