package com.teamyg.parfait.domain.repository.pushdeeplink

import com.teamyg.parfait.domain.model.pushdeeplink.PushDeepLink

/**
 * 푸시 딥링크 발행구. [PushDeepLinkSource] 와 나눠 둔 이유: 발행자(Android `Intent` 를 처음
 * 받는 지점, 지금은 `MainActivity`)는 발행만 할 수 있어야 하고, 구독자(`MainRoute`)는
 * 구독만 할 수 있어야 한다. 하나로 합치면 구독자도 발행할 수 있게 돼 "한 곳에서만 발행한다"는
 * 불변식을 타입으로 지킬 수 없다.
 */
interface PushDeepLinkPublisher {
    fun post(deepLink: PushDeepLink)
}
