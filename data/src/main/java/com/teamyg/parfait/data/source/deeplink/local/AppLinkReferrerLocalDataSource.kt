package com.teamyg.parfait.data.source.deeplink.local

/** Install Referrer 를 이 기기에서 이미 확인했는지 여부. */
interface AppLinkReferrerLocalDataSource {
    suspend fun hasChecked(): Boolean

    suspend fun markChecked()
}
