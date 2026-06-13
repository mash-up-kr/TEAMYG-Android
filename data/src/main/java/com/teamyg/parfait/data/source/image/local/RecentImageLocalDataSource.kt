package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.datastore.RecentImageEditor
import kotlinx.coroutines.flow.Flow

interface RecentImageLocalDataSource {
    val values: Flow<List<String>>

    fun encodeValue(value: List<String>): String

    fun decodeValue(raw: String?): List<String>

    suspend fun edit(transform: suspend (RecentImageEditor) -> Unit)

    suspend fun remove(values: List<String>)
}
