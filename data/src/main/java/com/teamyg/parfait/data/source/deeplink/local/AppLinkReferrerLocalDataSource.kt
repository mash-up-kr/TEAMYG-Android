package com.teamyg.parfait.data.source.deeplink.local

interface AppLinkReferrerLocalDataSource {
    suspend fun hasChecked(): Boolean

    suspend fun markChecked()
}
