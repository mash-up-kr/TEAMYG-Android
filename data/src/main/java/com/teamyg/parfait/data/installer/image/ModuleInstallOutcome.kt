package com.teamyg.parfait.data.installer.image

/**
 * [SegmentationModuleInstaller.ensureInstalled] 의 결과. 신호와 달리 대기 상한을 넘긴 경우가 있다.
 */
sealed interface ModuleInstallOutcome {
    data object Ready : ModuleInstallOutcome

    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallOutcome

    data object TimedOut : ModuleInstallOutcome
}
