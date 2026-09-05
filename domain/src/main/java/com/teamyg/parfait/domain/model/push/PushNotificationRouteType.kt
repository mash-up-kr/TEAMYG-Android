package com.teamyg.parfait.domain.model.push

/**
 * `route` 값. 푸시 알림이 열 목적지를 정하는 유일한 근거다([PushNotificationType] 은
 * 라우팅에 쓰지 않는다).
 *
 * @property key FCM `data.route` 에 실려 오는 서버 쪽 값.
 */
enum class PushNotificationRouteType(val key: String) {
    CANVAS("canvas"),
    GROUP("group"),
    ;

    companion object {
        /** 서버가 보낸 `route` 가 아는 값이 아니면(딥링크가 아닌 평범한 실행 등) `null`. */
        fun fromKeyOrNull(key: String?): PushNotificationRouteType? = entries.firstOrNull { it.key == key }
    }
}
