package com.teamyg.parfait.core.analytics

/** 분석 도구로 나가는 유일한 창구 — 모듈 배치 근거는 `parfait/adr/0031-analytics-central-screen-mapping.md`. */
interface AnalyticsLogger {
    fun setCollectionEnabled(enabled: Boolean)

    fun setUserProperty(
        name: String,
        value: String,
    )

    /** `null` 이면 지운다. */
    fun setUserId(id: String?)

    fun logScreenView(screen: AnalyticsScreen)

    fun logEvent(event: AnalyticsEvent)
}

data class AnalyticsScreen(
    val type: AnalyticsScreenType,
)

/** 화면 진입이 아니라 화면 안에서의 액션 — `docs/superpowers/specs/2026-10-06-user-events-design.md` */
data class AnalyticsEvent(
    val type: AnalyticsEventType,
    val params: Map<String, String> = emptyMap(),
)
