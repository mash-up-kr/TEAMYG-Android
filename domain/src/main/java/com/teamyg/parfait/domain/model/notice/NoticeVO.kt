package com.teamyg.parfait.domain.model.notice

/**
 * @property date 표시 형식 그대로(`2026.11.17`)
 */
data class NoticeVO(
    val id: Long,
    val title: String,
    val date: String,
    val content: String,
    val isNew: Boolean,
)
