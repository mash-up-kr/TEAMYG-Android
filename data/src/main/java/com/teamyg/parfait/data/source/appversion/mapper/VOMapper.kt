package com.teamyg.parfait.data.source.appversion.mapper

import com.teamyg.parfait.data.service.model.response.appversion.AppVersionCheckResponse
import com.teamyg.parfait.domain.model.appversion.AppUpdateType
import com.teamyg.parfait.domain.model.appversion.AppVersionInfo

internal fun AppVersionCheckResponse.toAppVersionInfo(): AppVersionInfo = AppVersionInfo(
    updateType = AppUpdateType.entries.firstOrNull { it.name == updateType } ?: AppUpdateType.UNKNOWN,
    storeUrl = storeUrl,
)
