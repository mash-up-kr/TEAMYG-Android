package com.teamyg.parfait.domain.repository.notification

interface NotificationRepository {
    /**
     * 이 기기의 현재 토큰 등록을 걸어만 두고 곧장 돌아온다. 등록 결과로 부르는 화면이 달라지지 않는다.
     *
     * 토큰 값을 받지 않고 매번 지금 값을 읽는다. 토큰은 SDK 가 설치 시점에 발급해서, `onNewToken` 만
     * 기다리면 등록이 영영 안 될 수 있다.
     */
    fun registerCurrentDeviceToken()
}
