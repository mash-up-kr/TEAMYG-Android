package com.teamyg.parfait.data.pushdeeplink

import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.pushdeeplink.PushDeepLink
import com.teamyg.parfait.domain.repository.pushdeeplink.PushDeepLinkSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱이 로그인 전이거나 백그라운드에서 막 켜진 순간 알림을 탭해도, 앱 루트가 수집을 시작할
 * 때까지 버퍼에 남아 있다가 그때 전달된다. 반대로 프로세스가 죽으면 채널도 함께 사라지므로
 * "세션 종료 시 폐기"(정책 4.2 절)가 별도 코드 없이 그대로 성립한다. 여러 알림을 연달아
 * 탭해도 마지막 것 하나로 접히는 것도 의도한 동작이다 — 화면은 결국 한 곳에만 도착한다.
 */
@Singleton
class PushDeepLinkBus @Inject constructor() : PushDeepLinkSource {
    private val channel = Channel<PushDeepLink>(Channel.CONFLATED)

    override val deepLinks: Flow<PushDeepLink> = channel.receiveAsFlow()

    fun post(deepLink: PushDeepLink) {
        if (channel.trySend(deepLink).isFailure) {
            sourceLogger.e { "PushDeepLink 를 전달하지 못했다" }
        }
    }
}
