package com.teamyg.parfait.domain.repository.appversion

import com.teamyg.parfait.domain.model.appversion.AppVersionInfo

interface AppVersionRepository {
    suspend fun checkVersion(version: String): Result<AppVersionInfo>
}
