package com.teamyg.parfait.data.installer.image

import kotlinx.coroutines.Deferred

/**
 * 모듈 설치의 GMS 쪽 표면. 이 뒤로 Play 서비스 타입이 하나도 새지 않아야 JVM 테스트가 닿는다.
 */
interface ModuleInstallGateway {
    suspend fun isAvailable(): Boolean

    /** 설치를 요청하고, 첫 종료 신호로 완료되는 [Deferred] 를 돌려준다. */
    fun install(): Deferred<ModuleInstallSignal>
}
