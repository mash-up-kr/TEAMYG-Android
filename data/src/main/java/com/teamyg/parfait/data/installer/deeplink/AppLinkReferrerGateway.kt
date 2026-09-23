package com.teamyg.parfait.data.installer.deeplink

/**
 * Install Referrer 의 Play 서비스 쪽 표면. [com.teamyg.parfait.data.installer.image.ModuleInstallGateway]
 * 와 같은 이유로 둔다 — 이 뒤로 GMS 타입이 새지 않아야 JVM 테스트가 닿는다.
 */
interface AppLinkReferrerGateway {
    /**
     * 스토어가 설치 직전 실어 보낸 raw referrer 문자열(예: `inviteCode=ABC123`). 조회할 수
     * 없으면(Play 스토어를 거치지 않은 설치, 연결 실패 등) `null`.
     */
    suspend fun getReferrer(): String?
}
