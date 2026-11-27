package com.teamyg.parfait.data.source.appversion.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.AppVersionService
import com.teamyg.parfait.data.source.appversion.mapper.toAppVersionInfo
import com.teamyg.parfait.domain.model.appversion.AppVersionInfo
import javax.inject.Inject

class AppVersionRemoteDataSourceImpl @Inject constructor(
    private val appVersionService: AppVersionService,
    private val apiCaller: ApiCaller,
) : AppVersionRemoteDataSource {
    override suspend fun checkVersion(version: String): Result<AppVersionInfo> = apiCaller
        .safeApiCall(
            block = { appVersionService.checkVersion(platform = PLATFORM, version = version) },
            transform = { it.toAppVersionInfo() },
        )

    private companion object {
        const val PLATFORM = "ANDROID"
    }
}
