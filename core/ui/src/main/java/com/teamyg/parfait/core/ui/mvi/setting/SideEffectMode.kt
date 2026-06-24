package com.teamyg.parfait.core.ui.mvi.setting

/**
 * SideEffect Flow 전략
 */
enum class SideEffectMode {
    /**
     * 이펙트가 정확히 한 구독자에게만 전달 (Channel.receiveAsFlow)
     */
    FAN_OUT,

    /**
     * FAN_OUT 과 같지만 두 번째 구독자가 붙으면 예외 방출 (Channel.consumeAsFlow)
     */
    FAN_OUT_STRICT,

    /**
     * 모든 활성 구독자에게 브로드캐스트 (MutableSharedFlow)
     */
    BROADCAST,
}
