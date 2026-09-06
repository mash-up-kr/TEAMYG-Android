package com.teamyg.parfait.domain.model.push

/**
 * `type` 값. 라우팅에는 안 쓴다 — 목적지는 `route`가 이미
 * 정해 보낸다. 탭 분석처럼 라우팅 밖의 용도로 [PushDeepLink] 에 실어 둔다.
 *
 * @property key FCM `data.type` 에 실려 오는 서버 쪽 값.
 */
enum class PushNotificationType(val key: String) {
    TOPPING("TOPPING"),
    REMIND_AM("REMIND_AM"),
    REMIND_PM("REMIND_PM"),
    ;

    companion object {
        fun fromKeyOrNull(key: String?): PushNotificationType? = entries.firstOrNull { it.key == key }
    }
}
