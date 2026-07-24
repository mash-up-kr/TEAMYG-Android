package com.teamyg.parfait.data.repository.ui

import com.teamyg.parfait.data.source.ui.local.LoadingLocalDataSource
import com.teamyg.parfait.domain.repository.ui.LoadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadingRepositoryImpl
@Inject
constructor(
    private val loadingLocalDataSource: LoadingLocalDataSource,
) : LoadingRepository {
    override val loadingFlow: Flow<Boolean>
        get() = loadingLocalDataSource.loadingMapFlow
            .map { it.isNotEmpty() }

    override fun getMutableLoadingMap(): MutableMap<String, Int> {
        return loadingLocalDataSource.loadingMap.toMutableMap()
    }

    override fun setLoadingMap(newValue: Map<String, Int>) {
        loadingLocalDataSource.setLoadingMap(newValue)
    }

    override fun containTag(tag: String): Boolean {
        return loadingLocalDataSource.loadingMap.contains(tag)
    }
}
