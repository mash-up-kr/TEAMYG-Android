package com.teamyg.parfait.data.repository.appversion

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.appversion.remote.AppVersionRemoteDataSource
import com.teamyg.parfait.domain.model.appversion.AppVersionInfo
import com.teamyg.parfait.domain.repository.appversion.AppVersionRepository
import javax.inject.Inject

class AppVersionRepositoryImpl @Inject constructor(
    private val appVersionRemoteDataSource: AppVersionRemoteDataSource,
) : AppVersionRepository {
    override suspend fun checkVersion(version: String): Result<AppVersionInfo> = appVersionRemoteDataSource
        .checkVersion(version)
        .mapErrorToAppError()
}
