package com.teamyg.parfait.domain.event

import com.teamyg.parfait.domain.model.push.PushDeepLink
import kotlinx.coroutines.flow.Flow

/**
 * 푸시 딥링크 발행·구독구. 발행자(Android `Intent` 를 처음 받는 지점, 지금은
 * `MainActivity`)가 [post] 를, 구독자(`MainRoute`)가 [deepLinks] 를 쓴다.
 *
 * **구독은 앱 루트 한 곳에서만 한다** — 화면마다 구독하면 한 번의 탭으로 여러 번 이동한다.
 * 구현이 `Channel` 기반이라 실제로도 단일 소비자다.
 */
interface PushDeepLinkEventBus {
    val deepLinks: Flow<PushDeepLink>

    fun post(deepLink: PushDeepLink)
}
