package com.teamyg.parfait.data.installer.deeplink

/**
 * Install Referrer 의 Play 서비스 쪽 표면. [com.teamyg.parfait.data.installer.image.ModuleInstallGateway]
 * 와 같은 이유로 둔다 — 이 뒤로 GMS 타입이 새지 않아야 JVM 테스트가 닿는다.
 */
interface AppLinkReferrerGateway {
    suspend fun getReferrer(): ReferrerLookup
}
