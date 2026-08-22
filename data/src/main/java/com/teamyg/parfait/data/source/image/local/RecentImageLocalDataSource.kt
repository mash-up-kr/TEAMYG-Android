package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.datastore.RecentImageEditor
import com.teamyg.parfait.data.model.local.RecentImageEntity
import kotlinx.coroutines.flow.Flow

interface RecentImageLocalDataSource {
    val values: Flow<List<RecentImageEntity>>

    fun encodeValue(value: List<RecentImageEntity>): String

    fun decodeValue(raw: String?): List<RecentImageEntity>

    suspend fun edit(transform: suspend (RecentImageEditor) -> Unit)

    suspend fun remove(uris: List<String>)
}
