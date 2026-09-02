package com.teamyg.parfait.pushdeeplink

import android.content.Intent
import com.teamyg.parfait.domain.model.pushdeeplink.PushDeepLink

/**
 * 알림의 `PendingIntent` 가 [MainActivity][com.teamyg.parfait.MainActivity] 를 이 extras 로
 * 띄운다. 알림을 실제로 만드는 쪽(FCM 메시징 서비스, 아직 없음)이 여기 키로 extras 를 채워야
 * 알림 탭이 이 파서와 맞는다.
 */
private const val EXTRA_PUSH_TYPE = "push_type"
private const val EXTRA_GROUP_ID = "push_group_id"

/** extras 가 알려진 푸시 딥링크 모양이 아니면(일반 실행 등) `null`. */
fun Intent.toPushDeepLinkOrNull(): PushDeepLink? = when (getStringExtra(EXTRA_PUSH_TYPE)) {
    "add_topping" -> {
        val groupId = getLongExtra(EXTRA_GROUP_ID, -1L)
        if (groupId <= 0) null else PushDeepLink.AddTopping(groupId)
    }

    "reminder" -> PushDeepLink.Reminder

    else -> null
}
