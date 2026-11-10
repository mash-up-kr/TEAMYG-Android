package com.teamyg.parfait.feature.app.notice.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 공지 한 건을 통째로 받는다. 상세는 서버를 부르지 않고, 백스택이 복원돼도 내용이 그대로 남는다.
 *
 * @property date 표시 형식(`2026.11.17`)
 */
@Serializable
data class NavKeyNoticeDetail(
    val noticeId: Long,
    val title: String,
    val date: String,
    val content: String,
) : NavKey
