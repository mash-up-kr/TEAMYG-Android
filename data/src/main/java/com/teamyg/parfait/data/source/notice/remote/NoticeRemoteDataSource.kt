package com.teamyg.parfait.data.source.notice.remote

import com.teamyg.parfait.domain.model.notice.NoticeVO

interface NoticeRemoteDataSource {
    suspend fun getNotices(): Result<List<NoticeVO>>
}
