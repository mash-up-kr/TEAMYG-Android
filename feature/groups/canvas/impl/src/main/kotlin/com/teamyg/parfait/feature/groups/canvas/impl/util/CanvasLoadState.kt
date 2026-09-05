package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 한 장의 결말이면서 여러 장을 접은 결과이기도 하다 — 접기가 몇 겹이든 같은 규칙이다 */
internal enum class CanvasLoadState { Loading, Loaded, Failed }

/**
 * **한 장만 실패해도 전체가 실패다.** 토핑을 하나라도 못 보면 캔버스를 쓸 이유가 없다는
 * 기획 판단이라, 일부 실패를 깨진 그림으로 열어 주던 이전 규칙을 뒤집었다.
 *
 * 실패가 있으면 나머지를 기다리지 않는다 — 다 기다린 뒤에 말하면 그만큼 늦는다.
 * 빈 목록은 실패가 아니다 — 기다릴 것이 없는 캔버스도 온전한 캔버스다.
 */
internal fun canvasLoadState(states: List<CanvasLoadState>): CanvasLoadState = when {
    states.any { it == CanvasLoadState.Failed } -> CanvasLoadState.Failed
    states.all { it == CanvasLoadState.Loaded } -> CanvasLoadState.Loaded
    else -> CanvasLoadState.Loading
}
