package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 일괄 등장 빗장에 넘길 결말 목록. 이미지는 성공이든 실패든 결말나야 하고, 텍스트는 받아 올 것이
 * 없어 처음부터 결말난 셈이다 — 텍스트만 있는 캔버스가 빗장에 막혀 안 보이면 안 된다.
 */
internal fun canvasRevealSettled(
    imageStates: List<CanvasLoadState>,
    textCount: Int,
): List<Boolean> = imageStates.map { it != CanvasLoadState.Loading } + List(textCount) { true }
