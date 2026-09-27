package com.teamyg.parfait.data.installer.image

/** [SegmentationModuleInstaller.ensureInstalled] 의 결과. */
sealed interface ModuleInstallOutcome {
    data object Ready : ModuleInstallOutcome

    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallOutcome

    data object TimedOut : ModuleInstallOutcome
}
