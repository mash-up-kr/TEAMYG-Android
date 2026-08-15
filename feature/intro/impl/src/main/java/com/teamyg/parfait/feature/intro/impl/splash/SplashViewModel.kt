package com.teamyg.parfait.feature.intro.impl.splash

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.usecase.session.BootstrapSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data object SplashState : UiState

sealed interface SplashIntent : UiIntent

sealed interface SplashSideEffect : UiSideEffect {
    data object NavigateToLogin : SplashSideEffect

    data object NavigateToGroupList : SplashSideEffect
}

@HiltViewModel
class SplashViewModel
@Inject
constructor(
    private val bootstrapSession: BootstrapSessionUseCase,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(initialState = SplashState) {
    init {
        viewModelLogger.i { "SplashViewModel::init" }
        bootstrap()
    }

    override fun processIntent(intent: SplashIntent) = Unit

    /**
     * 목적지 판단은 전부 [BootstrapSessionUseCase] 가 끝낸다 — 여기서는 토큰 유무·조회
     * 성공 여부를 다시 따지지 않고 [SessionBootstrap] 을 이펙트로만 옮긴다.
     *
     * [KEY_BOOTSTRAP] 로 감싸는 이유는 화면의 다른 ViewModel들과 같은 관용구를 맞추기
     * 위해서다 — 스플래시는 재시도 진입점이 없어 실제로 두 번째 호출이 발생하진 않지만
     * (재조회가 필요하면 [BootstrapSessionUseCase] 가 내부에서 이미 실패를 흡수해
     * 항상 [SessionBootstrap.ToLogin]/[SessionBootstrap.ToGroupList] 중 하나로 끝난다),
     * 같은 key 로 guard 를 걸어 두면 이후 이 함수가 다른 진입점(예: 포그라운드 복귀)에서도
     * 다시 불려도 안전하다.
     */
    private fun bootstrap() {
        launch(key = KEY_BOOTSTRAP) {
            val destination = bootstrapSession()
            postSideEffect(
                when (destination) {
                    SessionBootstrap.ToLogin -> SplashSideEffect.NavigateToLogin
                    SessionBootstrap.ToGroupList -> SplashSideEffect.NavigateToGroupList
                },
            )
        }
    }

    private companion object {
        const val KEY_BOOTSTRAP = "bootstrap"
    }
}
