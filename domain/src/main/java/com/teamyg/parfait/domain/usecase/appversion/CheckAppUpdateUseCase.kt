package com.teamyg.parfait.domain.usecase.appversion

import com.teamyg.parfait.domain.model.appversion.AppUpdateCheck
import com.teamyg.parfait.domain.model.appversion.AppUpdateType
import com.teamyg.parfait.domain.repository.appversion.AppVersionRepository
import javax.inject.Inject

/** 조회 실패도 [AppUpdateCheck.NotRequired] 다. 판정을 못 했다고 앱 진입까지 막지 않는다 */
class CheckAppUpdateUseCase @Inject constructor(
    private val repository: AppVersionRepository,
) {
    suspend operator fun invoke(version: String): AppUpdateCheck {
        val info = repository.checkVersion(version).getOrNull() ?: return AppUpdateCheck.NotRequired
        return when (info.updateType) {
            AppUpdateType.FORCE -> AppUpdateCheck.Required(info.storeUrl)
            AppUpdateType.OPTIONAL -> AppUpdateCheck.Optional(info.storeUrl)
            AppUpdateType.NONE, AppUpdateType.UNKNOWN -> AppUpdateCheck.NotRequired
        }
    }
}
