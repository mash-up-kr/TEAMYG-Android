package com.teamyg.parfait.analytics

import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GA4 사용자 ID 를 계정 정보 SSoT 의 memberId 에 맞춘다 — iOS 와 같은 값이라 플랫폼 간 같은 회원으로 묶인다.
 *
 * 로그인·가입·자동로그인은 SSoT 를 채우고, 로그아웃·탈퇴·재발급 거절(`TokenAuthenticator`)은 지운다.
 * 진입점마다 부르면 하나를 빠뜨리기 쉬워 SSoT 하나만 구독한다.
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
                .collect(analyticsLogger::setUserId)
        }
    }
}
