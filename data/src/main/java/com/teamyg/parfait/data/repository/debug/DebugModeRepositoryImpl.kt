package com.teamyg.parfait.data.repository.debug

import com.teamyg.parfait.data.source.debug.local.DebugModeLocalDataSource
import com.teamyg.parfait.domain.repository.debug.DebugModeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DebugModeRepositoryImpl @Inject constructor(
    private val debugModeLocalDataSource: DebugModeLocalDataSource,
) : DebugModeRepository {
    override val isEnabled: Flow<Boolean> = debugModeLocalDataSource.isEnabled

    override suspend fun setEnabled(enabled: Boolean) = debugModeLocalDataSource.setEnabled(enabled)
}
