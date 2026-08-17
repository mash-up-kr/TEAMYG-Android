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

/**
 * 다음 화면으로 가려면 두 가지가 모두 끝나야 한다 — 부트스트랩 응답과 로띠 재생이다. 둘은
 * 서로를 기다리지 않고 각자 끝나므로 어느 쪽이 먼저일지 정해져 있지 않다. 그래서 각각을
 * 상태로 남겨 두고, 나중에 끝난 쪽이 이동을 일으킨다.
 *
 * @property destination 부트스트랩이 정한 목적지. `null` 은 아직 응답을 못 받은 것이다
 * @property isAnimationFinished 로띠가 끝났는지. 재생을 마쳤거나 로띠를 불러오지 못한 경우다
 */
data class SplashState(
    val destination: SessionBootstrap? = null,
    val isAnimationFinished: Boolean = false,
) : UiState

sealed interface SplashIntent : UiIntent {
    /** 화면 진입 */
    data object Init : SplashIntent

    /** 로띠 재생이 끝났다 */
    data object AnimationFinished : SplashIntent
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
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(initialState = SplashState()) {
    init {
        viewModelLogger.i { "SplashViewModel::init" }
        processIntent(SplashIntent.Init)
    }

    override fun processIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Init -> handleInit()
            SplashIntent.AnimationFinished -> handleAnimationFinished()
        }
    }

    /**
     * 목적지 판단은 전부 [BootstrapSessionUseCase] 가 끝낸다 — 여기서는 토큰 유무·조회
     * 성공 여부를 다시 따지지 않고 [SessionBootstrap] 을 받아 둔다.
     *
     * [KEY_BOOTSTRAP] 로 감싸는 이유 — 지금은 진입점이 [SplashIntent.Init] 하나라 두 번째
     * 호출이 실제로 발생하진 않지만, 같은 key 로 guard 를 걸어 두면 이후 다른 진입점(예:
     * 포그라운드 복귀)이 생겨도 조회가 겹치지 않는다.
     */
    private fun handleInit() {
        launch(key = KEY_BOOTSTRAP) {
            val destination = bootstrapSession()

            updateState { copy(destination = destination) }
            navigateIfReady()
        }
    }

    /**
     * 첫 신호만 받아들인다 — 컴포지션이 다시 서면 화면이 같은 신호를 두 번 보낼 수 있고,
     * 그때 이동 이펙트가 두 번 나가면 백스택을 두 번 갈아 끼운다.
     */
    private fun handleAnimationFinished() {
        if (state.value.isAnimationFinished) return

        updateState { copy(isAnimationFinished = true) }
        navigateIfReady()
    }

    /** 둘 중 나중에 끝난 쪽이 이동을 일으킨다 — 먼저 끝난 쪽에서는 조건이 아직 안 차 그냥 빠진다 */
    private fun navigateIfReady() {
        val current = state.value
        val destination = current.destination

        if (destination == null || !current.isAnimationFinished) return

        postSideEffect(
            when (destination) {
                SessionBootstrap.ToLogin -> SplashSideEffect.NavigateToLogin
                SessionBootstrap.ToGroupList -> SplashSideEffect.NavigateToGroupList
            },
        )
    }

    private companion object {
        const val KEY_BOOTSTRAP = "bootstrap"
    }
}
