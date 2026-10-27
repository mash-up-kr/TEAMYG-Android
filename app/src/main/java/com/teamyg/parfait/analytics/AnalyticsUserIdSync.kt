package com.teamyg.parfait.analytics

import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers
import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val logger = Loggers.create("AnalyticsUserIdSync")

/**
 * GA4 사용자 ID 를 계정 정보 SSoT 의 memberId 에 맞춘다(iOS 와 같은 값).
 * 세션 진입점마다 부르지 않고 SSoT 하나만 구독하는 근거는 `parfait/adr/0031-analytics-central-screen-mapping.md`.
 */
@Singleton
class AnalyticsUserIdSync @Inject constructor(
    private val getMyAccountFlow: GetMyAccountFlowUseCase,
    private val analyticsLogger: AnalyticsLogger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            getMyAccountFlow()
                .map { account -> account?.memberId?.value?.toString() }
                .distinctUntilChanged()
                // 앱 스코프에 핸들러가 없어, 저장소 읽기 실패가 여기서 새면 분석 부가 기능이 앱을 죽인다
                .catch { logger.e(it) { "계정 정보 구독 실패 — 사용자 ID 동기화를 멈춘다" } }
                .collect(analyticsLogger::setUserId)
        }
    }
}
