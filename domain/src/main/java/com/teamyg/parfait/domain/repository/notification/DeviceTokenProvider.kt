package com.teamyg.parfait.domain.repository.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken

interface DeviceTokenProvider {
    /** SDK 가 아직 토큰을 발급하지 못했으면 `null`. */
    suspend fun currentToken(): DeviceToken?
}
