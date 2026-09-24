package com.teamyg.parfait.data.event

import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.push.PushDeepLink
import com.teamyg.parfait.domain.event.PushDeepLinkEventBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 루트가 수집을 시작하기 전에 알림을 탭해도 버퍼에 남았다가 전달되고, 연달아 탭하면 마지막
 * 것 하나로 접힌다. 채널은 프로세스와 함께 사라져 "세션 종료 시 폐기"(정책 4.2 절)가 그대로 성립한다.
 */
@Singleton
class PushDeepLinkEventBusImpl @Inject constructor() : PushDeepLinkEventBus {
    private val channel = Channel<PushDeepLink>(Channel.CONFLATED)

    override val deepLinks: Flow<PushDeepLink> = channel.receiveAsFlow()

    override fun post(deepLink: PushDeepLink) {
        if (channel.trySend(deepLink).isFailure) {
            sourceLogger.e { "PushDeepLink 를 전달하지 못했다" }
        }
    }
}
