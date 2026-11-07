package com.teamyg.parfait.domain.repository.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO

interface NoticeRepository {
    suspend fun getNotices(): Result<List<NoticeVO>>

    suspend fun getNotice(id: Long): Result<NoticeVO>
}
