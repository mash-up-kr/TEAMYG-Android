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
