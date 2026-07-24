package com.teamyg.parfait.data.source.ui.local

import kotlinx.coroutines.flow.Flow

interface LoadingLocalDataSource {
    val loadingMapFlow: Flow<Map<String, Int>>

    val loadingMap: Map<String, Int>


    fun setLoadingMap(loadingMap: Map<String, Int>)

    fun updateLoadingMap(loadingMap: Map<String, Int>)

    suspend fun emitLoadingMap(loadingMap: Map<String, Int>)
}
