package com.teamyg.parfait.analytics

/** 분석 도구로 나가는 유일한 창구 — `parfait/adr/0031-analytics-central-screen-mapping.md` */
interface AnalyticsLogger {
    fun setCollectionEnabled(enabled: Boolean)

    fun setUserProperty(
        name: String,
        value: String,
    )

    fun logScreenView(screen: AnalyticsScreen)
}

/**
 * @property screenClass `NavKey` 이름. 리플렉션이 아니라 상수다 —
 *   release 는 R8 난독화가 켜져 있어 `simpleName` 이 뭉개진다
 */
data class AnalyticsScreen(
    val screenId: String,
    val screenClass: String,
)
