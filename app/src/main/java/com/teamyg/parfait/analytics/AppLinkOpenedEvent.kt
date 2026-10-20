package com.teamyg.parfait.analytics

import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink

fun AppLinkDeepLink.toOpenedEvent(): AnalyticsEvent {
    val destination = when (this) {
        is AppLinkDeepLink.OpenGroupInvite -> "group_invite"
    }
    return AnalyticsEvent(
        type = AnalyticsEventType.APP_LINK_OPENED,
        params = mapOf("destination" to destination),
    )
}
