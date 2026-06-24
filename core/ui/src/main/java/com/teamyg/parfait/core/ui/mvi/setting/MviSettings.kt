package com.teamyg.parfait.core.ui.mvi.setting

import kotlinx.coroutines.channels.Channel

/**
 * Mvi 설정값
 *
 * @property sideEffectMode 이펙트 전달 전략
 * @property sideEffectBufferSize 이펙트 버퍼 크기
 * @property replayClearDelayMillis BROADCAST 에서 구독 시 replay 캐시를 비우기 전 대기 시간
 * @property subscriptionStopTimeoutMillis repeatOnSubscription 의 구독 해제 디바운스
 */
data class MviSettings(
    val sideEffectMode: SideEffectMode = SideEffectMode.BROADCAST,
    val sideEffectBufferSize: Int = Channel.BUFFERED,
    val replayClearDelayMillis: Long = 100L,
    val subscriptionStopTimeoutMillis: Long = 5_000L,
)
