package com.teamyg.parfait.domain.repository.ui

import kotlinx.coroutines.flow.Flow

interface LoadingRepository {
    val loadingFlow: Flow<Boolean>

    fun getMutableLoadingMap(): MutableMap<String, Int>

    fun setLoadingMap(newValue: Map<String, Int>)

    fun containTag(tag: String): Boolean
}
