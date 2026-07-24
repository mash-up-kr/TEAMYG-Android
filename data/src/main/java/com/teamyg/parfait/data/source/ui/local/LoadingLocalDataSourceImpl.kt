package com.teamyg.parfait.data.source.ui.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadingLocalDataSourceImpl
@Inject
constructor() : LoadingLocalDataSource {
    private val _loadingMap = MutableStateFlow<Map<String, Int>>(emptyMap())

    override val loadingMapFlow: Flow<Map<String, Int>>
        get() = _loadingMap.asStateFlow()

    override val loadingMap: Map<String, Int>
        get() = _loadingMap.value

    override fun setLoadingMap(loadingMap: Map<String, Int>) {
        _loadingMap.value = loadingMap
    }

    override fun updateLoadingMap(loadingMap: Map<String, Int>) {
        _loadingMap.update { loadingMap }
    }

    override suspend fun emitLoadingMap(loadingMap: Map<String, Int>) {
        _loadingMap.emit(loadingMap)
    }
}
