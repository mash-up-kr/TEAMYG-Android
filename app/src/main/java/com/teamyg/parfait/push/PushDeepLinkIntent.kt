package com.teamyg.parfait.push

import android.content.Intent
import com.teamyg.parfait.domain.model.push.PushDeepLink

/** 키 이름을 FCM `data` payload 그대로 쓴다. */
private const val EXTRA_TYPE = "type"
private const val EXTRA_ROUTE = "route"
private const val EXTRA_GROUP_ID = "groupId"

/**
 * extras 가 알려진 푸시 딥링크 모양이 아니면(일반 실행 등) `null`. 파싱 자체는 [PushDeepLinkParser] 가 한다.
 *
 * 알림이 만든 인텐트는 그것이 띄운 **태스크의 base intent** 로 남는다. 뒤로가기로 액티비티가
 * 끝나도 태스크 기록은 남으므로, 최근 앱·런처로 그 태스크를 되살리면 시스템이 같은 extras 를
 * 다시 실어 `onCreate` 를 부른다 — `MainActivity` 의 `setIntent(Intent())` 는 액티비티 인스턴스의
 * 필드만 비우는 것이라 이것을 막지 못한다. 되살린 경우에만 붙는
 * [Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY] 로 가른다.
 */
fun Intent.toPushDeepLinkOrNull(): PushDeepLink? {
    if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null

    return PushDeepLinkParser.parse(
        route = getStringExtra(EXTRA_ROUTE),
        groupId = getStringExtra(EXTRA_GROUP_ID),
        type = getStringExtra(EXTRA_TYPE),
    )
}
