package com.teamyg.parfait.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsLogger {
    override fun setCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun setUserProperty(
        name: String,
        value: String,
    ) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    // 파라미터 이름을 문자열로 쓰지 않는다. firebase_ 는 GA4 예약 접두사라 그 이름으로 실어
    // 보낸 값은 버려질 수 있다.
    override fun logScreenView(screen: AnalyticsScreen) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenId)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen.screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }
}
