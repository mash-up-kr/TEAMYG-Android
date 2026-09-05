package com.teamyg.parfait.push

import com.teamyg.parfait.domain.model.push.PushDeepLink
import com.teamyg.parfait.domain.model.push.PushNotificationType

/**
 * FCM `data` payload 의 필드(FCM 페이로드 스펙 v1 §3)를 [PushDeepLink] 로 바꾼다. 파싱
 * 로직을 `Intent` 추출([toPushDeepLinkOrNull])에서 떼어 둔 이유: 여기가 앞으로 필드가
 * 늘 확장 지점이라, `Intent`(mockk 없이는 순수 JVM에서 못 만든다) 없이 원시 문자열만으로
 * 테스트할 수 있어야 한다.
 */
object PushDeepLinkParser {
    /** `route`/`groupId`/`type` 이 알려진 푸시 딥링크 모양이 아니면(일반 실행 등) `null`. */
    fun parse(
        route: String?,
        groupId: String?,
        type: String?,
    ): PushDeepLink? = when (route) {
        "canvas" -> {
            val id = groupId?.toLongOrNull()
            if (id == null || id <= 0) null else PushDeepLink.AddTopping(id)
        }

        "group" -> PushDeepLink.GroupList(type = PushNotificationType.fromKeyOrNull(type))

        else -> null
    }
}
