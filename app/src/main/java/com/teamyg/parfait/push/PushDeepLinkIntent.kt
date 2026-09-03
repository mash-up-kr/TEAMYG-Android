package com.teamyg.parfait.push

import android.content.Intent
import com.teamyg.parfait.domain.model.push.PushDeepLink

/**
 * 키 이름을 FCM `data` payload 그대로 쓴다(FCM 페이로드 스펙 v1 §3).
 * `type`(TOPPING/REMIND_AM/REMIND_PM)과 `date`는 여기서 안 쓴다 — 목적지는 서버가 이미
 * `route`로 정해 보내고, P-01은 정책상 알림에 담긴 날짜가 아니라 항상 최신 캔버스로 연다.
 */
private const val EXTRA_ROUTE = "route"
private const val EXTRA_GROUP_ID = "groupId"

/** extras 가 알려진 푸시 딥링크 모양이 아니면(일반 실행 등) `null`. */
fun Intent.toPushDeepLinkOrNull(): PushDeepLink? = when (getStringExtra(EXTRA_ROUTE)) {
    "canvas" -> {
        val groupId = getStringExtra(EXTRA_GROUP_ID)?.toLongOrNull()
        if (groupId == null || groupId <= 0) null else PushDeepLink.AddTopping(groupId)
    }

    "group" -> PushDeepLink.Reminder

    else -> null
}
