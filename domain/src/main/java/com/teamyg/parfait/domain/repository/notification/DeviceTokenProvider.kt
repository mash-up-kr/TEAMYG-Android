package com.teamyg.parfait.domain.repository.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken

/**
 * 지금 이 기기의 FCM 등록 토큰을 읽는다.
 */
interface DeviceTokenProvider {
    /** SDK 가 아직 토큰을 발급하지 못했으면(드묾) `null`. */
    suspend fun currentToken(): DeviceToken?
}
