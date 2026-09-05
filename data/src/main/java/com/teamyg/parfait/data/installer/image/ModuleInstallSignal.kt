package com.teamyg.parfait.data.installer.image

/**
 * [ModuleInstallGateway] 가 흘리는 설치 신호. GMS 상태 코드를 그대로 나르지 않고 종료만 추린다.
 */
sealed interface ModuleInstallSignal {
    data object AlreadyInstalled : ModuleInstallSignal

    data object Completed : ModuleInstallSignal

    /** 취소는 `errorCode` 가 0이라 코드만으로는 실패와 안 갈린다 — 상태를 함께 싣는다 */
    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallSignal
}
