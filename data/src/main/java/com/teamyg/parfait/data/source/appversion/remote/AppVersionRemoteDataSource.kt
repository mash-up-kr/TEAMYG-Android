package com.teamyg.parfait.data.source.appversion.remote

import com.teamyg.parfait.domain.model.appversion.AppVersionInfo

interface AppVersionRemoteDataSource {
    suspend fun checkVersion(version: String): Result<AppVersionInfo>
}
