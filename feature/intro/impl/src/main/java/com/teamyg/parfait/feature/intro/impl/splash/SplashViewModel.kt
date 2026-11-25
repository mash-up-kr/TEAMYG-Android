package com.teamyg.parfait.feature.intro.impl.splash

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.APP_VERSION_NAME
import com.teamyg.parfait.domain.model.appversion.AppUpdateCheck
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.usecase.appversion.CheckAppUpdateUseCase
import com.teamyg.parfait.domain.usecase.session.BootstrapSessionUseCase
import com.teamyg.parfait.feature.intro.impl.model.VERSION_CHECK_TIMEOUT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 다음 화면으로 가려면 세 가지가 모두 끝나야 한다 — 부트스트랩 응답, 버전 판정, 로띠 재생이다.
 * 셋은 서로를 기다리지 않고 각자 끝나므로 어느 쪽이 먼저일지 정해져 있지 않다. 그래서 각각을
 * 상태로 남겨 두고, 마지막에 끝난 쪽이 이동을 일으킨다.
 *
 * @property destination 부트스트랩이 정한 목적지. `null` 은 아직 응답을 못 받은 것이다
 * @property isAnimationFinished 로띠가 끝났는지. 재생을 마쳤거나 로띠를 불러오지 못한 경우다
 * @property updateCheck 버전 판정. `null` 은 아직 모르는 것이다. 선택 업데이트는 사용자가 미루거나 스토어로
 *   가면 [AppUpdateCheck.NotRequired] 로 바꿔 진행 조건을 채운다
 */
data class SplashState(
    val destination: SessionBootstrap? = null,
    val isAnimationFinished: Boolean = false,
    val updateCheck: AppUpdateCheck? = null,
) : UiState {
    /**
     * 이펙트가 아니라 상태로 둔다 — 스토어에서 돌아와도 모달이 남아야 한다.
     * 부트스트랩은 상한이 없어서 기다리지 않는다.
     */
    val isForceUpdateVisible: Boolean
        get() = isAnimationFinished && updateCheck is AppUpdateCheck.Required

    val isOptionalUpdateVisible: Boolean
        get() = isAnimationFinished && updateCheck is AppUpdateCheck.Optional
}

sealed interface SplashIntent : UiIntent {
    data object Init : SplashIntent

    data object AnimationFinished : SplashIntent

    data object UpdateClick : SplashIntent

    data object QuitClick : SplashIntent

    data object LaterClick : SplashIntent
}

sealed interface SplashSideEffect : UiSideEffect {
    data object NavigateToLogin : SplashSideEffect

    data object NavigateToGroupList : SplashSideEffect

    data class OpenStore(val storeUrl: String?) : SplashSideEffect

    data object QuitApp : SplashSideEffect
}

@HiltViewModel
class SplashViewModel
@Inject
constructor(
    private val bootstrapSession: BootstrapSessionUseCase,
    private val checkAppUpdate: CheckAppUpdateUseCase,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(initialState = SplashState()) {
    init {
        viewModelLogger.i { "SplashViewModel::init" }
        processIntent(SplashIntent.Init)
    }

    override fun processIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Init -> handleInit()
            SplashIntent.AnimationFinished -> handleAnimationFinished()
            SplashIntent.UpdateClick -> handleUpdateClick()
            SplashIntent.QuitClick -> postSideEffect(SplashSideEffect.QuitApp)
            SplashIntent.LaterClick -> handleLaterClick()
        }
    }

    /**
     * 목적지 판단은 전부 [BootstrapSessionUseCase] 가 끝낸다 — 여기서는 토큰 유무·조회
     * 성공 여부를 다시 따지지 않고 [SessionBootstrap] 을 받아 둔다.
     *
     * [KEY_BOOTSTRAP] 로 감싸는 이유 — 지금은 진입점이 [SplashIntent.Init] 하나라 두 번째
     * 호출이 실제로 발생하진 않지만, 같은 key 로 guard 를 걸어 두면 이후 다른 진입점(예:
     * 포그라운드 복귀)이 생겨도 조회가 겹치지 않는다.
     *
     * 부트스트랩의 부수효과(푸시 토큰 등록, 인증 거절 시 로그아웃)는 강제 업데이트 중에도 막지 않는다.
     *
     * 버전 체크는 어떻게 실패해도 통과다 — 스플래시에 갇히는 쪽이 더 나쁘다. 조회 실패는 유스케이스,
     * 대기 상한은 [VERSION_CHECK_TIMEOUT], 예상 못 한 예외는 `onError` 가 각각 통과로 돌린다.
     */
    private fun handleInit() {
        launch(key = KEY_BOOTSTRAP) {
            val destination = bootstrapSession()

            updateState { copy(destination = destination) }
            navigateIfReady()
        }
        launch(
            key = KEY_VERSION_CHECK,
            onError = { applyUpdateCheck(AppUpdateCheck.NotRequired) },
        ) {
            val updateCheck = withTimeoutOrNull(VERSION_CHECK_TIMEOUT) {
                checkAppUpdate(APP_VERSION_NAME)
            } ?: AppUpdateCheck.NotRequired

            applyUpdateCheck(updateCheck)
        }
    }

    private fun applyUpdateCheck(updateCheck: AppUpdateCheck) {
        updateState { copy(updateCheck = updateCheck) }
        navigateIfReady()
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

    private fun handleUpdateClick() {
        when (val updateCheck = state.value.updateCheck) {
            is AppUpdateCheck.Required -> postSideEffect(SplashSideEffect.OpenStore(updateCheck.storeUrl))

            is AppUpdateCheck.Optional -> {
                postSideEffect(SplashSideEffect.OpenStore(updateCheck.storeUrl))
                applyUpdateCheck(AppUpdateCheck.NotRequired)
            }

            AppUpdateCheck.NotRequired, null -> Unit
        }
    }

    private fun handleLaterClick() {
        if (state.value.updateCheck !is AppUpdateCheck.Optional) return

        applyUpdateCheck(AppUpdateCheck.NotRequired)
    }

    private fun navigateIfReady() {
        val current = state.value
        val destination = current.destination ?: return

        if (!current.isAnimationFinished || current.updateCheck != AppUpdateCheck.NotRequired) return

        postSideEffect(
            when (destination) {
                SessionBootstrap.ToLogin -> SplashSideEffect.NavigateToLogin
                SessionBootstrap.ToGroupList -> SplashSideEffect.NavigateToGroupList
            },
        )
    }

    private companion object {
        const val KEY_BOOTSTRAP = "bootstrap"
        const val KEY_VERSION_CHECK = "version_check"
    }
}
