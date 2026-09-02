package com.teamyg.parfait.data.installer.image

/**
 * 모듈 설치의 GMS 쪽 표면. 이 뒤로 Play 서비스 타입이 하나도 새지 않아야 JVM 테스트가 닿는다.
 */
interface ModuleInstallGateway {
    suspend fun isAvailable(): Boolean

    /**
     * ⚠️ [onSignal] 은 GMS 리스너 스레드에서 불린다 — **정지 함수가 아니다.**
     * 받는 쪽은 락을 잡거나 정지 함수를 부를 수 없다.
     */
    fun install(onSignal: (ModuleInstallSignal) -> Unit)
}

sealed interface ModuleInstallSignal {
    data object AlreadyInstalled : ModuleInstallSignal

    data object Completed : ModuleInstallSignal

    /** 취소는 `errorCode` 가 0이라 코드만으로는 실패와 안 갈린다 — 상태를 함께 싣는다 */
    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallSignal
}

sealed interface ModuleInstallOutcome {
    data object Ready : ModuleInstallOutcome

    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallOutcome

    data object TimedOut : ModuleInstallOutcome
}
