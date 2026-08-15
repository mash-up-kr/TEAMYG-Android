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

sealed interface SplashIntent : UiIntent {
    /** 화면 진입. 스플래시는 사용자 조작이 없어 이것이 유일한 의도다 */
    data object Init : SplashIntent
}

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
        processIntent(SplashIntent.Init)
    }

    override fun processIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Init -> handleInit()
        }
    }

    /**
     * 목적지 판단은 전부 [BootstrapSessionUseCase] 가 끝낸다 — 여기서는 토큰 유무·조회
     * 성공 여부를 다시 따지지 않고 [SessionBootstrap] 을 이펙트로만 옮긴다.
     *
     * [KEY_BOOTSTRAP] 로 감싸는 이유 — 지금은 진입점이 [SplashIntent.Init] 하나라 두 번째
     * 호출이 실제로 발생하진 않지만, 같은 key 로 guard 를 걸어 두면 이후 다른 진입점(예:
     * 포그라운드 복귀)이 생겨도 조회가 겹치지 않는다.
     */
    private fun handleInit() {
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
