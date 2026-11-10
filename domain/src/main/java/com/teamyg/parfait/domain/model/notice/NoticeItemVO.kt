package com.teamyg.parfait.domain.model.notice

/**
 * 화면이 그리는 단위.
 *
 * @property isNew 게시 14일 미만이면서 이 앱에서 상세를 연 적 없는 공지
 */
data class NoticeItemVO(
    val notice: NoticeVO,
    val isNew: Boolean,
)
