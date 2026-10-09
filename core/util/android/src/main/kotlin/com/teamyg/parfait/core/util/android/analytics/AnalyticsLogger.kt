package com.teamyg.parfait.core.util.android.analytics

/**
 * 분석 도구로 나가는 유일한 창구 — `parfait/adr/0031-analytics-central-screen-mapping.md`.
 *
 * `app` 모듈이 아니라 `core.util.android`에 있는 이유는, 각 feature `impl` 모듈의
 * `ViewModel`이 액션 이벤트를 찍으려면 이 인터페이스를 직접 참조해야 하는데
 * `app` → `feature:*:impl` 방향 의존만 허용되는 모듈 구조상 역방향 참조가 안 되기 때문이다.
 * 구현체(`FirebaseAnalyticsLogger`)와 Hilt 바인딩은 여전히 `app` 모듈에 있다.
 */
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
    val eventId: String,
    val eventName: String,
    val params: Map<String, String> = emptyMap(),
)
