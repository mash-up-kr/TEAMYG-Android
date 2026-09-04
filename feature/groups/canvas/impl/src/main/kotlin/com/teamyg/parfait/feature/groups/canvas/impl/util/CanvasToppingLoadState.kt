package com.teamyg.parfait.feature.groups.canvas.impl.util

internal enum class ToppingImageState { Loading, Loaded, Failed }

internal enum class CanvasToppingLoadState { Loading, Loaded, Failed }

/**
 * **한 장만 실패해도 전체가 실패다.** 토핑을 하나라도 못 보면 캔버스를 쓸 이유가 없다는
 * 기획 판단이라, 일부 실패를 깨진 그림으로 열어 주던 이전 규칙을 뒤집었다.
 *
 * 실패가 있으면 나머지를 기다리지 않는다 — 다 기다린 뒤에 말하면 그만큼 늦는다.
 * 빈 목록은 실패가 아니다 — 토핑이 없는 캔버스도 온전한 캔버스다.
 */
internal fun canvasToppingLoadState(states: List<ToppingImageState>): CanvasToppingLoadState = when {
    states.any { it == ToppingImageState.Failed } -> CanvasToppingLoadState.Failed
    states.all { it == ToppingImageState.Loaded } -> CanvasToppingLoadState.Loaded
    else -> CanvasToppingLoadState.Loading
}
