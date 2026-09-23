package com.teamyg.parfait.domain.event

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import kotlinx.coroutines.flow.Flow

/**
 * App Links/Universal Links 딥링크 발행·구독구. [PushDeepLinkEventBus] 와 같은 이유로
 * 별도 버스를 둔다 — 발행 경로(Intent data URI, Install Referrer)가 푸시와 다르다.
 *
 * **구독은 앱 루트 한 곳에서만 한다** — 화면마다 구독하면 한 번의 클릭으로 여러 번 이동한다.
 */
interface AppLinkDeepLinkEventBus {
    val deepLinks: Flow<AppLinkDeepLink>

    fun post(deepLink: AppLinkDeepLink)
}
