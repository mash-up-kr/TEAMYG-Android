package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.model.entity.RecentImageEntity
import kotlinx.coroutines.flow.Flow

interface RecentImageLocalDataSource {
    val values: Flow<List<RecentImageEntity>>

    /** 한 `edit` 트랜잭션 안에서 현재 목록을 읽고 바꾼다. */
    suspend fun update(transform: (List<RecentImageEntity>) -> List<RecentImageEntity>)

    suspend fun remove(uris: List<String>)
}
