package com.teamyg.parfait.domain.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken

interface DeviceTokenProvider {
    /** 아직 발급하지 못했으면 값이 아니라 예외로 온다 — 등록은 다음 시점이 메운다. */
    suspend fun currentToken(): DeviceToken
}
