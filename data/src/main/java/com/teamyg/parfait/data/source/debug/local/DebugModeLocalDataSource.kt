package com.teamyg.parfait.data.source.debug.local

import kotlinx.coroutines.flow.Flow

interface DebugModeLocalDataSource {
    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}
