package com.teamyg.parfait.push

import android.content.Intent
import com.teamyg.parfait.domain.model.push.PushDeepLink

/** 키 이름을 FCM `data` payload 그대로 쓴다. */
private const val EXTRA_TYPE = "type"
private const val EXTRA_ROUTE = "route"
private const val EXTRA_GROUP_ID = "groupId"

/** extras 가 알려진 푸시 딥링크 모양이 아니면(일반 실행 등) `null`. 파싱 자체는 [PushDeepLinkParser] 가 한다. */
fun Intent.toPushDeepLinkOrNull(): PushDeepLink? = PushDeepLinkParser.parse(
    route = getStringExtra(EXTRA_ROUTE),
    groupId = getStringExtra(EXTRA_GROUP_ID),
    type = getStringExtra(EXTRA_TYPE),
)
