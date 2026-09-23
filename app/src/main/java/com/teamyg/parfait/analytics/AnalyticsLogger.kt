package com.teamyg.parfait.analytics

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink

/** 분석 도구로 나가는 유일한 창구 — `parfait/adr/0031-analytics-central-screen-mapping.md` */
interface AnalyticsLogger {
    fun setCollectionEnabled(enabled: Boolean)

    fun setUserProperty(
        name: String,
        value: String,
    )

    fun logScreenView(screen: AnalyticsScreen)

    /**
     * App Links 로 앱이 열렸을 때(직접 클릭 또는 Install Referrer 복원) 남긴다. 웹 폴백
     * 페이지(미설치 → 스토어 이동)의 이벤트와 같은 스키마를 공유해야 서버 쪽 GA4 property 에서
     * 캠페인별 클릭→오픈 전환을 함께 집계할 수 있다.
     */
    fun logAppLinkOpened(deepLink: AppLinkDeepLink)
}

/**
 * @property screenClass `NavKey` 이름. 리플렉션이 아니라 상수다 —
 *   release 는 R8 난독화가 켜져 있어 `simpleName` 이 뭉개진다
 */
data class AnalyticsScreen(
    val screenId: String,
    val screenClass: String,
)
