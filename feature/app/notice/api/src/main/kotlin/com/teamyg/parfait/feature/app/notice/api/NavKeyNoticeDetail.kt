package com.teamyg.parfait.feature.app.notice.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 내용은 싣지 않고 id 만 받는다. 상세가 공지 인메모리 캐시에서 이 id 로 찾으며,
 * 캐시가 비어 있으면(프로세스 복원 직후 등) 스스로 서버에서 다시 받는다.
 */
@Serializable
data class NavKeyNoticeDetail(
    val noticeId: Long,
) : NavKey
