package com.teamyg.parfait.data.event

import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.event.AppLinkDeepLinkEventBus
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** [PushDeepLinkEventBusImpl] 과 같은 이유로 CONFLATED 채널을 쓴다. */
@Singleton
class AppLinkDeepLinkEventBusImpl @Inject constructor() : AppLinkDeepLinkEventBus {
    private val channel = Channel<AppLinkDeepLink>(Channel.CONFLATED)

    override val deepLinks: Flow<AppLinkDeepLink> = channel.receiveAsFlow()

    override fun post(deepLink: AppLinkDeepLink) {
        if (channel.trySend(deepLink).isFailure) {
            sourceLogger.e { "AppLinkDeepLink 를 전달하지 못했다" }
        }
    }
}
