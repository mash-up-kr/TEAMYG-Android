package com.teamyg.parfait.core.util.jvm.useranalytics

/** 분석 도구로 나가는 유일한 창구 — 모듈 배치 근거는 `parfait/adr/0031-analytics-central-screen-mapping.md`. */
interface AnalyticsLogger {
    fun setCollectionEnabled(enabled: Boolean)

    fun setUserProperty(
        name: String,
        value: String,
    )

    fun logScreenView(screen: AnalyticsScreen)

    fun logEvent(event: AnalyticsEvent)
}

/**
 * @property screenClass `NavKey` 이름. 리플렉션이 아니라 상수다 —
 *   release 는 R8 난독화가 켜져 있어 `simpleName` 이 뭉개진다
 */
data class AnalyticsScreen(
    val screenId: String,
    val screenClass: String,
)

/** 화면 진입이 아니라 화면 안에서의 액션 — `docs/superpowers/specs/2026-10-06-user-events-design.md` */
data class AnalyticsEvent(
    val type: AnalyticsEventType,
    val params: Map<String, String> = emptyMap(),
)
