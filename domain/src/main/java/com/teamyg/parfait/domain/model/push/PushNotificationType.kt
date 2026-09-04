package com.teamyg.parfait.domain.model.push

/**
 * `type` 값. 라우팅에는 안 쓴다 — 목적지는 `route`가 이미
 * 정해 보낸다. 탭 분석처럼 라우팅 밖의 용도로 [PushDeepLink] 에 실어 둔다.
 */
enum class PushNotificationType {
    TOPPING,
    REMIND_AM,
    REMIND_PM,
}
